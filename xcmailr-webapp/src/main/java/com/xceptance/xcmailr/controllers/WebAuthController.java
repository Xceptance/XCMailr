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

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.xceptance.xcmailr.config.XcmailrProperties;
import com.xceptance.xcmailr.repositories.UserRepository;
import com.xceptance.xcmailr.services.OutboundMailService;

import models.User;

/**
 * Web controller handling user authentication and self-service lifecycle flows:
 * login, registration, confirmation tokens, and password reset.
 *
 * @author Xceptance Software Technologies GmbH
 */
@Controller
public class WebAuthController
{
    private static final Logger LOG = LoggerFactory.getLogger(WebAuthController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OutboundMailService outboundMailService;
    private final XcmailrProperties properties;

    public WebAuthController(final UserRepository userRepository,
                             final PasswordEncoder passwordEncoder,
                             final OutboundMailService outboundMailService,
                             final XcmailrProperties properties)
    {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.outboundMailService = outboundMailService;
        this.properties = properties;
    }

    /**
     * Renders the login page.
     *
     * @param model template model
     * @return login view name
     */
    @GetMapping("/login")
    public String login(final Model model)
    {
        return "auth/login";
    }

    /**
     * Renders the user registration form.
     *
     * @param model template model
     * @return register view name
     */
    @GetMapping("/register")
    public String registerForm(final Model model)
    {
        return "auth/register";
    }

    /**
     * Processes self-service account registration.
     *
     * @param forename user's first name
     * @param surname user's last name
     * @param mail email address (target forwarding address)
     * @param password user plaintext password
     * @param confirmPassword repeated password confirmation
     * @param language preferred UI language
     * @param model template model
     * @return redirect or view name
     */
    @PostMapping("/register")
    public String registerSubmit(@RequestParam("forename") final String forename,
                                 @RequestParam("surname") final String surname,
                                 @RequestParam("mail") final String mail,
                                 @RequestParam("password") final String password,
                                 @RequestParam("confirmPassword") final String confirmPassword,
                                 @RequestParam(value = "language", defaultValue = "en") final String language,
                                 final Model model)
    {
        if (password == null || password.length() < 6)
        {
            model.addAttribute("errorMessage", "Password must be at least 6 characters long.");
            populateForm(model, forename, surname, mail);
            return "auth/register";
        }

        if (!password.equals(confirmPassword))
        {
            model.addAttribute("errorMessage", "Passwords do not match.");
            populateForm(model, forename, surname, mail);
            return "auth/register";
        }

        final String cleanMail = mail.trim().toLowerCase();
        if (userRepository.existsByMailIgnoreCase(cleanMail))
        {
            model.addAttribute("errorMessage", "An account with this email address already exists.");
            populateForm(model, forename, surname, cleanMail);
            return "auth/register";
        }

        final User user = new User();
        user.setForename(forename.trim());
        user.setSurname(surname.trim());
        user.setMail(cleanMail);
        user.setPasswd(passwordEncoder.encode(password));
        user.setLanguage(language);

        final String token = UUID.randomUUID().toString();
        final long validityHours = properties.getApp().getConfirmationPeriodHours();
        final long expiration = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(validityHours);

        user.setActive(false);
        user.setConfirmation(token);
        user.setTs_confirm(expiration);
        userRepository.save(user);

        final String confirmUrl = properties.getApp().getUrl() + "/confirm/" + token;
        final String subject = "Activate your " + properties.getApp().getName() + " account";
        final String body = "Hello " + user.getForename() + ",\n\n"
                          + "Please confirm your account by visiting the following link:\n"
                          + confirmUrl + "\n\n"
                          + "This link expires in " + validityHours + " hours.\n";

        outboundMailService.sendAsync(cleanMail, subject, body);
        LOG.info("Sent registration confirmation email to {}", cleanMail);
        return "redirect:/login?registered";
    }

    /**
     * Confirms and activates a user account via confirmation token.
     *
     * @param token confirmation token string
     * @return redirect to login page with status
     */
    @GetMapping("/confirm/{token}")
    public String confirmAccount(@PathVariable("token") final String token)
    {
        final Optional<User> userOpt = userRepository.findByConfirmation(token);
        if (userOpt.isEmpty())
        {
            return "redirect:/login?invalidToken";
        }

        final User user = userOpt.get();
        if (user.getTs_confirm() != null && user.getTs_confirm() < System.currentTimeMillis())
        {
            LOG.warn("Confirmation token expired for user {}", user.getMail());
            return "redirect:/login?invalidToken";
        }

        user.setActive(true);
        user.setConfirmation(null);
        user.setTs_confirm(null);
        userRepository.save(user);
        LOG.info("Account {} successfully confirmed and activated", user.getMail());

        return "redirect:/login?confirmed";
    }

    /**
     * Renders forgot-password request form.
     *
     * @param model template model
     * @return forgot-password view name
     */
    @GetMapping("/forgot-password")
    public String forgotPasswordForm(final Model model)
    {
        return "auth/forgot-password";
    }

    /**
     * Initiates password recovery process.
     *
     * @param mail email of the account to reset
     * @return redirect to login with info message
     */
    @PostMapping("/forgot-password")
    public String forgotPasswordSubmit(@RequestParam("mail") final String mail)
    {
        final String cleanMail = mail.trim().toLowerCase();
        final Optional<User> userOpt = userRepository.findByMailIgnoreCase(cleanMail);

        if (userOpt.isPresent())
        {
            final User user = userOpt.get();
            final String token = UUID.randomUUID().toString();
            final long expiration = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2);

            user.setConfirmation(token);
            user.setTs_confirm(expiration);
            userRepository.save(user);

            final String resetUrl = properties.getApp().getUrl() + "/pwreset/" + token;
            final String subject = properties.getApp().getName() + " Password Reset Request";
            final String body = "Hello " + user.getForename() + ",\n\n"
                              + "You requested a password reset. You can set a new password here:\n"
                              + resetUrl + "\n\n"
                              + "This link expires in 2 hours.\n";

            outboundMailService.sendAsync(cleanMail, subject, body);
            LOG.info("Sent password reset link to {}", cleanMail);
        }

        return "redirect:/login?resetRequested";
    }

