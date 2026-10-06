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
import org.springframework.util.StringUtils;

import com.xceptance.xcmailr.repositories.DomainRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

import models.Domain;
import models.User;

/**
 * Initializes the primary administrator account and default domain whitelist entry upon
 * application startup if they do not already exist in the database.
 * <p>
 * This initializer operates in a strictly non-destructive manner across all environments:
 * <ul>
 *   <li>If administrator credentials are omitted or blank, bootstrapping is bypassed with zero database queries.</li>
 *   <li>If an account with the configured administrator address already exists, it is left completely untouched
 *       to prevent overwriting user-updated passwords or permissions.</li>
 *   <li>If absent (such as on a fresh database or during disaster recovery after accidental deletion), the account
 *       is created and the default domain whitelist entry is seeded.</li>
 * </ul>
 */
@Component
@Order(10)
public class AdminUserInitializer implements ApplicationRunner
{
    private static final Logger LOG = LoggerFactory.getLogger(AdminUserInitializer.class);

    /**
     * Default placeholder password used for local development warnings.
     */
    private static final String DEFAULT_DEV_PASSWORD = "1234";

    private final UserRepository userRepository;
    private final DomainRepository domainRepository;
    private final XcmailrProperties properties;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructs the initializer with required repository and security dependencies.
     *
     * @param userRepository repository for managing user persistence
     * @param domainRepository repository for managing domain whitelist persistence
     * @param properties application configuration properties
     * @param passwordEncoder encoder used to hash passwords securely
     */
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
        final String adminMail = properties.getAdmin().getAddress();
        final String adminPassword = properties.getAdmin().getPassword();

        // 1. Guard against blank or unconfigured credentials: skip lookups completely if either is missing or blank.
        if (!StringUtils.hasText(adminMail) || !StringUtils.hasText(adminPassword))
        {
            LOG.info("Administrator address or password is blank or not configured. Skipping admin account bootstrap.");
            return;
        }

        final String normalizedMail = adminMail.trim().toLowerCase();

        final String adminApiToken = properties.getAdmin().getApiToken();

        // 2. Perform idempotent absence check: only seed if the account does not already exist.
        final Optional<User> existingUserOpt = userRepository.findByMailIgnoreCase(normalizedMail);
        if (existingUserOpt.isPresent())
        {
            final User existing = existingUserOpt.get();
            if (!StringUtils.hasText(existing.getApiToken()) && StringUtils.hasText(adminApiToken))
            {
                existing.setApiToken(adminApiToken.trim());
                existing.setApiTokenCreationTimestamp(System.currentTimeMillis());
                userRepository.save(existing);
                LOG.info("Configured initial API token for existing administrator account '{}'", normalizedMail);
            }
            LOG.debug("Administrator account '{}' already exists. Leaving credentials and permissions untouched.", normalizedMail);
            return;
        }

        // 3. Create missing administrator account (fresh installation or disaster recovery).
        LOG.info("Administrator account '{}' not found. Bootstrapping initial administrative user.", normalizedMail);
        if (DEFAULT_DEV_PASSWORD.equals(adminPassword))
        {
            LOG.warn("SECURITY WARNING: Bootstrapping administrator account '{}' with default placeholder password. "
                     + "Change this password immediately in production!", normalizedMail);
        }

        final User admin = new User();
        admin.setForename("Site");
        admin.setSurname("Admin");
        admin.setMail(normalizedMail);
        admin.setPasswd(passwordEncoder.encode(adminPassword));
        admin.setLanguage("en");
        admin.setAdmin(true);
        admin.setActive(true);
        if (StringUtils.hasText(adminApiToken))
        {
            admin.setApiToken(adminApiToken.trim());
            admin.setApiTokenCreationTimestamp(System.currentTimeMillis());
        }
        userRepository.save(admin);
        LOG.info("Administrative account '{}' created successfully.", normalizedMail);

        // 4. Seed default domain whitelist entry if absent.
        seedDefaultDomain(normalizedMail);
    }

    /**
     * Extracts the domain from the administrator's email address and ensures it exists in the domain whitelist.
     *
     * @param normalizedAdminMail the normalized administrator email address
     */
    private void seedDefaultDomain(final String normalizedAdminMail)
    {
        if (!normalizedAdminMail.contains("@"))
        {
            return;
        }

        final String domainPart = normalizedAdminMail.substring(normalizedAdminMail.indexOf('@') + 1).trim().toLowerCase();
        if (StringUtils.hasText(domainPart) && !domainRepository.existsByDomainnameIgnoreCase(domainPart))
        {
            LOG.info("Seeding default domain in whitelist: {}", domainPart);
            final Domain domain = new Domain(domainPart);
            domainRepository.save(domain);
        }
    }
}
