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
package com.xceptance.xcmailr.config;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.xceptance.xcmailr.repositories.DomainRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

import models.Domain;
import models.User;

/**
 * Initializes the default administrator account and domain whitelist upon application startup
 * if they do not already exist in the database.
 */
@Component
@Order(10)
public class AdminUserInitializer implements ApplicationRunner
{
    private static final Logger LOG = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final UserRepository userRepository;
    private final DomainRepository domainRepository;
    private final XcmailrProperties properties;
    private final PasswordEncoder passwordEncoder;

    public AdminUserInitializer(final UserRepository userRepository,
                                final DomainRepository domainRepository,
                                final XcmailrProperties properties,
                                final PasswordEncoder passwordEncoder)
    {
        this.userRepository = userRepository;
        this.domainRepository = domainRepository;
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(final ApplicationArguments args)
    {
        seedAdminUser();
        seedDefaultDomain();
    }

    private void seedAdminUser()
    {
        final String adminMail = properties.getAdmin().getAddress();
        final String adminPassword = properties.getAdmin().getPassword();

        if (adminMail == null || adminMail.isBlank())
        {
            LOG.warn("No administrator address configured in xcmailr.admin.address. Skipping admin account creation.");
            return;
        }

        final String normalizedMail = adminMail.trim().toLowerCase();
        final Optional<User> existingUserOpt = userRepository.findByMailIgnoreCase(normalizedMail);

        if (existingUserOpt.isEmpty())
        {
            LOG.info("Creating default administrator account: {}", normalizedMail);
            final User admin = new User();
            admin.setForename("Site");
            admin.setSurname("Admin");
            admin.setMail(normalizedMail);
            admin.setPasswd(passwordEncoder.encode(adminPassword));
            admin.setLanguage("en");
            admin.setAdmin(true);
            admin.setActive(true);
            userRepository.save(admin);
            LOG.info("Default administrator account created successfully.");
        }
        else
        {
            final User existing = existingUserOpt.get();
            boolean modified = false;

            if (!existing.isAdmin())
            {
                existing.setAdmin(true);
                modified = true;
            }
            if (!existing.isActive())
            {
                existing.setActive(true);
                modified = true;
            }
            if (existing.getPasswd() == null || existing.getPasswd().isBlank()
                || !passwordEncoder.matches(adminPassword, existing.getPasswd()))
            {
                existing.setPasswd(passwordEncoder.encode(adminPassword));
                modified = true;
                LOG.info("Synchronized administrator account password with configured admin password.");
            }

            if (modified)
            {
                userRepository.save(existing);
                LOG.info("Updated existing administrator account permissions for: {}", normalizedMail);
            }
        }
    }

    private void seedDefaultDomain()
    {
        final String adminMail = properties.getAdmin().getAddress();
        if (adminMail == null || !adminMail.contains("@"))
        {
            return;
        }

        final String domainPart = adminMail.substring(adminMail.indexOf('@') + 1).trim().toLowerCase();
        if (!domainPart.isBlank() && !domainRepository.existsByDomainnameIgnoreCase(domainPart))
        {
            LOG.info("Seeding default domain in whitelist: {}", domainPart);
            final Domain domain = new Domain(domainPart);
            domainRepository.save(domain);
        }
    }
}