    /**
     * Renders password reset form for token.
     *
     * @param token reset token
     * @param model template model
     * @return reset-password view name
     */
    @GetMapping("/pwreset/{token}")
    public String resetPasswordForm(@PathVariable("token") final String token, final Model model)
    {
        final Optional<User> userOpt = userRepository.findByConfirmation(token);
        if (userOpt.isEmpty())
        {
            return "redirect:/login?invalidToken";
        }

        final User user = userOpt.get();
        if (user.getTs_confirm() != null && user.getTs_confirm() < System.currentTimeMillis())
        {
            return "redirect:/login?invalidToken";
        }

        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    /**
     * Submits new password for token.
     *
     * @param token reset token
     * @param password new password
     * @param confirmPassword repeated new password
     * @param model template model
     * @return redirect or view name
     */
    @PostMapping("/pwreset/{token}")
    public String resetPasswordSubmit(@PathVariable("token") final String token,
                                      @RequestParam("password") final String password,
                                      @RequestParam("confirmPassword") final String confirmPassword,
                                      final Model model)
    {
        final Optional<User> userOpt = userRepository.findByConfirmation(token);
        if (userOpt.isEmpty())
        {
            return "redirect:/login?invalidToken";
        }

        final User user = userOpt.get();
        if (password == null || password.length() < 6)
        {
            model.addAttribute("errorMessage", "Password must be at least 6 characters long.");
            model.addAttribute("token", token);
            return "auth/reset-password";
        }

        if (!password.equals(confirmPassword))
        {
            model.addAttribute("errorMessage", "Passwords do not match.");
            model.addAttribute("token", token);
            return "auth/reset-password";
        }

        user.setPasswd(passwordEncoder.encode(password));
        user.setConfirmation(null);
        user.setTs_confirm(null);
        userRepository.save(user);
        LOG.info("Password successfully reset for user {}", user.getMail());

        return "redirect:/login?passwordReset";
    }

    private void populateForm(final Model model, final String forename, final String surname, final String mail)
    {
        model.addAttribute("forename", forename);
        model.addAttribute("surname", surname);
        model.addAttribute("mail", mail);
    }
}
