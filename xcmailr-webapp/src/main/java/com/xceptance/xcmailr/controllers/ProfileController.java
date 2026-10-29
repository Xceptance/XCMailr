/*
 * Copyright (c) Xceptance Software Technologies GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xceptance.xcmailr.controllers;

import java.security.Principal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;
import com.xceptance.xcmailr.security.PasswordContext;
import com.xceptance.xcmailr.security.PasswordValidationResult;
import com.xceptance.xcmailr.security.PasswordValidator;
import com.xceptance.xcmailr.security.UserPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import models.MBox;
import models.User;

/**
 * Controller for self-service user profile management and REST API token administration.
 *
 * @author Xceptance Software Technologies GmbH
 */
@Controller
@RequestMapping("/profile")
public class ProfileController
{
    private static final Logger LOG = LoggerFactory.getLogger(ProfileController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                                                             .withZone(ZoneId.systemDefault());

    private final UserRepository userRepository;
    private final MailboxRepository mailboxRepository;
    private final MailRepository mailRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;

    /**
     * Constructs a new {@link ProfileController} instance with all required repositories and security services.
     *
     * @param userRepository repository for accessing and managing user accounts
     * @param mailboxRepository repository for managing mailboxes
     * @param mailRepository repository for managing emails
     * @param passwordEncoder encoder used to hash and verify user passwords
     * @param passwordValidator validator for enforcing password quality standards
     */
    public ProfileController(final UserRepository userRepository,
                             final MailboxRepository mailboxRepository,
                             final MailRepository mailRepository,
                             final PasswordEncoder passwordEncoder,
                             final PasswordValidator passwordValidator)
    {
        this.userRepository = userRepository;
        this.mailboxRepository = mailboxRepository;
        this.mailRepository = mailRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidator = passwordValidator;
    }


    /**
     * Renders user profile and API token management page.
     *
     * @param principal authenticated user principal
     * @param model template model
     * @return profile view name
     */
    @GetMapping
    public String showProfile(final Principal principal, final Model model)
    {
        final User user = getUser(principal);
        populateProfileModel(model, user);
        return "user/profile";
    }

    /**
     * Updates user's personal details and synchronizes in-memory session state.
     *
     * @param forename first name
     * @param surname last name
     * @param language preferred language
     * @param principal authenticated user principal
     * @return redirect to profile
     */
    @PostMapping("/update")
    public String updateProfile(@RequestParam("forename") final String forename,
                                @RequestParam("surname") final String surname,
                                @RequestParam(value = "language", defaultValue = "en") final String language,
                                final Principal principal)
    {
        final User user = getUser(principal);
        user.setForename(forename.trim());
        user.setSurname(surname.trim());
        user.setLanguage(language);
        userRepository.save(user);

        // Defense-in-depth Layer 2: Synchronize in-memory UserPrincipal to maintain session consistency
        // without altering security-sensitive attributes (roles, admin status, credentials)
        final Authentication auth = principal instanceof final Authentication a
            ? a
            : SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof final UserPrincipal userPrincipal)
        {
            final User sessionUser = userPrincipal.getUser();
            if (sessionUser != null)
            {
                sessionUser.setForename(user.getForename());
                sessionUser.setSurname(user.getSurname());
                sessionUser.setLanguage(user.getLanguage());
            }
        }

        LOG.info("Updated profile for user {}", user.getMail());
        return "redirect:/profile?updated";
    }

    /**
     * Changes user account password after validating old password against stored hash and new password against
     * modern OWASP ASVS v4.0 and NIST SP 800-63B guidelines.
     *
     * @param currentPassword current plaintext password
     * @param newPassword new password candidate
     * @param confirmNewPassword new password confirmation
     * @param principal authenticated user principal
     * @param model template model
     * @return redirect or view name on error
     */
    @PostMapping("/change-password")
    public String changePassword(@RequestParam("currentPassword") final String currentPassword,
                                 @RequestParam("newPassword") final String newPassword,
                                 @RequestParam("confirmNewPassword") final String confirmNewPassword,
                                 final Principal principal,
                                 final Model model)
    {
        final User user = getUser(principal);

        if (!passwordEncoder.matches(currentPassword, user.getPasswd()))
        {
            populateProfileModel(model, user);
            model.addAttribute("errorMessage", "Current password does not match.");
            return "user/profile";
        }

        if (!Objects.equals(newPassword, confirmNewPassword))
        {
            populateProfileModel(model, user);
            model.addAttribute("errorMessage", "New passwords do not match.");
            return "user/profile";
        }

        final PasswordContext passwordContext = PasswordContext.fromUser(user);
        final PasswordValidationResult validationResult = passwordValidator.validate(newPassword, passwordContext);
        if (!validationResult.isValid())
        {
            populateProfileModel(model, user);
            model.addAttribute("errorMessage", validationResult.errorMessage());
            return "user/profile";
        }

        try
        {
            user.setPasswd(passwordEncoder.encode(newPassword));
        }
        catch (final IllegalArgumentException e)
        {
            LOG.warn("Failed to encode password during password change for {}: {}", user.getMail(), e.getMessage());
            populateProfileModel(model, user);
            model.addAttribute("errorMessage", "Password cannot be processed (exceeds allowable length or byte limits).");
            return "user/profile";
        }

        userRepository.save(user);

        LOG.info("Password successfully changed for user {}", user.getMail());
        return "redirect:/profile?passwordChanged";
    }

