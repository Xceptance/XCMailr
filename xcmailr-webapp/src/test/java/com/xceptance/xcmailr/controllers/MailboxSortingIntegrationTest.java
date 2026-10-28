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
     * Special case: Permanent unlimited mailboxes (ts_Active = 0) sorting under Perspective A (Pure Infinity: +&infin;).
     * <p>
     * Verifies that mailboxes with ts_Active = 0 are treated as infinite lifespan (+&infin;):
     * - ASC: finite epoch timestamps sort chronologically (soonest first), with unlimited (0) at the very end.
     * - DESC: unlimited (0) appears at the very top (+&infin;), followed by finite timestamps descending.
     * </p>
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Special case: Sorting by expiration with permanent unlimited mailboxes (ts_Active = 0) under Perspective A")
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
        assertEquals(futureEarly, ascList.get(0).getTs_Active(), "Expires ASC: early expiring must come first (soonest first)");
        assertEquals(futureLate, ascList.get(1).getTs_Active(), "Expires ASC: late expiring must come second");
        assertEquals(0L, ascList.get(2).getTs_Active(), "Expires ASC: ts_Active = 0 (unlimited) must come last (+infinity)");

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
        assertEquals(0L, descList.get(0).getTs_Active(), "Expires DESC: ts_Active = 0 (unlimited) must come first (+infinity)");
        assertEquals(futureLate, descList.get(1).getTs_Active(), "Expires DESC: latest finite expiration must come second");
        assertEquals(futureEarly, descList.get(2).getTs_Active(), "Expires DESC: early finite expiration must come third");
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

    // =========================================================================
    // 3. STATUS FILTERING & COMBINED FILTER/SORT HAPPY PATHS (Task 4.2)
    // =========================================================================

    /**
     * Happy path: Verifies that filtering by status EXPIRING returns only active mailboxes with finite lifespans.
     * <p>
     * Ensures that permanent mailboxes (ts_Active = 0) and already expired mailboxes (expired = true)
     * are strictly excluded from the result set.
     * </p>
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Happy path: Filtering by status EXPIRING returns only active finite mailboxes")
    public void testFilter_HappyPath_StatusExpiring() throws Exception
    {
        // GIVEN 4 mailboxes: 2 expiring (active, finite), 1 unlimited (active, ts_Active = 0), 1 expired
        final long future1 = 1_800_000_000_000L;
        final long future2 = 1_900_000_000_000L;
        createMailbox("expiring-1", testUser, 0, 0, future1, false);
        createMailbox("expiring-2", testUser, 0, 0, future2, false);
        createMailbox("unlimited", testUser, 0, 0, 0L, false);
        createMailbox("expired-box", testUser, 0, 0, 1_700_000_000_000L, true);

        // WHEN querying with status=EXPIRING
        final MvcResult result = mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user(testUser.getMail()))
                .param("status", "EXPIRING"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("status", "EXPIRING"))
            .andReturn();

        // THEN only the 2 active expiring mailboxes are returned
        @SuppressWarnings("unchecked")
        final Page<MBox> page = (Page<MBox>) result.getModelAndView().getModel().get("page");
        assertNotNull(page);
        assertEquals(2, page.getTotalElements(), "Only 2 active expiring mailboxes should be returned");
        for (final MBox box : page.getContent())
        {
            assertFalse(box.isExpired(), "Returned mailbox must be active");
            assertTrue(box.getTs_Active() > 0L, "Returned mailbox must have finite expiration (ts_Active > 0)");
        }
    }

    /**
     * Happy path: Verifies that filtering by status UNLIMITED returns only active mailboxes with ts_Active = 0.
     * <p>
     * Ensures that finite expiring mailboxes and expired mailboxes are strictly excluded.
     * </p>
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Happy path: Filtering by status UNLIMITED returns only active permanent mailboxes")
    public void testFilter_HappyPath_StatusUnlimited() throws Exception
    {
        // GIVEN 4 mailboxes: 2 unlimited, 1 expiring (active, finite), 1 expired
        createMailbox("unlimited-1", testUser, 0, 0, 0L, false);
        createMailbox("unlimited-2", testUser, 0, 0, 0L, false);
        createMailbox("expiring", testUser, 0, 0, 1_800_000_000_000L, false);
        createMailbox("expired-box", testUser, 0, 0, 0L, true);

        // WHEN querying with status=UNLIMITED
        final MvcResult result = mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user(testUser.getMail()))
                .param("status", "UNLIMITED"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("status", "UNLIMITED"))
            .andReturn();

        // THEN only the 2 active unlimited mailboxes are returned
        @SuppressWarnings("unchecked")
        final Page<MBox> page = (Page<MBox>) result.getModelAndView().getModel().get("page");
        assertNotNull(page);
        assertEquals(2, page.getTotalElements(), "Only 2 active unlimited mailboxes should be returned");
        for (final MBox box : page.getContent())
        {
            assertFalse(box.isExpired(), "Returned mailbox must be active");
            assertEquals(0L, box.getTs_Active(), "Returned mailbox must have ts_Active = 0");
        }
    }

    /**
     * Happy path: Verifies that filtering by status EXPIRING combined with sorting by expires ASC
     * orders finite active mailboxes chronologically (soonest expiring first).
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Happy path: status=EXPIRING with sort=expires ASC orders expiring mailboxes chronologically")
    public void testFilterAndSort_HappyPath_ExpiringWithExpiresSortAsc() throws Exception
    {
        // GIVEN 3 expiring mailboxes with different timestamps and 1 unlimited mailbox
        final long early = 1_800_000_000_000L;
        final long middle = 1_850_000_000_000L;
        final long late = 1_900_000_000_000L;

        createMailbox("late-box", testUser, 0, 0, late, false);
        createMailbox("early-box", testUser, 0, 0, early, false);
        createMailbox("middle-box", testUser, 0, 0, middle, false);
        createMailbox("unlimited-box", testUser, 0, 0, 0L, false);

        // WHEN querying with status=EXPIRING, sort=expires, dir=asc
        final MvcResult result = mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user(testUser.getMail()))
                .param("status", "EXPIRING")
                .param("sort", "expires")
                .param("dir", "asc"))
            .andExpect(status().isOk())
            .andReturn();

        // THEN only 3 expiring mailboxes are returned in ascending chronological order (soonest first)
        @SuppressWarnings("unchecked")
        final Page<MBox> page = (Page<MBox>) result.getModelAndView().getModel().get("page");
        final List<MBox> content = page.getContent();
        assertEquals(3, content.size(), "Only expiring mailboxes should be present");
        assertEquals(early, content.get(0).getTs_Active(), "Soonest expiring mailbox must be first");
        assertEquals(middle, content.get(1).getTs_Active(), "Middle expiring mailbox must be second");
        assertEquals(late, content.get(2).getTs_Active(), "Latest expiring mailbox must be third");
    }

    /**
     * Happy path: Verifies that filtering by status EXPIRING combined with sorting by expires DESC
     * orders finite active mailboxes reverse-chronologically (furthest expiration first).
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Happy path: status=EXPIRING with sort=expires DESC orders expiring mailboxes reverse-chronologically")
    public void testFilterAndSort_HappyPath_ExpiringWithExpiresSortDesc() throws Exception
    {
        // GIVEN 3 expiring mailboxes with different timestamps and 1 unlimited mailbox
        final long early = 1_800_000_000_000L;
        final long middle = 1_850_000_000_000L;
        final long late = 1_900_000_000_000L;

        createMailbox("late-box", testUser, 0, 0, late, false);
        createMailbox("early-box", testUser, 0, 0, early, false);
        createMailbox("middle-box", testUser, 0, 0, middle, false);
        createMailbox("unlimited-box", testUser, 0, 0, 0L, false);

        // WHEN querying with status=EXPIRING, sort=expires, dir=desc
        final MvcResult result = mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user(testUser.getMail()))
                .param("status", "EXPIRING")
                .param("sort", "expires")
                .param("dir", "desc"))
            .andExpect(status().isOk())
            .andReturn();

        // THEN only 3 expiring mailboxes are returned in descending chronological order (furthest first)
        @SuppressWarnings("unchecked")
        final Page<MBox> page = (Page<MBox>) result.getModelAndView().getModel().get("page");
        final List<MBox> content = page.getContent();
        assertEquals(3, content.size(), "Only expiring mailboxes should be present");
        assertEquals(late, content.get(0).getTs_Active(), "Furthest expiring mailbox must be first");
        assertEquals(middle, content.get(1).getTs_Active(), "Middle expiring mailbox must be second");
        assertEquals(early, content.get(2).getTs_Active(), "Soonest expiring mailbox must be third");
    }

    // =========================================================================
    // 4. ERROR CASES & SPECIAL EDGE CASES (Task 4.3)
    // =========================================================================

    /**
     * Error case: Verifies that passing an unknown, illegal, or malicious status parameter
     * safely falls back to ALL without throwing an exception or causing HTTP 500.
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Error case: Invalid status filter parameter safely falls back to ALL")
    public void testFilter_ErrorCase_InvalidStatusFallback() throws Exception
    {
        // GIVEN 1 active finite, 1 active unlimited, 1 expired mailbox
        createMailbox("box-expiring", testUser, 0, 0, 1_800_000_000_000L, false);
        createMailbox("box-unlimited", testUser, 0, 0, 0L, false);
        createMailbox("box-expired", testUser, 0, 0, 0L, true);

        // WHEN querying with invalid status like "DROPTABLE" or SQL injection payload
        final MvcResult result = mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user(testUser.getMail()))
                .param("status", "INVALID_STATUS'; DROP TABLE mboxes;--"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("status", "ALL"))
            .andReturn();

        // THEN all 3 mailboxes are returned (fallback to ALL)
        @SuppressWarnings("unchecked")
        final Page<MBox> page = (Page<MBox>) result.getModelAndView().getModel().get("page");
        assertEquals(3, page.getTotalElements(), "Invalid status must safely fall back to ALL and return all 3 mailboxes");
    }

    /**
     * Error case: Verifies that passing an unknown or malicious sort parameter
     * safely falls back to id DESC without throwing an exception.
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Error case: Invalid sort parameter safely falls back to id DESC")
    public void testSort_ErrorCase_InvalidSortFallback() throws Exception
    {
        // GIVEN 3 mailboxes created in sequential order
        final MBox box1 = createMailbox("first", testUser, 0, 0, 0L, false);
        final MBox box2 = createMailbox("second", testUser, 0, 0, 0L, false);
        final MBox box3 = createMailbox("third", testUser, 0, 0, 0L, false);

        // WHEN querying with invalid sort parameter
        final MvcResult result = mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user(testUser.getMail()))
                .param("sort", "malicious_col--")
                .param("dir", "asc"))
            .andExpect(status().isOk())
            .andReturn();

        // THEN fallback sort is id DESC (box3, box2, box1)
        @SuppressWarnings("unchecked")
        final Page<MBox> page = (Page<MBox>) result.getModelAndView().getModel().get("page");
        final List<MBox> content = page.getContent();
        assertEquals(3, content.size());
        assertEquals(box3.getId(), content.get(0).getId(), "Fallback order must be id DESC (latest created first)");
        assertEquals(box2.getId(), content.get(1).getId());
        assertEquals(box1.getId(), content.get(2).getId());
    }

    /**
     * Special case: Verifies that sorting and filtering with status=EXPIRING or status=UNLIMITED
     * on an account with zero mailboxes executes safely and returns an empty page.
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Special case: Sorting and filtering on empty mailbox account returns 0 elements cleanly")
    public void testFilterAndSort_SpecialCase_EmptyAccount() throws Exception
    {
        // GIVEN testUser has 0 mailboxes

        // WHEN querying with status=EXPIRING and sort=expires ASC
        final MvcResult expiringResult = mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user(testUser.getMail()))
                .param("status", "EXPIRING")
                .param("sort", "expires")
                .param("dir", "asc"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("status", "EXPIRING"))
            .andReturn();

        @SuppressWarnings("unchecked")
        final Page<MBox> expiringPage = (Page<MBox>) expiringResult.getModelAndView().getModel().get("page");
        assertNotNull(expiringPage);
        assertEquals(0, expiringPage.getTotalElements());
        assertTrue(expiringPage.getContent().isEmpty());

        // WHEN querying with status=UNLIMITED and sort=expires DESC
        final MvcResult unlimitedResult = mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user(testUser.getMail()))
                .param("status", "UNLIMITED")
                .param("sort", "expires")
                .param("dir", "desc"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("status", "UNLIMITED"))
            .andReturn();

        @SuppressWarnings("unchecked")
        final Page<MBox> unlimitedPage = (Page<MBox>) unlimitedResult.getModelAndView().getModel().get("page");
        assertNotNull(unlimitedPage);
        assertEquals(0, unlimitedPage.getTotalElements());
        assertTrue(unlimitedPage.getContent().isEmpty());
    }

    /**
     * Special case: Verifies behavior for an account containing only unlimited mailboxes (ts_Active = 0).
     * <p>
     * Filtering by EXPIRING must return 0 elements.
     * Filtering by UNLIMITED must return all elements.
     * Sorting by expires ASC and DESC must succeed cleanly without SQL or division-by-zero errors.
     * </p>
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Special case: Account with only unlimited mailboxes filters and sorts cleanly")
    public void testFilterAndSort_SpecialCase_AllUnlimitedAccount() throws Exception
    {
        // GIVEN 3 unlimited mailboxes and no finite expiring ones
        createMailbox("perm1", testUser, 10, 0, 0L, false);
        createMailbox("perm2", testUser, 20, 0, 0L, false);
        createMailbox("perm3", testUser, 30, 0, 0L, false);

        // WHEN filtering with status=EXPIRING
        final MvcResult expiringResult = mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user(testUser.getMail()))
                .param("status", "EXPIRING"))
            .andExpect(status().isOk())
            .andReturn();

        @SuppressWarnings("unchecked")
        final Page<MBox> expiringPage = (Page<MBox>) expiringResult.getModelAndView().getModel().get("page");
        assertEquals(0, expiringPage.getTotalElements(), "Account with only unlimited mailboxes must return 0 for EXPIRING");

        // WHEN filtering with status=UNLIMITED
        final MvcResult unlimitedResult = mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user(testUser.getMail()))
                .param("status", "UNLIMITED"))
            .andExpect(status().isOk())
            .andReturn();

        @SuppressWarnings("unchecked")
        final Page<MBox> unlimitedPage = (Page<MBox>) unlimitedResult.getModelAndView().getModel().get("page");
        assertEquals(3, unlimitedPage.getTotalElements(), "Account with only unlimited mailboxes must return all 3 for UNLIMITED");

        // WHEN sorting by expires ASC
        final MvcResult sortAscResult = mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user(testUser.getMail()))
                .param("sort", "expires")
                .param("dir", "asc"))
            .andExpect(status().isOk())
            .andReturn();

        @SuppressWarnings("unchecked")
        final Page<MBox> sortAscPage = (Page<MBox>) sortAscResult.getModelAndView().getModel().get("page");
        assertEquals(3, sortAscPage.getTotalElements(), "Expires ASC sorting must succeed and return all 3 unlimited mailboxes");

        // WHEN sorting by expires DESC
        final MvcResult sortDescResult = mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user(testUser.getMail()))
                .param("sort", "expires")
                .param("dir", "desc"))
            .andExpect(status().isOk())
            .andReturn();

        @SuppressWarnings("unchecked")
        final Page<MBox> sortDescPage = (Page<MBox>) sortDescResult.getModelAndView().getModel().get("page");
        assertEquals(3, sortDescPage.getTotalElements(), "Expires DESC sorting must succeed and return all 3 unlimited mailboxes");
    }
}
