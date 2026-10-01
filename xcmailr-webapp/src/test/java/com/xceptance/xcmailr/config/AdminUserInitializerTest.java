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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.xceptance.xcmailr.repositories.DomainRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

import models.Domain;
import models.User;

/**
 * Component and database persistence tests for {@link AdminUserInitializer}.
 * Verifies non-destructive initialization, preserving existing accounts without modification,
 * seeding default domains on an active database, and disaster recovery.
 */
@SpringBootTest
@ActiveProfiles("test")
public class AdminUserInitializerTest
{
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private MailboxRepository mailboxRepository;

    @Autowired
    private XcmailrProperties properties;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AdminUserInitializer initializer;

    @BeforeEach
    void setUp()
    {
        mailboxRepository.deleteAll();
        domainRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * Verifies that on an empty database with configured administrator credentials,
     * the initial administrator account and default domain whitelist are seeded.
     */
    @Test
    void testSeedsAdminUserAndDomainOnEmptyDatabase()
    {
        initializer.run(new DefaultApplicationArguments());

        final String adminMail = properties.getAdmin().getAddress().toLowerCase();
        final Optional<User> adminOpt = userRepository.findByMailIgnoreCase(adminMail);

        assertTrue(adminOpt.isPresent(), "Admin user should have been created");
        final User admin = adminOpt.get();
        assertEquals("Site", admin.getForename());
        assertEquals("Admin", admin.getSurname());
        assertTrue(admin.isAdmin(), "User should have admin role");
        assertTrue(admin.isActive(), "User should be active");
        assertTrue(passwordEncoder.matches("1234", admin.getPasswd()), "Password should match BCrypt hash");

        final String domainName = adminMail.substring(adminMail.indexOf('@') + 1);
        final Optional<Domain> domainOpt = domainRepository.findByDomainnameIgnoreCase(domainName);
        assertTrue(domainOpt.isPresent(), "Default domain should have been seeded");
    }

    /**
     * Verifies that when an administrator account already exists in the database,
     * the initializer performs no updates and strictly preserves the existing password and details.
     */
    @Test
    void testPreservesExistingAdminPasswordWithoutMutation()
    {
        final String adminMail = properties.getAdmin().getAddress().toLowerCase();
        final String customPassword = "myCustomSecretPassword99!";

        final User existing = new User();
        existing.setForename("Custom");
        existing.setSurname("Person");
        existing.setMail(adminMail);
        existing.setPasswd(passwordEncoder.encode(customPassword));
        existing.setAdmin(true);
        existing.setActive(true);
        existing.setLanguage("de");
        userRepository.save(existing);

        // Run the initializer again (simulating subsequent application restart)
        initializer.run(new DefaultApplicationArguments());

        final User admin = userRepository.findByMailIgnoreCase(adminMail).orElseThrow();
        assertEquals("Custom", admin.getForename(), "Forename must not be altered");
        assertEquals("Person", admin.getSurname(), "Surname must not be altered");
        assertEquals("de", admin.getLanguage(), "Language preference must not be altered");
        assertTrue(passwordEncoder.matches(customPassword, admin.getPasswd()), "Custom user password must be preserved");
        assertFalse(passwordEncoder.matches("1234", admin.getPasswd()), "Default password must NOT have overwritten custom password");
    }

    /**
     * Verifies disaster recovery: if an administrator account was previously deleted,
     * restarting the application with configured credentials safely recreates the admin account.
     */
    @Test
    void testDisasterRecoveryRecreatesDeletedAdmin()
    {
        // 1. Initial bootstrap
        initializer.run(new DefaultApplicationArguments());
        final String adminMail = properties.getAdmin().getAddress().toLowerCase();
        assertTrue(userRepository.findByMailIgnoreCase(adminMail).isPresent(), "Admin should exist after first run");

        // 2. Accidental deletion
        userRepository.delete(userRepository.findByMailIgnoreCase(adminMail).orElseThrow());
        assertTrue(userRepository.findByMailIgnoreCase(adminMail).isEmpty(), "Admin should be deleted");

        // 3. Application restart (disaster recovery)
        initializer.run(new DefaultApplicationArguments());
        final Optional<User> recoveredOpt = userRepository.findByMailIgnoreCase(adminMail);
        assertTrue(recoveredOpt.isPresent(), "Admin account should be recovered on restart");
        assertTrue(recoveredOpt.get().isAdmin(), "Recovered user should have admin privileges");
    }
}
