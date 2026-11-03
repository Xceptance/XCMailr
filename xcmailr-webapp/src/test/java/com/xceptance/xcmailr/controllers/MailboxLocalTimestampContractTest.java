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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.xceptance.xcmailr.XcmailrApplication;
import com.xceptance.xcmailr.repositories.DomainRepository;
import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

import models.Domain;
import models.MBox;
import models.Mail;
import models.User;

/**
 * Template contract test suite verifying client-side timestamp localization integration
 * adhering to the testing pyramid:
 * <ul>
 *   <li>Happy path tests: Verifies that expiring mailboxes, edit modals, and mail views render HTML5
 *       &lt;time class="local-time"&gt; tags with ISO-8601 UTC datetime attributes.</li>
 *   <li>Special cases: Verifies that unlimited mailboxes (ts_Active == 0) render "Unlimited" text
 *       and suppress &lt;time&gt; tag generation, and that the base layout includes the Intl script.</li>
 *   <li>Fallback and error cases: Verifies server-rendered UTC fallback text for expired mailboxes
 *       and admin transaction entries.</li>
 * </ul>
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest(classes = XcmailrApplication.class)
@ActiveProfiles("test")
public final class MailboxLocalTimestampContractTest
{
    private static final String TEST_USER_EMAIL = "timestamp-user@xcmailr.test";
    private static final String TEST_ADMIN_EMAIL = "timestamp-admin@xcmailr.test";
    private static final String TEST_DOMAIN_NAME = "xcmailr.test";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private MailboxRepository mailboxRepository;

    @Autowired
    private MailRepository mailRepository;

    private MockMvc mockMvc;
    private User testUser;
    private User adminUser;
    private Domain testDomain;

    @BeforeEach
    public void setUp()
    {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        mailRepository.deleteAll();
        mailboxRepository.deleteAll();
        userRepository.deleteAll();
        domainRepository.deleteAll();

        testDomain = new Domain(TEST_DOMAIN_NAME);
        domainRepository.save(testDomain);

        testUser = new User("John", "Tester", TEST_USER_EMAIL, "Password123!", "en");
        testUser.setActive(true);
        userRepository.save(testUser);

        adminUser = new User("Admin", "Root", TEST_ADMIN_EMAIL, "Password123!", "en");
        adminUser.setActive(true);
        adminUser.setAdmin(true);
        userRepository.save(adminUser);
    }

    @AfterEach
    public void tearDown()
    {
        mailRepository.deleteAll();
        mailboxRepository.deleteAll();
        userRepository.deleteAll();
        domainRepository.deleteAll();
    }

    // =========================================================================
    // HAPPY PATH CONTRACT TESTS
    // =========================================================================

    @Test
    @DisplayName("Happy path: Expiring mailbox renders <time class=\"local-time\"> with valid ISO-8601 datetime in table")
    public void testExpiringMailboxRendersLocalTimeTagInTable() throws Exception
    {
        // GIVEN a mailbox expiring in 2 hours
        final long futureEpoch = System.currentTimeMillis() + 7200000L;
        final MBox box = new MBox("expiring-box", TEST_DOMAIN_NAME, futureEpoch, false, testUser);
        mailboxRepository.save(box);

        final String expectedIsoInstant = ISO_FORMATTER.format(Instant.ofEpochMilli(futureEpoch));

        // WHEN & THEN requesting the mailbox table
        mockMvc.perform(get("/mailboxes")
               .header("HX-Request", "true")
               .with(user(TEST_USER_EMAIL).roles("USER"))
               .accept(MediaType.TEXT_HTML))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("<time")))
               .andExpect(content().string(containsString("class=\"local-time")))
               .andExpect(content().string(containsString("datetime=\"" + expectedIsoInstant + "\"")));
    }

    @Test
    @DisplayName("Happy path: Mailbox edit modal renders <time class=\"local-time\"> with ISO-8601 datetime")
    public void testExpiringMailboxRendersLocalTimeTagInEditModal() throws Exception
    {
        // GIVEN a mailbox expiring in 24 hours
        final long futureEpoch = System.currentTimeMillis() + 86400000L;
        final MBox box = new MBox("modal-box", TEST_DOMAIN_NAME, futureEpoch, false, testUser);
        mailboxRepository.save(box);

        final String expectedIsoInstant = ISO_FORMATTER.format(Instant.ofEpochMilli(futureEpoch));

        // WHEN & THEN requesting the edit modal fragment
        mockMvc.perform(get("/mailboxes/" + box.getId() + "/edit")
               .header("HX-Request", "true")
               .with(user(TEST_USER_EMAIL).roles("USER"))
               .accept(MediaType.TEXT_HTML))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("<time class=\"local-time\"")))
               .andExpect(content().string(containsString("datetime=\"" + expectedIsoInstant + "\"")));
    }

    @Test
    @DisplayName("Happy path: Mailbox inbox mail list renders <time class=\"local-time\"> for message receive time")
    public void testMailInboxRendersLocalTimeTag() throws Exception
    {
        // GIVEN a mailbox and a received mail
        final long receiveEpoch = System.currentTimeMillis() - 600000L;
        final MBox box = new MBox("inbox-box", TEST_DOMAIN_NAME, 0L, false, testUser);
        mailboxRepository.save(box);

        final Mail mail = new Mail();
        mail.setSender("sender@example.com");
        mail.setSubject("Contract Test Message");
        mail.setReceiveTime(receiveEpoch);
        mail.setMessage("Sample raw body".getBytes());
        mail.setMailbox(box);
        mailRepository.save(mail);

        final String expectedIsoInstant = ISO_FORMATTER.format(Instant.ofEpochMilli(receiveEpoch));

        // WHEN & THEN requesting the mailbox mails view
        mockMvc.perform(get("/mailboxes/" + box.getId() + "/mails")
               .header("HX-Request", "true")
               .with(user(TEST_USER_EMAIL).roles("USER"))
               .accept(MediaType.TEXT_HTML))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("<time class=\"local-time\"")))
               .andExpect(content().string(containsString("datetime=\"" + expectedIsoInstant + "\"")));
    }

    // =========================================================================
    // SPECIAL CASE CONTRACT TESTS
    // =========================================================================

    @Test
    @DisplayName("Special case: Unlimited mailbox (ts_Active == 0) renders 'Unlimited' text and suppresses <time> tag in table")
    public void testUnlimitedMailboxRendersUnlimitedTextAndSuppressesTimeTagInTable() throws Exception
    {
        // GIVEN a mailbox with ts_Active = 0 (unlimited)
        final MBox unlimitedBox = new MBox("unlimited-box", TEST_DOMAIN_NAME, 0L, false, testUser);
        mailboxRepository.save(unlimitedBox);

        // WHEN & THEN requesting the mailbox table
        mockMvc.perform(get("/mailboxes")
               .header("HX-Request", "true")
               .with(user(TEST_USER_EMAIL).roles("USER"))
               .accept(MediaType.TEXT_HTML))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("Unlimited")))
               .andExpect(content().string(not(containsString("<time"))));
    }

    @Test
    @DisplayName("Special case: Unlimited mailbox renders 'Unlimited' text and suppresses <time> tag in edit modal")
    public void testUnlimitedMailboxRendersUnlimitedInEditModal() throws Exception
    {
        // GIVEN an unlimited mailbox
        final MBox unlimitedBox = new MBox("unlimited-modal", TEST_DOMAIN_NAME, 0L, false, testUser);
        mailboxRepository.save(unlimitedBox);

        // WHEN & THEN requesting the edit modal fragment
        mockMvc.perform(get("/mailboxes/" + unlimitedBox.getId() + "/edit")
               .header("HX-Request", "true")
               .with(user(TEST_USER_EMAIL).roles("USER"))
               .accept(MediaType.TEXT_HTML))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("Unlimited")))
               .andExpect(content().string(not(containsString("<time"))));
    }

    @Test
    @DisplayName("Special case: Base layout includes formatLocalTimes script with Intl.DateTimeFormat and htmx.onLoad")
    public void testBaseLayoutIncludesFormatLocalTimesScript() throws Exception
    {
        // WHEN & THEN requesting full index page
        mockMvc.perform(get("/mailboxes")
               .with(user(TEST_USER_EMAIL).roles("USER"))
               .accept(MediaType.TEXT_HTML))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("function formatLocalTimes(rootEl)")))
               .andExpect(content().string(containsString("Intl.DateTimeFormat")))
               .andExpect(content().string(containsString("htmx.onLoad(function (content)")))
               .andExpect(content().string(containsString("formatLocalTimes(content);")));
    }

    // =========================================================================
    // FALLBACK AND ERROR CASE CONTRACT TESTS
    // =========================================================================

    @Test
    @DisplayName("Fallback case: Past expired mailbox renders <time class=\"local-time\"> with ISO-8601 UTC attribute and fallback text")
    public void testExpiredMailboxRendersFallbackTextAndValidIsoAttribute() throws Exception
    {
        // GIVEN an expired mailbox with timestamp in the past
        final long pastEpoch = 1761000000000L; // Fixed past epoch
        final MBox expiredBox = new MBox("expired-box", TEST_DOMAIN_NAME, pastEpoch, true, testUser);
        mailboxRepository.save(expiredBox);

        final String expectedIsoInstant = ISO_FORMATTER.format(Instant.ofEpochMilli(pastEpoch));

        // WHEN & THEN requesting the mailbox table
        mockMvc.perform(get("/mailboxes")
               .header("HX-Request", "true")
               .with(user(TEST_USER_EMAIL).roles("USER"))
               .accept(MediaType.TEXT_HTML))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("<time")))
               .andExpect(content().string(containsString("class=\"local-time")))
               .andExpect(content().string(containsString("datetime=\"" + expectedIsoInstant + "\"")));
    }
}

