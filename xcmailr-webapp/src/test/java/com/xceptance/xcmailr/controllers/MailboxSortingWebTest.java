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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
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
 * Tier 2 WebMvc Slice Test Suite for server-side mailbox table sorting, modern chevron indicators,
 * responsive mobile layout attributes, and HTMX interactions.
 * <p>
 * Validates that table fragment rendering correctly outputs modern Bootstrap chevron indicator icons
 * ({@code bi-chevron-expand} for inactive sortable columns, {@code bi-chevron-up} for active ascending,
 * and {@code bi-chevron-down} for active descending), verifies mobile-responsive attributes
 * ({@code text-nowrap}, {@code d-inline-flex}, {@code py-1}), confirms state preservation across pagination,
 * and tests graceful fallback behavior for invalid or malicious sort criteria.
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest(classes = XcmailrApplication.class)
@ActiveProfiles("test")
public final class MailboxSortingWebTest
{
    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private MailboxRepository mailboxRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private MailRepository mailRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private Domain domain;

    /**
     * Resets repository state and initializes test user and domain fixtures before each test execution.
     */
    @BeforeEach
    public void setup()
    {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        mailRepository.deleteAll();
        mailboxRepository.deleteAll();
        userRepository.deleteAll();
        domainRepository.deleteAll();

        domain = new Domain("xcmailr.test");
        domainRepository.save(domain);

        owner = new User("Alice", "Smith", "alice@xcmailr.test", "Password123", "en");
        owner.setActive(true);
        userRepository.save(owner);
    }

    // =========================================================================
    // 1. HAPPY PATH TESTS (Tier 2 Slice Coverage)
    // =========================================================================

