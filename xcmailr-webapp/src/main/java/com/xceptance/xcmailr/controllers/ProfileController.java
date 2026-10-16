/*
 * Copyright (c) 2013-2026 Xceptance Software Technologies GmbH
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
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.xceptance.xcmailr.repositories.UserRepository;

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
    private final PasswordEncoder passwordEncoder;

    public ProfileController(final UserRepository userRepository,
                             final PasswordEncoder passwordEncoder)
    {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
        model.addAttribute("user", user);

        if (user.getApiTokenCreationTimestamp() > 0)
        {
            final String formatted = DATE_FORMATTER.format(Instant.ofEpochMilli(user.getApiTokenCreationTimestamp()));
            model.addAttribute("tokenCreationDate", formatted);
        }

        return "user/profile";
    }

    /**
     * Updates user's personal details.
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

        LOG.info("Updated profile for user {}", user.getMail());
        return "redirect:/profile?updated";
    }

    /**
     * Changes user account password after validating old password.
     *
     * @param currentPassword current plaintext password
     * @param newPassword new password
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
            model.addAttribute("user", user);
            model.addAttribute("errorMessage", "Current password does not match.");
            return "user/profile";
        }

        if (newPassword == null || newPassword.length() < 6)
        {
            model.addAttribute("user", user);
            model.addAttribute("errorMessage", "New password must be at least 6 characters long.");
            return "user/profile";
        }

        if (!newPassword.equals(confirmNewPassword))
        {
            model.addAttribute("user", user);
            model.addAttribute("errorMessage", "New passwords do not match.");
            return "user/profile";
        }

        user.setPasswd(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        LOG.info("Password successfully changed for user {}", user.getMail());
        return "redirect:/profile?passwordChanged";
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

    private User getUser(final Principal principal)
    {
        return userRepository.findByMailIgnoreCase(principal.getName())
                             .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + principal.getName()));
    }
}
