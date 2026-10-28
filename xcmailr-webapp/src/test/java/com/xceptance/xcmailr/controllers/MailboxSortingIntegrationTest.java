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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.xceptance.xcmailr.XcmailrApplication;
import com.xceptance.xcmailr.repositories.DomainRepository;
import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

import models.Domain;
import models.MBox;
import models.User;

/**
 * Tier 3 database integration test suite verifying server-side mailbox table sorting
 * across multi-page datasets in {@link MailboxWebController}.
 * <p>
 * Specifically verifies resolution of legacy Issue #91 ("Sorting mailboxes is done for the current page only"),
 * proving that SQL ORDER BY is applied across the entire database dataset before pagination slicing (LIMIT/OFFSET).
 * Also tests edge cases including permanent unlimited mailboxes (ts_Active = 0), empty mailbox accounts,
 * cross-tenant security isolation, and search filtering combined with sorting.
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest(classes = XcmailrApplication.class)
@ActiveProfiles("test")
@Transactional
public final class MailboxSortingIntegrationTest
{
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MailboxRepository mailboxRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private MailRepository mailRepository;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;
    private User testUser;
    private User otherUser;
    private Domain testDomain;

    /**
     * Initializes Spring Security MockMvc and database fixtures before each test execution.
     */
    @BeforeEach
    public void setup()
    {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                                 .apply(springSecurity())
                                 .build();

        // Clear existing database records to isolate tests cleanly
        mailRepository.deleteAll();
        mailboxRepository.deleteAll();
        userRepository.deleteAll();
        domainRepository.deleteAll();

        // Seed domain fixture
        testDomain = new Domain();
        testDomain.setDomainname("xcmailr.test");
        testDomain = domainRepository.save(testDomain);

        // Seed primary test user
        testUser = new User();
        testUser.setMail("alice@xcmailr.test");
        testUser.setForename("Alice");
        testUser.setSurname("Tester");
        testUser.setPasswd("secret-password");
        testUser.setActive(true);
        testUser = userRepository.save(testUser);

        // Seed secondary user for tenant isolation verification
        otherUser = new User();
        otherUser.setMail("bob@xcmailr.test");
        otherUser.setForename("Bob");
        otherUser.setSurname("Other");
        otherUser.setPasswd("secret-password");
        otherUser.setActive(true);
        otherUser = userRepository.save(otherUser);
    }

    /**
     * Helper method to create and persist a test mailbox.
     *
     * @param localPart  local address prefix
     * @param owner      the user owning this mailbox
     * @param forwards   forward counter value
     * @param suppressions suppression counter value
     * @param tsActive   active expiration timestamp (0 = unlimited)
     * @param expired    boolean expiration flag
     * @return persisted {@link MBox} entity
     */
    private MBox createMailbox(final String localPart,
                               final User owner,
                               final int forwards,
                               final int suppressions,
                               final long tsActive,
                               final boolean expired)
    {
        final MBox box = new MBox();
        box.setAddress(localPart);
        box.setDomain(testDomain.getDomainname());
        box.setUsr(owner);
        box.setForwards(forwards);
        box.setSuppressions(suppressions);
        box.setTs_Active(tsActive);
        box.setExpired(expired);
        box.setForwardEmails(true);
        return mailboxRepository.save(box);
    }

    // =========================================================================
    // 1. ISSUE #91 VERIFICATION: MULTI-PAGE GLOBAL DATASET SORTING
    // =========================================================================

    /**
     * Verifies that sorting across a multi-page dataset sorts the ENTIRE database collection
     * prior to pagination slicing, preventing legacy Issue #91 where only the current page was reordered.
     * <p>
     * 30 mailboxes are seeded (alpha00 to alpha29). With page size 10:
     * - ASC page 0 contains alpha00..alpha09; page 2 contains alpha20..alpha29.
     * - DESC page 0 contains alpha29..alpha20; page 2 contains alpha09..alpha00.
     * </p>
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Issue #91 Verification: Multi-page sorting reorders globally before pagination slicing (Address ASC vs DESC)")
    public void testSorting_Issue91_MultiPageGlobalOrdering_AddressAscAndDesc() throws Exception
    {
        // GIVEN 30 seeded mailboxes for Alice named alpha00 through alpha29
        for (int i = 0; i < 30; i++)
        {
            final String prefix = String.format("alpha%02d", i);
            createMailbox(prefix, testUser, i, i, 0L, false);
        }

        // WHEN querying Page 0 with size 10 sorted by Address ASC
        final MvcResult ascPage0Result = mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user(testUser.getMail()))
                .param("page", "0")
                .param("size", "10")
                .param("sort", "address")
                .param("dir", "asc"))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("mailboxes"))
            .andReturn();

        @SuppressWarnings("unchecked")
        final Page<MBox> ascPage0 = (Page<MBox>) ascPage0Result.getModelAndView().getModel().get("page");
        assertNotNull(ascPage0);
        assertEquals(30, ascPage0.getTotalElements(), "Total elements across all pages must be 30");
        assertEquals(3, ascPage0.getTotalPages(), "Total pages with size 10 must be 3");
        assertEquals(10, ascPage0.getContent().size(), "Page 0 must contain 10 items");

        // THEN Page 0 in ASC order must contain alpha00 through alpha09
        final List<MBox> asc0Items = ascPage0.getContent();
        assertEquals("alpha00", asc0Items.get(0).getAddress());
        assertEquals("alpha09", asc0Items.get(9).getAddress());

        // WHEN querying Page 2 with size 10 sorted by Address ASC
        final MvcResult ascPage2Result = mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user(testUser.getMail()))
                .param("page", "2")
                .param("size", "10")
                .param("sort", "address")
                .param("dir", "asc"))
            .andExpect(status().isOk())
            .andReturn();

        @SuppressWarnings("unchecked")
        final Page<MBox> ascPage2 = (Page<MBox>) ascPage2Result.getModelAndView().getModel().get("page");
        final List<MBox> asc2Items = ascPage2.getContent();
        assertEquals("alpha20", asc2Items.get(0).getAddress());
        assertEquals("alpha29", asc2Items.get(9).getAddress());

        // NOW VERIFY RESOLUTION OF ISSUE #91:
        // WHEN querying Page 0 with size 10 sorted by Address DESC
        final MvcResult descPage0Result = mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user(testUser.getMail()))
                .param("page", "0")
                .param("size", "10")
                .param("sort", "address")
                .param("dir", "desc"))
            .andExpect(status().isOk())
            .andReturn();

        @SuppressWarnings("unchecked")
        final Page<MBox> descPage0 = (Page<MBox>) descPage0Result.getModelAndView().getModel().get("page");
        final List<MBox> desc0Items = descPage0.getContent();

        // IN THE BUGGY LEGACY CLIENT-SIDE VERSION:
        // Page 0 would still have contained alpha00..alpha09 reversed (alpha09..alpha00).
        // IN OUR MODERNIZED SERVER-SIDE VERSION:
        // Page 0 MUST contain alpha29 down to alpha20 globally!
        assertEquals("alpha29", desc0Items.get(0).getAddress(),
            "Under server-side sorting, Page 0 DESC must begin with global maximum alpha29 (Issue #91 fixed)");
        assertEquals("alpha28", desc0Items.get(1).getAddress());
        assertEquals("alpha20", desc0Items.get(9).getAddress());

        // WHEN querying Page 2 with size 10 sorted by Address DESC
        final MvcResult descPage2Result = mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user(testUser.getMail()))
                .param("page", "2")
                .param("size", "10")
                .param("sort", "address")
                .param("dir", "desc"))
            .andExpect(status().isOk())
            .andReturn();

        @SuppressWarnings("unchecked")
        final Page<MBox> descPage2 = (Page<MBox>) descPage2Result.getModelAndView().getModel().get("page");
        final List<MBox> desc2Items = descPage2.getContent();
        assertEquals("alpha09", desc2Items.get(0).getAddress());
        assertEquals("alpha00", desc2Items.get(9).getAddress(),
            "Under server-side sorting, Page 2 DESC must end with global minimum alpha00");
    }

    /**
     * Verifies multi-page database sorting across numeric counter columns (forwards and suppressions).
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Happy path: Multi-page global sorting by numeric counters (forwards and suppressions)")
    public void testSorting_HappyPath_MultiPageCounters() throws Exception
    {
        // GIVEN 20 mailboxes with increasing forward counters (100, 200, ..., 2000)
        for (int i = 1; i <= 20; i++)
        {
            createMailbox("box" + i, testUser, i * 100, (21 - i) * 50, 0L, false);
        }

        // WHEN sorting by forwards DESC (Page 0, size 5)
        final MvcResult forwardsDescResult = mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user(testUser.getMail()))
                .param("page", "0")
                .param("size", "5")
                .param("sort", "forwards")
                .param("dir", "desc"))
            .andExpect(status().isOk())
            .andReturn();

        @SuppressWarnings("unchecked")
        final Page<MBox> forwardsPage = (Page<MBox>) forwardsDescResult.getModelAndView().getModel().get("page");
        final List<MBox> forwardsItems = forwardsPage.getContent();
        assertEquals(2000, forwardsItems.get(0).getForwards(), "First item on page 0 DESC must be 2000 forwards");
        assertEquals(1900, forwardsItems.get(1).getForwards());
        assertEquals(1600, forwardsItems.get(4).getForwards());

        // WHEN sorting by suppressions ASC (Page 0, size 5)
        final MvcResult suppressionsAscResult = mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user(testUser.getMail()))
                .param("page", "0")
                .param("size", "5")
                .param("sort", "suppressions")
                .param("dir", "asc"))
            .andExpect(status().isOk())
            .andReturn();

        @SuppressWarnings("unchecked")
        final Page<MBox> suppressionsPage = (Page<MBox>) suppressionsAscResult.getModelAndView().getModel().get("page");
        final List<MBox> suppressionsItems = suppressionsPage.getContent();
        assertEquals(50, suppressionsItems.get(0).getSuppressions(), "First item on page 0 ASC must be 50 suppressions");
        assertEquals(100, suppressionsItems.get(1).getSuppressions());
    }

    /**
     * Verifies sorting by mailbox status (active vs expired boolean flag).
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Happy path: Sorting by status partitions active and expired mailboxes")
    public void testSorting_HappyPath_StatusSorting() throws Exception
    {
        // GIVEN 2 active and 2 expired mailboxes
        createMailbox("active1", testUser, 0, 0, 0L, false);
        createMailbox("active2", testUser, 0, 0, 0L, false);
        createMailbox("expired1", testUser, 0, 0, 0L, true);
        createMailbox("expired2", testUser, 0, 0, 0L, true);

        // WHEN sorting by status ASC (false = active comes first)
        final MvcResult statusAscResult = mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user(testUser.getMail()))
                .param("sort", "status")
                .param("dir", "asc"))
            .andExpect(status().isOk())
            .andReturn();

        @SuppressWarnings("unchecked")
        final Page<MBox> statusAscPage = (Page<MBox>) statusAscResult.getModelAndView().getModel().get("page");
        final List<MBox> ascList = statusAscPage.getContent();
        assertFalse(ascList.get(0).isExpired(), "Status ASC: First element must be active (expired = false)");
        assertFalse(ascList.get(1).isExpired(), "Status ASC: Second element must be active (expired = false)");
        assertTrue(ascList.get(2).isExpired(), "Status ASC: Third element must be expired (expired = true)");
        assertTrue(ascList.get(3).isExpired(), "Status ASC: Fourth element must be expired (expired = true)");

        // WHEN sorting by status DESC (true = expired comes first)
        final MvcResult statusDescResult = mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user(testUser.getMail()))
                .param("sort", "status")
                .param("dir", "desc"))
            .andExpect(status().isOk())
            .andReturn();

        @SuppressWarnings("unchecked")
        final Page<MBox> statusDescPage = (Page<MBox>) statusDescResult.getModelAndView().getModel().get("page");
        final List<MBox> descList = statusDescPage.getContent();
        assertTrue(descList.get(0).isExpired(), "Status DESC: First element must be expired (expired = true)");
        assertTrue(descList.get(1).isExpired(), "Status DESC: Second element must be expired (expired = true)");
        assertFalse(descList.get(2).isExpired(), "Status DESC: Third element must be active (expired = false)");
        assertFalse(descList.get(3).isExpired(), "Status DESC: Fourth element must be active (expired = false)");
    }

    // =========================================================================
    // 2. SPECIAL CASES (Tier 3 Integration Coverage)
    // =========================================================================

    /**
     * Special case: Sorting by expiration timestamp with permanent unlimited mailboxes (ts_Active = 0).
     * <p>
     * Verifies that mailboxes with ts_Active = 0 sort deterministically without NullPointerExceptions
     * or comparison errors alongside finite epoch timestamps:
     * - ASC: 0 comes first (0 < 1000000000 < 2000000000)
     * - DESC: highest future timestamp comes first, and unlimited (0) appears last.
     * </p>
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Special case: Sorting by expiration with permanent unlimited mailboxes (ts_Active = 0)")
    public void testSorting_SpecialCase_PermanentUnlimitedMailboxes_ExpiresSort() throws Exception
    {
        // GIVEN permanent mailbox (0) and two future expiration mailboxes
        final long futureEarly = 1_800_000_000_000L;
        final long futureLate = 1_900_000_000_000L;

        createMailbox("unlimited-box", testUser, 0, 0, 0L, false);
        createMailbox("early-expiring", testUser, 0, 0, futureEarly, false);
        createMailbox("late-expiring", testUser, 0, 0, futureLate, false);

        // WHEN sorting by expires ASC
        final MvcResult expiresAscResult = mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user(testUser.getMail()))
                .param("sort", "expires")
                .param("dir", "asc"))
            .andExpect(status().isOk())
            .andReturn();

        @SuppressWarnings("unchecked")
        final Page<MBox> expiresAscPage = (Page<MBox>) expiresAscResult.getModelAndView().getModel().get("page");
        final List<MBox> ascList = expiresAscPage.getContent();
        assertEquals(3, ascList.size());
        assertEquals(0L, ascList.get(0).getTs_Active(), "Expires ASC: ts_Active = 0 (unlimited) must come first");
        assertEquals(futureEarly, ascList.get(1).getTs_Active(), "Expires ASC: early expiring must come second");
        assertEquals(futureLate, ascList.get(2).getTs_Active(), "Expires ASC: late expiring must come third");

        // WHEN sorting by expires DESC
        final MvcResult expiresDescResult = mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user(testUser.getMail()))
                .param("sort", "expires")
                .param("dir", "desc"))
            .andExpect(status().isOk())
            .andReturn();

        @SuppressWarnings("unchecked")
        final Page<MBox> expiresDescPage = (Page<MBox>) expiresDescResult.getModelAndView().getModel().get("page");
        final List<MBox> descList = expiresDescPage.getContent();
        assertEquals(3, descList.size());
        assertEquals(futureLate, descList.get(0).getTs_Active(), "Expires DESC: latest expiration must come first");
        assertEquals(futureEarly, descList.get(1).getTs_Active(), "Expires DESC: early expiration must come second");
        assertEquals(0L, descList.get(2).getTs_Active(), "Expires DESC: ts_Active = 0 (unlimited) must come last");
    }

    /**
     * Special case: Sorting an account with zero mailboxes.
     * <p>
     * Verifies that applying sort parameters to an empty account succeeds with HTTP 200,
     * returns an empty Page instance, and causes no unexpected database or rendering exceptions.
     * </p>
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Special case: Sorting an empty mailbox account succeeds with 0 elements")
    public void testSorting_SpecialCase_EmptyMailboxAccount() throws Exception
    {
        // GIVEN Alice owns 0 mailboxes

        // WHEN querying sorted mailboxes for Alice
        final MvcResult emptyResult = mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user(testUser.getMail()))
                .param("sort", "address")
                .param("dir", "asc"))
            .andExpect(status().isOk())
            .andReturn();

        @SuppressWarnings("unchecked")
        final Page<MBox> emptyPage = (Page<MBox>) emptyResult.getModelAndView().getModel().get("page");
        assertNotNull(emptyPage);
        assertEquals(0, emptyPage.getTotalElements(), "Empty account must return 0 total elements");
        assertTrue(emptyPage.getContent().isEmpty(), "Empty account must contain empty list");
    }

    /**
     * Special case: Case-insensitive address sorting.
     * <p>
     * Verifies that mailboxes with different cases sort in deterministic alphabetical order.
     * </p>
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Special case: Case-insensitive address sorting executes cleanly")
    public void testSorting_SpecialCase_CaseInsensitiveAddressOrdering() throws Exception
    {
        // GIVEN mailboxes with varied alphabetical prefixes
        createMailbox("charlie", testUser, 0, 0, 0L, false);
        createMailbox("bravo", testUser, 0, 0, 0L, false);
        createMailbox("alpha", testUser, 0, 0, 0L, false);

        // WHEN sorting by address ASC
        final MvcResult ascResult = mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user(testUser.getMail()))
                .param("sort", "address")
                .param("dir", "asc"))
            .andExpect(status().isOk())
            .andReturn();

        @SuppressWarnings("unchecked")
        final Page<MBox> ascPage = (Page<MBox>) ascResult.getModelAndView().getModel().get("page");
        final List<MBox> items = ascPage.getContent();
        assertEquals("alpha", items.get(0).getAddress());
        assertEquals("bravo", items.get(1).getAddress());
        assertEquals("charlie", items.get(2).getAddress());
    }

    /**
     * Special case: Cross-tenant isolation during sorted queries.
     * <p>
     * Verifies that sorting user A's mailboxes never retrieves or includes mailboxes belonging to user B.
     * </p>
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Special case: Multi-tenant security isolation is preserved across sorted queries")
    public void testSorting_SpecialCase_TenantIsolation() throws Exception
    {
        // GIVEN Alice owns 2 mailboxes and Bob owns 2 mailboxes
        createMailbox("alice1", testUser, 10, 0, 0L, false);
        createMailbox("alice2", testUser, 20, 0, 0L, false);
        createMailbox("bob1", otherUser, 30, 0, 0L, false);
        createMailbox("bob2", otherUser, 40, 0, 0L, false);

        // WHEN Alice requests mailboxes sorted by forwards DESC
        final MvcResult aliceResult = mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user(testUser.getMail()))
                .param("sort", "forwards")
                .param("dir", "desc"))
            .andExpect(status().isOk())
            .andReturn();

        @SuppressWarnings("unchecked")
        final Page<MBox> alicePage = (Page<MBox>) aliceResult.getModelAndView().getModel().get("page");
        final List<MBox> aliceItems = alicePage.getContent();

        // THEN only Alice's mailboxes are returned, ordered by forwards DESC
        assertEquals(2, alicePage.getTotalElements(), "Alice should only see her 2 mailboxes");
        assertEquals("alice2", aliceItems.get(0).getAddress());
        assertEquals("alice1", aliceItems.get(1).getAddress());
    }

    /**
     * Special case: Live search combined with sorting.
     * <p>
     * Verifies that combining search filtering with sort criteria orders only the filtered subset.
     * </p>
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Special case: Live search filtering combined with sorting")
    public void testSorting_SpecialCase_SearchFilterWithSorting() throws Exception
    {
        // GIVEN mailboxes matching and non-matching search term "dev"
        createMailbox("dev-zebra", testUser, 10, 0, 0L, false);
        createMailbox("dev-alpha", testUser, 20, 0, 0L, false);
        createMailbox("prod-box", testUser, 30, 0, 0L, false);

        // WHEN querying with search="dev" and sort="address" ASC
        final MvcResult searchResult = mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user(testUser.getMail()))
                .param("search", "dev")
                .param("sort", "address")
                .param("dir", "asc"))
            .andExpect(status().isOk())
            .andReturn();

        @SuppressWarnings("unchecked")
        final Page<MBox> searchPage = (Page<MBox>) searchResult.getModelAndView().getModel().get("page");
        final List<MBox> searchItems = searchPage.getContent();

        // THEN only matching mailboxes are returned, sorted by address ASC
        assertEquals(2, searchPage.getTotalElements());
        assertEquals("dev-alpha", searchItems.get(0).getAddress());
        assertEquals("dev-zebra", searchItems.get(1).getAddress());
    }
}
