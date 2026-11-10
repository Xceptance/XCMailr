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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.xceptance.xcmailr.XcmailrApplication;
import com.xceptance.xcmailr.models.User;
import com.xceptance.xcmailr.repositories.DomainRepository;
import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailTransactionRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

/**
 * Layer 2 WebMvc Slice Test Suite for server-side user table sorting on {@code /admin/users},
 * bidirectional chevron indicator icons, parameter preservation across pagination and display count,
 * directory-style composite Name ordering, search query combination, and defensive fallbacks.
 * <p>
 * Validates that user table fragment rendering correctly outputs modern Bootstrap chevron indicator icons
 * ({@code bi-chevron-expand} for inactive sortable columns, {@code bi-chevron-up} for active ascending,
 * and {@code bi-chevron-down} for active descending), verifies mobile-responsive attributes
 * ({@code text-nowrap}, {@code d-inline-flex}, {@code py-1}), confirms state preservation across pagination
 * and display count presets, and tests graceful fallback behavior for invalid sort criteria.
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest(classes = XcmailrApplication.class)
@ActiveProfiles("test")
public final class AdminUsersSortingWebTest
{
    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailboxRepository mailboxRepository;

    @Autowired
    private MailRepository mailRepository;

    @Autowired
    private MailTransactionRepository transactionRepository;

    @Autowired
    private DomainRepository domainRepository;

    private User admin;

    /**
     * Resets repository state and initializes primary administrative fixture before each test execution.
     */
    @BeforeEach
    public void setup()
    {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        transactionRepository.deleteAll();
        mailRepository.deleteAll();
        mailboxRepository.deleteAll();
        userRepository.deleteAll();
        domainRepository.deleteAll();

        // Create primary administrator for authenticated administrative requests
        admin = new User("System", "Administrator", "admin@xcmailr.test", "AdminPassword123!", "en");
        admin.setActive(true);
        admin.setAdmin(true);
        userRepository.save(admin);
    }

    // =========================================================================
    // 1. HAPPY PATH TESTS (Layer 2 WebMvc Slice Coverage)
    // =========================================================================

    /**
     * Verifies that the default initial load without sort parameters defaults to Email ASC,
     * rendering an up-chevron on the Email header and neutral expand chevrons on all other headers.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Happy path: Default view (email ASC) renders up-chevron on Email and neutral expand icons on other headers")
    public void testSorting_HappyPath_DefaultView_RendersEmailAscending() throws Exception
    {
        final User userAlpha = new User("Alpha", "User", "alpha@xcmailr.test", "Pw123!", "en");
        userAlpha.setActive(true);
        userRepository.save(userAlpha);

        mockMvc.perform(get("/admin/users")
                .header("HX-Request", "true")
                .with(user("admin@xcmailr.test").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/fragments/user-table :: table"))
            .andExpect(model().attribute("sort", "email"))
            .andExpect(model().attribute("dir", "asc"))
            // Hidden input state preservation reflects active sort criteria
            .andExpect(content().string(containsString("name=\"sort\" value=\"email\"")))
            .andExpect(content().string(containsString("name=\"dir\" value=\"asc\"")))
            // Active up-chevron on Email column
            .andExpect(content().string(containsString("bi bi-chevron-up sort-icon text-primary ms-1")))
            // Email header toggles target direction to DESC
            .andExpect(content().string(containsString("sort=email")))
            .andExpect(content().string(containsString("dir=desc")))
            // Neutral expand chevrons rendered for inactive sortable columns (Name, Status, Role)
            .andExpect(content().string(containsString("bi bi-chevron-expand sort-icon text-body-secondary opacity-50 ms-1")));
    }

    /**
     * Verifies that sorting by Email DESC renders the down-chevron icon on the Email header,
     * toggles the link direction to ASC, and correctly orders user rows descending by email.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Happy path: Email DESC renders down-chevron, toggles link to ASC, and orders rows descending")
    public void testSorting_HappyPath_EmailDescending_RendersDownChevronAndDescendingOrder() throws Exception
    {
        final User userAlpha = new User("Alpha", "User", "alpha@xcmailr.test", "Pw123!", "en");
        userAlpha.setActive(true);
        final User userZebra = new User("Zebra", "User", "zebra@xcmailr.test", "Pw123!", "en");
        userZebra.setActive(true);
        userRepository.save(userAlpha);
        userRepository.save(userZebra);

        final MvcResult result = mockMvc.perform(get("/admin/users")
                .header("HX-Request", "true")
                .with(user("admin@xcmailr.test").roles("ADMIN"))
                .param("sort", "email")
                .param("dir", "desc"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/fragments/user-table :: table"))
            .andExpect(model().attribute("sort", "email"))
            .andExpect(model().attribute("dir", "desc"))
            // Active down-chevron on Email
            .andExpect(content().string(containsString("bi bi-chevron-down sort-icon text-primary ms-1")))
            // Header link toggled to 'asc'
            .andExpect(content().string(containsString("sort=email")))
            .andExpect(content().string(containsString("dir=asc")))
            .andReturn();

        final String html = result.getResponse().getContentAsString();

        // Verify row ordering in DESC: zebra comes before alpha
        final int zebraIdx = html.indexOf("id=\"user-row-" + userZebra.getId() + "\"");
        final int alphaIdx = html.indexOf("id=\"user-row-" + userAlpha.getId() + "\"");

        assertTrue(zebraIdx != -1, "Zebra user row must be present in HTML");
        assertTrue(alphaIdx != -1, "Alpha user row must be present in HTML");
        assertTrue(zebraIdx < alphaIdx, "In Email DESC, zebra must appear before alpha");
    }

    /**
     * Verifies that sorting by Name ASC sorts directory-style (surname ASC, forename ASC)
     * and renders the active up-chevron on the Name header.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Happy path: Name ASC sorts composite surname/forename ascending with up-chevron")
    public void testSorting_HappyPath_NameAscending_SortsCompositeSurnameForename() throws Exception
    {
        // 1. Adams, Bob
        final User u1 = new User("Bob", "Adams", "bob.adams@xcmailr.test", "Pw123!", "en");
        u1.setActive(true);
        // 2. Smith, Alice
        final User u2 = new User("Alice", "Smith", "alice.smith@xcmailr.test", "Pw123!", "en");
        u2.setActive(true);
        // 3. Smith, Zoe (same surname Smith, forename Zoe > Alice)
        final User u3 = new User("Zoe", "Smith", "zoe.smith@xcmailr.test", "Pw123!", "en");
        u3.setActive(true);
        // 4. Taylor, Charlie
        final User u4 = new User("Charlie", "Taylor", "charlie.taylor@xcmailr.test", "Pw123!", "en");
        u4.setActive(true);

        userRepository.saveAll(List.of(u1, u2, u3, u4));

        final MvcResult result = mockMvc.perform(get("/admin/users")
                .header("HX-Request", "true")
                .with(user("admin@xcmailr.test").roles("ADMIN"))
                .param("sort", "name")
                .param("dir", "asc"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("sort", "name"))
            .andExpect(model().attribute("dir", "asc"))
            .andExpect(content().string(containsString("bi bi-chevron-up sort-icon text-primary ms-1")))
            .andReturn();

        final String html = result.getResponse().getContentAsString();

        final int u1Idx = html.indexOf("id=\"user-row-" + u1.getId() + "\"");
        final int u2Idx = html.indexOf("id=\"user-row-" + u2.getId() + "\"");
        final int u3Idx = html.indexOf("id=\"user-row-" + u3.getId() + "\"");
        final int u4Idx = html.indexOf("id=\"user-row-" + u4.getId() + "\"");

        assertTrue(u1Idx != -1 && u2Idx != -1 && u3Idx != -1 && u4Idx != -1, "All user rows must be rendered");
        assertTrue(u1Idx < u2Idx, "Adams, Bob must precede Smith, Alice");
        assertTrue(u2Idx < u3Idx, "Smith, Alice must precede Smith, Zoe (secondary forename ASC)");
        assertTrue(u3Idx < u4Idx, "Smith, Zoe must precede Taylor, Charlie");
    }

    /**
     * Verifies that sorting by Name DESC sorts directory-style (surname DESC, forename DESC)
     * and renders the active down-chevron on the Name header.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Happy path: Name DESC sorts composite surname/forename descending with down-chevron")
    public void testSorting_HappyPath_NameDescending_SortsCompositeSurnameForename() throws Exception
    {
        final User u1 = new User("Bob", "Adams", "bob.adams@xcmailr.test", "Pw123!", "en");
        u1.setActive(true);
        final User u2 = new User("Alice", "Smith", "alice.smith@xcmailr.test", "Pw123!", "en");
        u2.setActive(true);
        final User u3 = new User("Zoe", "Smith", "zoe.smith@xcmailr.test", "Pw123!", "en");
        u3.setActive(true);
        final User u4 = new User("Charlie", "Taylor", "charlie.taylor@xcmailr.test", "Pw123!", "en");
        u4.setActive(true);

        userRepository.saveAll(List.of(u1, u2, u3, u4));

        final MvcResult result = mockMvc.perform(get("/admin/users")
                .header("HX-Request", "true")
                .with(user("admin@xcmailr.test").roles("ADMIN"))
                .param("sort", "name")
                .param("dir", "desc"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("sort", "name"))
            .andExpect(model().attribute("dir", "desc"))
            .andExpect(content().string(containsString("bi bi-chevron-down sort-icon text-primary ms-1")))
            .andReturn();

        final String html = result.getResponse().getContentAsString();

        final int u1Idx = html.indexOf("id=\"user-row-" + u1.getId() + "\"");
        final int u2Idx = html.indexOf("id=\"user-row-" + u2.getId() + "\"");
        final int u3Idx = html.indexOf("id=\"user-row-" + u3.getId() + "\"");
        final int u4Idx = html.indexOf("id=\"user-row-" + u4.getId() + "\"");

        assertTrue(u1Idx != -1 && u2Idx != -1 && u3Idx != -1 && u4Idx != -1, "All user rows must be rendered");
        assertTrue(u4Idx < u3Idx, "Taylor, Charlie must precede Smith, Zoe");
        assertTrue(u3Idx < u2Idx, "Smith, Zoe must precede Smith, Alice (secondary forename DESC)");
        assertTrue(u2Idx < u1Idx, "Smith, Alice must precede Adams, Bob");
    }

    /**
     * Verifies that sorting by Status ASC places inactive users before active users,
     * applying the secondary mail ASC tiebreaker.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Happy path: Status ASC groups inactive users before active users with mail ASC tiebreaker")
    public void testSorting_HappyPath_StatusAscending_GroupsInactiveBeforeActive() throws Exception
    {
        final User inactiveUser = new User("Inactive", "User", "inactive@xcmailr.test", "Pw123!", "en");
        inactiveUser.setActive(false);
        final User activeUser = new User("Active", "User", "active@xcmailr.test", "Pw123!", "en");
        activeUser.setActive(true);

        userRepository.save(inactiveUser);
        userRepository.save(activeUser);

        final MvcResult result = mockMvc.perform(get("/admin/users")
                .header("HX-Request", "true")
                .with(user("admin@xcmailr.test").roles("ADMIN"))
                .param("sort", "status")
                .param("dir", "asc"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("sort", "status"))
            .andExpect(model().attribute("dir", "asc"))
            .andExpect(content().string(containsString("bi bi-chevron-up sort-icon text-primary ms-1")))
            .andReturn();

        final String html = result.getResponse().getContentAsString();

        final int inactiveIdx = html.indexOf("id=\"user-row-" + inactiveUser.getId() + "\"");
        final int activeIdx = html.indexOf("id=\"user-row-" + activeUser.getId() + "\"");

        assertTrue(inactiveIdx != -1 && activeIdx != -1, "Both users must be rendered");
        assertTrue(inactiveIdx < activeIdx, "In Status ASC, inactive (false) must precede active (true)");
    }

    /**
     * Verifies that sorting by Role ASC groups standard users before administrators,
     * applying the secondary mail ASC tiebreaker.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Happy path: Role ASC groups standard users before admins with mail ASC tiebreaker")
    public void testSorting_HappyPath_RoleAscending_GroupsStandardUsersBeforeAdmins() throws Exception
    {
        final User regular = new User("Regular", "User", "regular@xcmailr.test", "Pw123!", "en");
        regular.setActive(true);
        regular.setAdmin(false);
        userRepository.save(regular);

        final MvcResult result = mockMvc.perform(get("/admin/users")
                .header("HX-Request", "true")
                .with(user("admin@xcmailr.test").roles("ADMIN"))
                .param("sort", "role")
                .param("dir", "asc"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("sort", "role"))
            .andExpect(model().attribute("dir", "asc"))
            .andExpect(content().string(containsString("bi bi-chevron-up sort-icon text-primary ms-1")))
            .andReturn();

        final String html = result.getResponse().getContentAsString();

        final int regularIdx = html.indexOf("id=\"user-row-" + regular.getId() + "\"");
        final int adminIdx = html.indexOf("id=\"user-row-" + admin.getId() + "\"");

        assertTrue(regularIdx != -1 && adminIdx != -1, "Both users must be rendered");
        assertTrue(regularIdx < adminIdx, "In Role ASC, standard user (admin=false) must precede admin (admin=true)");
    }

    /**
     * Verifies that pagination navigation links propagate active sort and dir parameters across pages.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Happy path: Pagination navigation links preserve sort and dir parameters")
    public void testSorting_HappyPath_PaginationLinks_PreserveSortAndDir() throws Exception
    {
        // Create 20 additional users to trigger multi-page pagination (size = 15)
        final List<User> userList = new ArrayList<>();
        for (int i = 0; i < 20; i++)
        {
            final User u = new User("First" + i, "Last" + i, "user" + i + "@xcmailr.test", "Pw123!", "en");
            u.setActive(true);
            userList.add(u);
        }
        userRepository.saveAll(userList);

        mockMvc.perform(get("/admin/users")
                .header("HX-Request", "true")
                .with(user("admin@xcmailr.test").roles("ADMIN"))
                .param("page", "0")
                .param("sort", "name")
                .param("dir", "desc"))
            .andExpect(status().isOk())
            // Pagination links must include active sort and dir
            .andExpect(content().string(containsString("sort=name")))
            .andExpect(content().string(containsString("dir=desc")))
            .andExpect(content().string(containsString("page=1")));
    }

    /**
     * Verifies that Display Count presets propagate active sort and dir parameters.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Happy path: Display Count presets preserve sort and dir parameters")
    public void testSorting_HappyPath_DisplayCountPresets_PreserveSortAndDir() throws Exception
    {
        mockMvc.perform(get("/admin/users")
                .header("HX-Request", "true")
                .with(user("admin@xcmailr.test").roles("ADMIN"))
                .param("sort", "role")
                .param("dir", "desc"))
            .andExpect(status().isOk())
            // Display Count buttons must preserve active sort and dir
            .andExpect(content().string(containsString("size=5")))
            .andExpect(content().string(containsString("size=25")))
            .andExpect(content().string(containsString("size=50")))
            .andExpect(content().string(containsString("sort=role")))
            .andExpect(content().string(containsString("dir=desc")));
    }

    /**
     * Verifies that the table container hx-include attribute contains sort, dir, search, and size selectors.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Happy path: Table container hx-include attribute includes [name='sort'], [name='dir'], and [name='size']")
    public void testSorting_HappyPath_ContainerHxInclude_PreservesSortAndDir() throws Exception
    {
        mockMvc.perform(get("/admin/users")
                .header("HX-Request", "true")
                .with(user("admin@xcmailr.test").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("hx-include=\"[name='search'], [name='sort'], [name='dir'], [name='size']\"")));
    }

    // =========================================================================
    // 2. RESPONSIVE & ACCESSIBILITY TESTS (Mobile-First Layout)
    // =========================================================================

    /**
     * Verifies that sortable header links contain required mobile responsiveness and accessibility attributes:
     * sortable-header class, text-body for theme compatibility, d-inline-flex and text-nowrap to prevent icon wrapping,
     * py-1 for touchscreen tap targets, and aria-hidden="true" on indicator icons.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Responsive & Accessibility: Sortable headers contain non-wrapping and touch-padding attributes")
    public void testSorting_ResponsiveAndAccessibility_AttributesPresent() throws Exception
    {
        mockMvc.perform(get("/admin/users")
                .header("HX-Request", "true")
                .with(user("admin@xcmailr.test").roles("ADMIN")))
            .andExpect(status().isOk())
            // Sortable header class and responsive layout
            .andExpect(content().string(containsString("class=\"sortable-header text-decoration-none text-body d-inline-flex align-items-center gap-1 text-nowrap py-1\"")))
            // Actions header flex wrapper for uniform baseline
            .andExpect(content().string(containsString("<span class=\"d-inline-flex align-items-center py-1\"")))
            // Screen reader accessibility: aria-hidden="true" on chevron icons
            .andExpect(content().string(containsString("aria-hidden=\"true\"")));
    }

    // =========================================================================
    // 3. ERROR & FALLBACK CASES (Layer 2 WebMvc Slice Coverage)
    // =========================================================================

    /**
     * Verifies that an invalid sort parameter defaults cleanly to email DESC without throwing errors
     * and renders safely.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Error case: Unrecognized sort parameter safely defaults to email DESC with clean rendering")
    public void testSorting_ErrorCase_UnrecognizedSortParameter_DefaultsToEmailDesc() throws Exception
    {
        mockMvc.perform(get("/admin/users")
                .header("HX-Request", "true")
                .with(user("admin@xcmailr.test").roles("ADMIN"))
                .param("sort", "nonexistent_field")
                .param("dir", "invalid_dir"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("sort", "email"))
            .andExpect(model().attribute("dir", "desc"))
            .andExpect(content().string(containsString("name=\"sort\" value=\"email\"")))
            .andExpect(content().string(containsString("name=\"dir\" value=\"desc\"")));
    }

    // =========================================================================
    // 4. SPECIAL CASES (Layer 2 WebMvc Slice Coverage)
    // =========================================================================

    /**
     * Verifies case-insensitive composite Name ordering across lower and uppercase surnames.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Special case: Case-insensitive composite Name ordering across varied casing")
    public void testSorting_SpecialCase_CaseInsensitiveCompositeNameOrdering() throws Exception
    {
        final User u1 = new User("Alice", "smith", "alice@xcmailr.test", "Pw123!", "en");
        u1.setActive(true);
        final User u2 = new User("Bob", "SMITH", "bob@xcmailr.test", "Pw123!", "en");
        u2.setActive(true);
        final User u3 = new User("Charlie", "Taylor", "charlie@xcmailr.test", "Pw123!", "en");
        u3.setActive(true);

        userRepository.saveAll(List.of(u1, u2, u3));

        final MvcResult result = mockMvc.perform(get("/admin/users")
                .header("HX-Request", "true")
                .with(user("admin@xcmailr.test").roles("ADMIN"))
                .param("sort", "name")
                .param("dir", "asc"))
            .andExpect(status().isOk())
            .andReturn();

        final String html = result.getResponse().getContentAsString();

        final int u1Idx = html.indexOf("id=\"user-row-" + u1.getId() + "\"");
        final int u2Idx = html.indexOf("id=\"user-row-" + u2.getId() + "\"");
        final int u3Idx = html.indexOf("id=\"user-row-" + u3.getId() + "\"");

        assertTrue(u1Idx < u2Idx, "smith, Alice must precede SMITH, Bob (case-insensitive surname, then forename)");
        assertTrue(u2Idx < u3Idx, "SMITH, Bob must precede Taylor, Charlie");
    }

    /**
     * Verifies that search filtering combines seamlessly with server-side column sorting.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Special case: Combined search filtering and column sorting")
    public void testSorting_SpecialCase_CombinedSearchAndSort() throws Exception
    {
        final User target1 = new User("Dan", "Smith", "dan.smith@xcmailr.test", "Pw123!", "en");
        target1.setActive(true);
        final User target2 = new User("Amy", "Smith", "amy.smith@xcmailr.test", "Pw123!", "en");
        target2.setActive(true);
        final User other = new User("Walter", "White", "walter@xcmailr.test", "Pw123!", "en");
        other.setActive(true);

        userRepository.saveAll(List.of(target1, target2, other));

        final MvcResult result = mockMvc.perform(get("/admin/users")
                .header("HX-Request", "true")
                .with(user("admin@xcmailr.test").roles("ADMIN"))
                .param("search", "Smith")
                .param("sort", "email")
                .param("dir", "asc"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("dan.smith@xcmailr.test")))
            .andExpect(content().string(containsString("amy.smith@xcmailr.test")))
            .andExpect(content().string(not(containsString("walter@xcmailr.test"))))
            .andReturn();

        final String html = result.getResponse().getContentAsString();

        final int amyIdx = html.indexOf("id=\"user-row-" + target2.getId() + "\"");
        final int danIdx = html.indexOf("id=\"user-row-" + target1.getId() + "\"");

        assertTrue(amyIdx < danIdx, "In Email ASC, amy.smith must precede dan.smith");
    }

    /**
     * Verifies deterministic ordering for duplicate primary column values via mail ASC tiebreaker.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Special case: Duplicate primary sort values ordered deterministically by mail ASC tiebreaker")
    public void testSorting_SpecialCase_DuplicateValuesDeterministicOrderingViaTiebreaker() throws Exception
    {
        // 3 inactive users sharing identical active=false status
        final User uC = new User("User", "C", "c_inactive@xcmailr.test", "Pw123!", "en");
        uC.setActive(false);
        final User uA = new User("User", "A", "a_inactive@xcmailr.test", "Pw123!", "en");
        uA.setActive(false);
        final User uB = new User("User", "B", "b_inactive@xcmailr.test", "Pw123!", "en");
        uB.setActive(false);

        userRepository.saveAll(List.of(uC, uA, uB));

        final MvcResult result = mockMvc.perform(get("/admin/users")
                .header("HX-Request", "true")
                .with(user("admin@xcmailr.test").roles("ADMIN"))
                .param("sort", "status")
                .param("dir", "asc"))
            .andExpect(status().isOk())
            .andReturn();

        final String html = result.getResponse().getContentAsString();

        final int uAIdx = html.indexOf("id=\"user-row-" + uA.getId() + "\"");
        final int uBIdx = html.indexOf("id=\"user-row-" + uB.getId() + "\"");
        final int uCIdx = html.indexOf("id=\"user-row-" + uC.getId() + "\"");

        assertTrue(uAIdx < uBIdx, "Tiebreaker mail ASC must order a_inactive before b_inactive");
        assertTrue(uBIdx < uCIdx, "Tiebreaker mail ASC must order b_inactive before c_inactive");
    }

    /**
     * Verifies that full page render of the users index includes sort and dir parameters in the search input hx-include.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Special case: Full page render includes sort, dir, and size in search hx-include")
    public void testSorting_SpecialCase_FullPageRender_IncludesSortInSearchHxInclude() throws Exception
    {
        mockMvc.perform(get("/admin/users")
                .with(user("admin@xcmailr.test").roles("ADMIN"))
                .param("sort", "name")
                .param("dir", "desc"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/users"))
            .andExpect(content().string(containsString("name=\"search\"")))
            .andExpect(content().string(containsString("hx-include=\"[name='sort'], [name='dir'], [name='size']\"")));
    }

    /**
     * Verifies that when users are sorted by Email descending, row action endpoints
     * (toggle-active, toggle-admin, delete) in Row 1 strictly bind to the displayed user ID
     * with zero references to other user IDs.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Issue #83 regression: Email DESC binds row action endpoints strictly to displayed user ID")
    public void testSorting_Issue83_EmailDescending_BindsRowActionsStrictlyToDisplayedUser() throws Exception
    {
        final User userAlpha = new User("Alpha", "User", "alpha@xcmailr.test", "Pw123!", "en");
        userAlpha.setActive(true);
        final User userZebra = new User("Zebra", "User", "zebra@xcmailr.test", "Pw123!", "en");
        userZebra.setActive(true);

        userRepository.save(userAlpha);
        userRepository.save(userZebra);

        final MvcResult result = mockMvc.perform(get("/admin/users")
                .header("HX-Request", "true")
                .with(user("admin@xcmailr.test").roles("ADMIN"))
                .param("sort", "email")
                .param("dir", "desc"))
            .andExpect(status().isOk())
            .andReturn();

        final String html = result.getResponse().getContentAsString();

        final String zebraRowMarker = "id=\"user-row-" + userZebra.getId() + "\"";
        final String alphaRowMarker = "id=\"user-row-" + userAlpha.getId() + "\"";

        final int zebraIdx = html.indexOf(zebraRowMarker);
        final int alphaIdx = html.indexOf(alphaRowMarker);

        assertTrue(zebraIdx != -1, "Zebra user row must be rendered");
        assertTrue(alphaIdx != -1, "Alpha user row must be rendered");
        assertTrue(zebraIdx < alphaIdx, "In Email DESC, zebra must appear before alpha");

        final String zebraRow = html.substring(zebraIdx, alphaIdx);

        // Actions in zebra row must strictly target zebra's ID
        assertTrue(zebraRow.contains("/admin/users/" + userZebra.getId() + "/toggle-active"), "Zebra row must target Zebra ID for toggle-active");
        assertTrue(zebraRow.contains("/admin/users/" + userZebra.getId() + "/toggle-admin"), "Zebra row must target Zebra ID for toggle-admin");
        assertTrue(zebraRow.contains("/admin/users/" + userZebra.getId() + "\""), "Zebra row must target Zebra ID for delete");

        // Zebra row must never contain alpha's ID
        assertFalse(zebraRow.contains("/admin/users/" + userAlpha.getId() + "/"), "Zebra row must not reference Alpha ID");
    }
}