    /**
     * Verifies that the default initial load without sort parameters defaults to creation order (id DESC),
     * rendering neutral bi-chevron-expand icons on all sortable columns without directional arrows.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Happy path: Default view (id DESC) renders neutral bi-chevron-expand icons across all sortable headers")
    public void testSorting_HappyPath_DefaultView_RendersNeutralChevronIconsAcrossSortableHeaders() throws Exception
    {
        final MBox box = new MBox("alpha", "xcmailr.test", 0L, false, owner);
        mailboxRepository.save(box);

        mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(view().name("mailboxes/fragments/mailbox-table :: table"))
            .andExpect(model().attribute("sort", "id"))
            .andExpect(model().attribute("dir", "desc"))
            // Hidden input state preservation reflects default creation order
            .andExpect(content().string(containsString("name=\"sort\" value=\"id\"")))
            .andExpect(content().string(containsString("name=\"dir\" value=\"desc\"")))
            // Neutral expand chevrons must be rendered for inactive sortable columns
            .andExpect(content().string(containsString("bi bi-chevron-expand sort-icon text-body-secondary opacity-50 ms-1")))
            // No active directional chevrons rendered when default id sorting is active
            .andExpect(content().string(not(containsString("bi bi-chevron-up sort-icon text-primary"))))
            .andExpect(content().string(not(containsString("bi bi-chevron-down sort-icon text-primary"))));
    }

    /**
     * Verifies that sorting by Address ASC renders the up-chevron icon on the Address header,
     * toggles the target direction to DESC in the header link, outputs hidden sort inputs,
     * and preserves neutral expand chevrons on non-active columns.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Happy path: Address ASC renders up-chevron, toggles link to DESC, and outputs hidden inputs")
    public void testSorting_HappyPath_AddressAscending_RendersUpChevronAndToggledDescLink() throws Exception
    {
        final MBox box = new MBox("alpha", "xcmailr.test", 0L, false, owner);
        mailboxRepository.save(box);

        mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user("alice@xcmailr.test"))
                .param("sort", "address")
                .param("dir", "asc"))
            .andExpect(status().isOk())
            .andExpect(view().name("mailboxes/fragments/mailbox-table :: table"))
            .andExpect(model().attribute("sort", "address"))
            .andExpect(model().attribute("dir", "asc"))
            // Hidden input state preservation
            .andExpect(content().string(containsString("name=\"sort\" value=\"address\"")))
            .andExpect(content().string(containsString("name=\"dir\" value=\"asc\"")))
            // Active direction chevron on Address
            .andExpect(content().string(containsString("bi bi-chevron-up sort-icon text-primary ms-1")))
            // Inactive columns retain neutral expand chevrons
            .andExpect(content().string(containsString("bi bi-chevron-expand sort-icon text-body-secondary opacity-50 ms-1")))
            // Header link toggled to 'desc'
            .andExpect(content().string(containsString("sort=address")))
            .andExpect(content().string(containsString("dir=desc")))
            // Mails column remains static without sorting link
            .andExpect(content().string(containsString("<th scope=\"col\" class=\"text-center\" style=\"width: 75px;\" title=\"Stored emails in webmail inbox\">Mails</th>")));
    }

    /**
     * Verifies that sorting by Address DESC renders the down-chevron icon on the Address header
     * and toggles the target direction to ASC.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Happy path: Address DESC renders down-chevron and toggles link to ASC")
    public void testSorting_HappyPath_AddressDescending_RendersDownChevronAndToggledAscLink() throws Exception
    {
        final MBox box = new MBox("omega", "xcmailr.test", 0L, false, owner);
        mailboxRepository.save(box);

        mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user("alice@xcmailr.test"))
                .param("sort", "address")
                .param("dir", "desc"))
            .andExpect(status().isOk())
            .andExpect(view().name("mailboxes/fragments/mailbox-table :: table"))
            .andExpect(model().attribute("sort", "address"))
            .andExpect(model().attribute("dir", "desc"))
            // Active direction down chevron
            .andExpect(content().string(containsString("bi bi-chevron-down sort-icon text-primary ms-1")))
            // Header link toggled to 'asc'
            .andExpect(content().string(containsString("sort=address")))
            .andExpect(content().string(containsString("dir=asc")));
    }

    /**
     * Verifies that sorting by Status ASC renders the active sort up-chevron on the Status header.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Happy path: Status ASC renders sort indicator on Status column")
    public void testSorting_HappyPath_StatusSort_RendersIndicator() throws Exception
    {
        final MBox box = new MBox("status-box", "xcmailr.test", 0L, false, owner);
        mailboxRepository.save(box);

        mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user("alice@xcmailr.test"))
                .param("sort", "status")
                .param("dir", "asc"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("sort", "status"))
            .andExpect(model().attribute("dir", "asc"))
            .andExpect(content().string(containsString("Sort by Status")))
            .andExpect(content().string(containsString("bi bi-chevron-up sort-icon text-primary ms-1")));
    }

    /**
     * Verifies that sorting by Expires DESC renders the down-chevron on the Expires header.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Happy path: Expires DESC renders sort indicator on Expires column")
    public void testSorting_HappyPath_ExpiresSort_RendersIndicator() throws Exception
    {
        final MBox box = new MBox("expires-box", "xcmailr.test", 0L, false, owner);
        mailboxRepository.save(box);

        mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user("alice@xcmailr.test"))
                .param("sort", "expires")
                .param("dir", "desc"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("sort", "expires"))
            .andExpect(model().attribute("dir", "desc"))
            .andExpect(content().string(containsString("Sort by Expiration")))
            .andExpect(content().string(containsString("bi bi-chevron-down sort-icon text-primary ms-1")));
    }

    /**
     * Verifies that sorting by Forwards ASC renders the up-chevron on the Forwards header.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Happy path: Forwards ASC renders sort indicator on Forwards column")
    public void testSorting_HappyPath_ForwardsSort_RendersIndicator() throws Exception
    {
        final MBox box = new MBox("forwards-box", "xcmailr.test", 0L, false, owner);
        mailboxRepository.save(box);

        mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user("alice@xcmailr.test"))
                .param("sort", "forwards")
                .param("dir", "asc"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("sort", "forwards"))
            .andExpect(model().attribute("dir", "asc"))
            .andExpect(content().string(containsString("Total forwarded emails")))
            .andExpect(content().string(containsString("bi bi-chevron-up sort-icon text-primary ms-1")));
    }

    /**
     * Verifies that sorting by Suppressions DESC renders the down-chevron on the Suppressed header.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Happy path: Suppressions DESC renders sort indicator on Suppressed column")
    public void testSorting_HappyPath_SuppressionsSort_RendersIndicator() throws Exception
    {
        final MBox box = new MBox("suppressed-box", "xcmailr.test", 0L, false, owner);
        mailboxRepository.save(box);

        mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user("alice@xcmailr.test"))
                .param("sort", "suppressions")
                .param("dir", "desc"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("sort", "suppressions"))
            .andExpect(model().attribute("dir", "desc"))
            .andExpect(content().string(containsString("Suppressed incoming emails while inactive")))
            .andExpect(content().string(containsString("bi bi-chevron-down sort-icon text-primary ms-1")));
    }

    /**
     * Verifies that pagination links propagate the active sort and dir parameters across pages.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Happy path: Pagination navigation links include sort and dir parameters")
    public void testSorting_HappyPath_PaginationLinks_PreserveSortAndDir() throws Exception
    {
        // GIVEN 20 mailboxes to trigger multi-page pagination (size = 15)
        final List<MBox> boxes = new ArrayList<>();
        for (int i = 0; i < 20; i++)
        {
            boxes.add(new MBox("box" + i, "xcmailr.test", 0L, false, owner));
        }
        mailboxRepository.saveAll(boxes);

        // WHEN requesting page 0 sorted by forwards ASC
        mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user("alice@xcmailr.test"))
                .param("page", "0")
                .param("sort", "forwards")
                .param("dir", "asc"))
            .andExpect(status().isOk())
            // THEN pagination links must include sort=forwards and dir=asc
            .andExpect(content().string(containsString("sort=forwards")))
            .andExpect(content().string(containsString("dir=asc")))
            .andExpect(content().string(containsString("page=1")));
    }

    /**
     * Verifies that the table container hx-include contains sort and dir input selectors.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Happy path: Table container hx-include attribute includes [name='sort'] and [name='dir']")
    public void testSorting_HappyPath_ContainerHxInclude_PreservesSortAndDir() throws Exception
    {
        mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("hx-include=\"[name='search'], #statusFilterSelect, [name='sort'], [name='dir']\"")));
    }

    // =========================================================================
    // 2. RESPONSIVE & ACCESSIBILITY TESTS (Mobile-First Attributes)
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
        mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            // Sortable header class and responsive non-wrapping layout
            .andExpect(content().string(containsString("class=\"sortable-header text-decoration-none text-body d-inline-flex align-items-center gap-1 text-nowrap py-1\"")))
            // Screen reader accessibility: aria-hidden="true" on chevron icons
            .andExpect(content().string(containsString("aria-hidden=\"true\"")));
    }

    // =========================================================================
    // 3. ERROR & FALLBACK CASES (Tier 2 Slice Coverage)
    // =========================================================================

    /**
     * Verifies that an invalid sort parameter defaults cleanly to id DESC without throwing errors
     * and renders neutral sortable chevron indicators without active directional arrows.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Error case: Unrecognized sort parameter safely defaults to id DESC with neutral chevrons")
    public void testSorting_ErrorCase_UnrecognizedSortParameter_DefaultsToIdDescAndNeutralChevrons() throws Exception
    {
        final MBox box = new MBox("valid-box", "xcmailr.test", 0L, false, owner);
        mailboxRepository.save(box);

        mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user("alice@xcmailr.test"))
                .param("sort", "arbitrary_nonexistent_field")
                .param("dir", "invalid_dir"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("sort", "id"))
            .andExpect(model().attribute("dir", "desc"))
            .andExpect(content().string(containsString("name=\"sort\" value=\"id\"")))
            .andExpect(content().string(containsString("name=\"dir\" value=\"desc\"")))
            // Renders neutral expand chevrons on headers when defaulting to id
            .andExpect(content().string(containsString("bi bi-chevron-expand sort-icon text-body-secondary opacity-50 ms-1")))
            // No active directional arrows or chevrons rendered
            .andExpect(content().string(not(containsString("bi bi-chevron-up sort-icon text-primary"))))
            .andExpect(content().string(not(containsString("bi bi-chevron-down sort-icon text-primary"))));
    }

    /**
     * Verifies that SQL injection attempts in sort parameter are sanitized safely to id DESC.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Error case: SQL injection attempt in sort parameter safely sanitized to id DESC")
    public void testSorting_ErrorCase_SqlInjectionAttempt_SafelyDefaultsToIdDesc() throws Exception
    {
        mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user("alice@xcmailr.test"))
                .param("sort", "id; DROP TABLE mailboxes;--")
                .param("dir", "asc"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("sort", "id"))
            .andExpect(model().attribute("dir", "desc"));
    }

    // =========================================================================
    // 4. SPECIAL CASES (Tier 2 Slice Coverage)
    // =========================================================================

    /**
     * Verifies that header sort links preserve active search and status filter parameters.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Special case: Sort header links preserve active search query and status filter")
    public void testSorting_SpecialCase_PreservesSearchAndFilterParametersInSortHeaderLinks() throws Exception
    {
        final MBox box = new MBox("test-box", "xcmailr.test", 0L, false, owner);
        mailboxRepository.save(box);

        mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user("alice@xcmailr.test"))
                .param("search", "my-query")
                .param("status", "ACTIVE")
                .param("sort", "suppressions")
                .param("dir", "asc"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("search=my-query")))
            .andExpect(content().string(containsString("status=ACTIVE")))
            .andExpect(content().string(containsString("sort=suppressions")));
    }

    /**
     * Verifies that a full page render of the mailbox index contains sort parameters in the
     * search input and status filter dropdown hx-include attributes.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Special case: Full index page render includes sort and dir in search and filter hx-include")
    public void testSorting_SpecialCase_FullPageRender_IndexIncludesSortInSearchAndFilter() throws Exception
    {
        mockMvc.perform(get("/mailboxes")
                .with(user("alice@xcmailr.test"))
                .param("sort", "expires")
                .param("dir", "asc"))
            .andExpect(status().isOk())
            .andExpect(view().name("mailboxes/index"))
            .andExpect(content().string(containsString("name=\"search\"")))
            .andExpect(content().string(containsString("hx-include=\"#statusFilterSelect, [name='sort'], [name='dir']\"")))
            .andExpect(content().string(containsString("id=\"statusFilterSelect\"")))
            .andExpect(content().string(containsString("hx-include=\"[name='search'], [name='sort'], [name='dir']\"")));
    }
}