    private void populateProfileModel(final Model model, final User user)
    {
        model.addAttribute("user", user);
        if (user.getApiTokenCreationTimestamp() > 0)
        {
            final String formatted = DATE_FORMATTER.format(Instant.ofEpochMilli(user.getApiTokenCreationTimestamp()));
            model.addAttribute("tokenCreationDate", formatted);
        }
    }

    /**
     * Generates a new API token for the user.
     *
     * @param principal authenticated user principal
     * @return redirect to profile
     */
    @PostMapping("/api-token")
    public String generateApiToken(final Principal principal)
    {
        final User user = getUser(principal);
        final String newToken = UUID.randomUUID().toString();

        user.setApiToken(newToken);
        user.setApiTokenCreationTimestamp(System.currentTimeMillis());
        userRepository.save(user);

        LOG.info("Generated new API token for user {}", user.getMail());
        return "redirect:/profile?tokenCreated";
    }

    /**
     * Revokes the user's active API token.
     *
     * @param principal authenticated user principal
     * @return redirect to profile
     */
    @PostMapping("/api-token/revoke")
    public String revokeApiToken(final Principal principal)
    {
        final User user = getUser(principal);
        user.setApiToken(null);
        user.setApiTokenCreationTimestamp(0L);
        userRepository.save(user);

        LOG.info("Revoked API token for user {}", user.getMail());
        return "redirect:/profile?tokenRevoked";
    }

    /**
     * Handles self-service user account deletion.
     * <p>
     * Re-authenticates the user using their current plaintext password, verifies that the user is not the sole
     * remaining active administrator, transactionally removes all emails and mailboxes associated with the account,
     * deletes the user record, clears the active security context, invalidates the HTTP session, and redirects to
     * {@code /login?accountDeleted}.
     * </p>
     *
     * @param currentPassword the user's current password for confirmation
     * @param principal authenticated user principal
     * @param request current HTTP request used to invalidate the session
     * @param model template model for presenting validation errors
     * @return redirect to login page upon success, or the profile view if validation fails
     */
    @PostMapping("/delete")
    @Transactional
    public String deleteAccount(@RequestParam(value = "currentPassword", defaultValue = "") final String currentPassword,
                                final Principal principal,
                                final HttpServletRequest request,
                                final Model model)
    {
        final User user = getUser(principal);

        // Validate that password was provided
        if (currentPassword == null || currentPassword.isBlank())
        {
            populateProfileModel(model, user);
            model.addAttribute("errorMessage", "Please enter your current password to confirm deletion.");
            return "user/profile";
        }

        // Validate password match
        if (!passwordEncoder.matches(currentPassword, user.getPasswd()))
        {
            populateProfileModel(model, user);
            model.addAttribute("errorMessage", "Current password does not match.");
            return "user/profile";
        }

        // Enforce sole administrator protection: at least one active administrator must remain
        if (user.isAdmin() && userRepository.countByAdminTrueAndActiveTrue() <= 1)
        {
            populateProfileModel(model, user);
            model.addAttribute("errorMessage", "Your account cannot be deleted because it is the last administrator account. At least one active administrator account must exist.");
            return "user/profile";
        }

        // Transactional cascade deletion of all mailboxes and emails owned by the user
        final List<MBox> boxes = mailboxRepository.findByUsr(user, Pageable.unpaged()).getContent();
        for (final MBox box : boxes)
        {
            mailRepository.deleteByMailbox(box.getId());
            mailboxRepository.delete(box);
        }

        // Delete user entity
        userRepository.delete(user);
        LOG.info("User {} successfully deleted their account via self-service", user.getMail());

        // Invalidate session and clear security context
        SecurityContextHolder.clearContext();
        final HttpSession session = request.getSession(false);
        if (session != null)
        {
            session.invalidate();
        }

        return "redirect:/login?accountDeleted";
    }

    private User getUser(final Principal principal)
    {
        return userRepository.findByMailIgnoreCase(principal.getName())
                             .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + principal.getName()));
    }
}

