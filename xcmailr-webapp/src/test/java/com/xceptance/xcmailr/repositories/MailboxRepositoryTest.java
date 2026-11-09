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
package com.xceptance.xcmailr.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.xceptance.xcmailr.models.MBox;
import com.xceptance.xcmailr.models.User;

/**
 * Unit and integration tests for {@link MailboxRepository} batch methods and tenant boundaries.
 * Follows the testing pyramid covering happy paths, tenant security boundaries, and edge cases.
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class MailboxRepositoryTest
{
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailboxRepository mailboxRepository;

    @Autowired
    private MailRepository mailRepository;

    private User primaryUser;
    private User foreignUser;

    @BeforeEach
    public void setUp()
    {
        mailRepository.deleteAll();
        mailboxRepository.deleteAll();
        userRepository.deleteAll();

        primaryUser = new User("Alice", "Tester", "alice@example.com", "secret", "en");
        userRepository.save(primaryUser);

        foreignUser = new User("Bob", "Other", "bob@example.com", "secret", "en");
        userRepository.save(foreignUser);
    }

    @Test
    @DisplayName("findByUsrAndIdIn returns only mailboxes belonging to the requesting user")
    public void testFindByUsrAndIdInTenantIsolation()
    {
        final MBox box1 = new MBox("alice1", "xcmailr.test", 0L, false, primaryUser);
        final MBox box2 = new MBox("alice2", "xcmailr.test", 0L, false, primaryUser);
        final MBox foreignBox = new MBox("bob1", "xcmailr.test", 0L, false, foreignUser);

        mailboxRepository.save(box1);
        mailboxRepository.save(box2);
        mailboxRepository.save(foreignBox);

        final List<Long> requestedIds = List.of(box1.getId(), box2.getId(), foreignBox.getId());
        final List<MBox> aliceBoxes = mailboxRepository.findByUsrAndIdIn(primaryUser, requestedIds);

        assertEquals(2, aliceBoxes.size(), "Should only return mailboxes owned by primaryUser");
        assertTrue(aliceBoxes.stream().anyMatch(b -> b.getId() == box1.getId()));
        assertTrue(aliceBoxes.stream().anyMatch(b -> b.getId() == box2.getId()));
        assertFalse(aliceBoxes.stream().anyMatch(b -> b.getId() == foreignBox.getId()));
    }

    @Test
    @DisplayName("findByUsrAndIdIn returns empty list when given empty ID collection")
    public void testFindByUsrAndIdInEmptyList()
    {
        final MBox box = new MBox("alice1", "xcmailr.test", 0L, false, primaryUser);
        mailboxRepository.save(box);

        final List<MBox> result = mailboxRepository.findByUsrAndIdIn(primaryUser, Collections.emptyList());
        assertTrue(result.isEmpty(), "Empty ID list query must return empty list");
    }

    @Test
    @DisplayName("resetCountersByUsrAndIds resets counters for specified user mailboxes only")
    public void testResetCountersByUsrAndIds()
    {
        final MBox box1 = new MBox("alice1", "xcmailr.test", 0L, false, primaryUser);
        box1.setForwards(10);
        box1.setSuppressions(5);
        mailboxRepository.save(box1);

        final MBox box2 = new MBox("alice2", "xcmailr.test", 0L, false, primaryUser);
        box2.setForwards(3);
        box2.setSuppressions(2);
        mailboxRepository.save(box2);

        final MBox foreignBox = new MBox("bob1", "xcmailr.test", 0L, false, foreignUser);
        foreignBox.setForwards(20);
        foreignBox.setSuppressions(8);
        mailboxRepository.save(foreignBox);

        final List<Long> idsToReset = List.of(box1.getId(), foreignBox.getId());
        final int updatedCount = mailboxRepository.resetCountersByUsrAndIds(primaryUser, idsToReset);

        assertEquals(1, updatedCount, "Only the primaryUser's mailbox should have been updated");

        final MBox reloadedBox1 = mailboxRepository.findById(box1.getId()).orElseThrow();
        assertEquals(0, reloadedBox1.getForwards());
        assertEquals(0, reloadedBox1.getSuppressions());

        final MBox reloadedBox2 = mailboxRepository.findById(box2.getId()).orElseThrow();
        assertEquals(3, reloadedBox2.getForwards(), "Untargeted box counters must remain unchanged");
        assertEquals(2, reloadedBox2.getSuppressions());

        final MBox reloadedForeignBox = mailboxRepository.findById(foreignBox.getId()).orElseThrow();
        assertEquals(20, reloadedForeignBox.getForwards(), "Foreign user box counters must remain unchanged");
        assertEquals(8, reloadedForeignBox.getSuppressions());
    }

    @Test
    @DisplayName("findByUsrAndExpired accurately partitions active vs expired mailboxes")
    public void testFindByUsrAndExpired()
    {
        final MBox activeBox1 = new MBox("active1", "xcmailr.test", 0L, false, primaryUser);
        activeBox1.setExpired(false);
        mailboxRepository.save(activeBox1);

        final MBox activeBox2 = new MBox("active2", "xcmailr.test", 0L, false, primaryUser);
        activeBox2.setExpired(false);
        mailboxRepository.save(activeBox2);

        final MBox expiredBox = new MBox("expired1", "xcmailr.test", 0L, false, primaryUser);
        expiredBox.setExpired(true);
        mailboxRepository.save(expiredBox);

        final List<MBox> activeList = mailboxRepository.findByUsrAndExpired(primaryUser, false);
        assertEquals(2, activeList.size());
        assertTrue(activeList.stream().noneMatch(MBox::isExpired));

        final List<MBox> expiredList = mailboxRepository.findByUsrAndExpired(primaryUser, true);
        assertEquals(1, expiredList.size());
        assertTrue(expiredList.get(0).isExpired());
    }

    /**
     * Tier 2 Repository Slice Test: Verifies that {@code findExpiringByUsr} returns exclusively
     * active mailboxes with a finite future expiration timestamp (expired = false AND ts_Active > 0).
     * Permanent unlimited mailboxes (ts_Active = 0), deactivated mailboxes (expired = true),
     * and foreign tenant mailboxes must be strictly excluded.
     */
    @Test
    @DisplayName("findExpiringByUsr returns only active finite mailboxes owned by primary user")
    public void testFindExpiringByUsr_HappyPathAndExclusions()
    {
        final long futureEpoch = System.currentTimeMillis() + 86_400_000L;

        // 1. Target: Active finite mailbox (should be included)
        final MBox activeExpiring = new MBox("expiring1", "xcmailr.test", futureEpoch, false, primaryUser);
        activeExpiring.setExpired(false);
        mailboxRepository.save(activeExpiring);

        // 2. Active unlimited mailbox (ts_Active = 0, should be excluded)
        final MBox activeUnlimited = new MBox("unlimited1", "xcmailr.test", 0L, false, primaryUser);
        activeUnlimited.setExpired(false);
        mailboxRepository.save(activeUnlimited);

        // 3. Expired finite mailbox (expired = true, should be excluded)
        final MBox expiredFinite = new MBox("expiredfinite", "xcmailr.test", futureEpoch, false, primaryUser);
        expiredFinite.setExpired(true);
        mailboxRepository.save(expiredFinite);

        // 4. Expired unlimited mailbox (expired = true AND ts_Active = 0, should be excluded)
        final MBox expiredUnlimited = new MBox("expiredunlimited", "xcmailr.test", 0L, false, primaryUser);
        expiredUnlimited.setExpired(true);
        mailboxRepository.save(expiredUnlimited);

        // 5. Foreign user active finite mailbox (tenant boundary, should be excluded)
        final MBox foreignExpiring = new MBox("foreignexpiring", "xcmailr.test", futureEpoch, false, foreignUser);
        foreignExpiring.setExpired(false);
        mailboxRepository.save(foreignExpiring);

        final Pageable pageable = PageRequest.of(0, 10);
        final Page<MBox> resultPage = mailboxRepository.findExpiringByUsr(primaryUser, pageable);

        assertEquals(1, resultPage.getTotalElements(), "Exactly one active finite mailbox should be returned");
        assertEquals("expiring1", resultPage.getContent().get(0).getAddress());
        assertEquals(futureEpoch, resultPage.getContent().get(0).getTs_Active());
        assertFalse(resultPage.getContent().get(0).isExpired());
    }

    /**
     * Tier 2 Repository Slice Test: Verifies that {@code findExpiringByUsrAndSearch} matches
     * active finite mailboxes by address prefix or domain substring case-insensitively,
     * while excluding non-matching, expired, unlimited, or foreign records.
     */
    @Test
    @DisplayName("findExpiringByUsrAndSearch matches address and domain substrings for active finite mailboxes")
    public void testFindExpiringByUsrAndSearch_PrefixAndDomainMatching()
    {
        final long futureEpoch = System.currentTimeMillis() + 86_400_000L;

        // Matches by address prefix
        final MBox matchByAddress = new MBox("searchme1", "alpha.test", futureEpoch, false, primaryUser);
        matchByAddress.setExpired(false);
        mailboxRepository.save(matchByAddress);

        // Matches by domain substring
        final MBox matchByDomain = new MBox("otherbox", "custom-search-domain.test", futureEpoch, false, primaryUser);
        matchByDomain.setExpired(false);
        mailboxRepository.save(matchByDomain);

        // Does not match search query
        final MBox noMatch = new MBox("nomatch", "beta.test", futureEpoch, false, primaryUser);
        noMatch.setExpired(false);
        mailboxRepository.save(noMatch);

        // Matches search query but is unlimited (should be excluded)
        final MBox unlimitedMatching = new MBox("searchme-unlimited", "alpha.test", 0L, false, primaryUser);
        unlimitedMatching.setExpired(false);
        mailboxRepository.save(unlimitedMatching);

        // Matches search query but belongs to foreign tenant (should be excluded)
        final MBox foreignMatching = new MBox("searchme-foreign", "alpha.test", futureEpoch, false, foreignUser);
        foreignMatching.setExpired(false);
        mailboxRepository.save(foreignMatching);

        final Pageable pageable = PageRequest.of(0, 10);
        final Page<MBox> searchResult = mailboxRepository.findExpiringByUsrAndSearch(primaryUser, "search", pageable);

        assertEquals(2, searchResult.getTotalElements(), "Should match exactly the two active finite mailboxes matching search");
        final List<String> matchedAddresses = searchResult.getContent().stream().map(MBox::getAddress).toList();
        assertTrue(matchedAddresses.contains("searchme1"));
        assertTrue(matchedAddresses.contains("otherbox"));
    }

    /**
     * Tier 2 Repository Slice Test: Verifies that {@code findUnlimitedByUsr} returns exclusively
     * active permanent mailboxes (expired = false AND ts_Active = 0).
     * Finite mailboxes (ts_Active > 0), deactivated mailboxes (expired = true),
     * and foreign tenant mailboxes must be strictly excluded.
     */
    @Test
    @DisplayName("findUnlimitedByUsr returns only active permanent mailboxes owned by primary user")
    public void testFindUnlimitedByUsr_HappyPathAndExclusions()
    {
        final long futureEpoch = System.currentTimeMillis() + 86_400_000L;

        // 1. Target: Active unlimited mailbox (should be included)
        final MBox activeUnlimited = new MBox("unlimited1", "xcmailr.test", 0L, false, primaryUser);
        activeUnlimited.setExpired(false);
        mailboxRepository.save(activeUnlimited);

        // 2. Active finite mailbox (ts_Active > 0, should be excluded)
        final MBox activeFinite = new MBox("finite1", "xcmailr.test", futureEpoch, false, primaryUser);
        activeFinite.setExpired(false);
        mailboxRepository.save(activeFinite);

        // 3. Expired unlimited mailbox (expired = true AND ts_Active = 0, should be excluded)
        final MBox expiredUnlimited = new MBox("expiredunlimited", "xcmailr.test", 0L, false, primaryUser);
        expiredUnlimited.setExpired(true);
        mailboxRepository.save(expiredUnlimited);

        // 4. Foreign user active unlimited mailbox (tenant boundary, should be excluded)
        final MBox foreignUnlimited = new MBox("foreignunlimited", "xcmailr.test", 0L, false, foreignUser);
        foreignUnlimited.setExpired(false);
        mailboxRepository.save(foreignUnlimited);

        final Pageable pageable = PageRequest.of(0, 10);
        final Page<MBox> resultPage = mailboxRepository.findUnlimitedByUsr(primaryUser, pageable);

        assertEquals(1, resultPage.getTotalElements(), "Exactly one active permanent mailbox should be returned");
        assertEquals("unlimited1", resultPage.getContent().get(0).getAddress());
        assertEquals(0L, resultPage.getContent().get(0).getTs_Active());
        assertFalse(resultPage.getContent().get(0).isExpired());
    }

    /**
     * Tier 2 Repository Slice Test: Verifies that {@code findUnlimitedByUsrAndSearch} matches
     * active permanent mailboxes by address prefix or domain substring case-insensitively,
     * while excluding non-matching, expired, finite, or foreign records.
     */
    @Test
    @DisplayName("findUnlimitedByUsrAndSearch matches address and domain substrings for active permanent mailboxes")
    public void testFindUnlimitedByUsrAndSearch_PrefixAndDomainMatching()
    {
        final long futureEpoch = System.currentTimeMillis() + 86_400_000L;

        // Matches by address prefix
        final MBox matchByAddress = new MBox("findme-perm", "alpha.test", 0L, false, primaryUser);
        matchByAddress.setExpired(false);
        mailboxRepository.save(matchByAddress);

        // Matches by domain substring
        final MBox matchByDomain = new MBox("otherperm", "target-findme.test", 0L, false, primaryUser);
        matchByDomain.setExpired(false);
        mailboxRepository.save(matchByDomain);

        // Does not match search query
        final MBox noMatch = new MBox("nomatch", "beta.test", 0L, false, primaryUser);
        noMatch.setExpired(false);
        mailboxRepository.save(noMatch);

        // Matches search query but has finite expiration (should be excluded)
        final MBox finiteMatching = new MBox("findme-finite", "alpha.test", futureEpoch, false, primaryUser);
        finiteMatching.setExpired(false);
        mailboxRepository.save(finiteMatching);

        // Matches search query but is expired (should be excluded)
        final MBox expiredMatching = new MBox("findme-expired", "alpha.test", 0L, false, primaryUser);
        expiredMatching.setExpired(true);
        mailboxRepository.save(expiredMatching);

        // Matches search query but belongs to foreign tenant (should be excluded)
        final MBox foreignMatching = new MBox("findme-foreign", "alpha.test", 0L, false, foreignUser);
        foreignMatching.setExpired(false);
        mailboxRepository.save(foreignMatching);

        final Pageable pageable = PageRequest.of(0, 10);
        final Page<MBox> searchResult = mailboxRepository.findUnlimitedByUsrAndSearch(primaryUser, "findme", pageable);

        assertEquals(2, searchResult.getTotalElements(), "Should match exactly the two active permanent mailboxes matching search");
        final List<String> matchedAddresses = searchResult.getContent().stream().map(MBox::getAddress).toList();
        assertTrue(matchedAddresses.contains("findme-perm"));
        assertTrue(matchedAddresses.contains("otherperm"));
    }

    /**
     * Tier 2 Repository Slice Test: Verifies persistence and retrieval of comments containing
     * special characters, symbols, and German umlauts (ä, ö, ü, ß).
     */
    @Test
    @DisplayName("Persist and retrieve mailbox with comment containing German umlauts and special characters")
    public void testPersistAndRetrieveMailboxWithComment_UmlautsAndSpecialChars()
    {
        final String commentText = "Projekt Q4: Prüfung für Überweisung & Großkunden (äöüß!@#$%^&*?)";
        final MBox box = new MBox("test-comment-umlauts", "xcmailr.test", 0L, false, primaryUser);
        box.setComment(commentText);

        final MBox saved = mailboxRepository.saveAndFlush(box);
        assertNotNull(saved.getId(), "Mailbox ID should be generated upon save");

        final MBox retrieved = mailboxRepository.findById(saved.getId()).orElse(null);
        assertNotNull(retrieved, "Mailbox should be retrievable by ID");
        assertEquals(commentText, retrieved.getComment(), "Comment with umlauts and symbols must be preserved exactly");
    }

    /**
     * Tier 2 Repository Slice Test: Verifies that blank strings are normalized to null on setComment.
     */
    @Test
    @DisplayName("Blank and empty comment strings are normalized to null")
    public void testNormalizeBlankCommentToNull()
    {
        final MBox box = new MBox("test-comment-blank", "xcmailr.test", 0L, false, primaryUser);
        box.setComment("   ");
        assertNull(box.getComment(), "Whitespace-only comment must be normalized to null");

        box.setComment("");
        assertNull(box.getComment(), "Empty comment string must be normalized to null");

        box.setComment("Valid note");
        final MBox saved = mailboxRepository.saveAndFlush(box);
        assertEquals("Valid note", saved.getComment());

        // Update to blank and verify cleared
        saved.setComment("   ");
        final MBox updated = mailboxRepository.saveAndFlush(saved);
        assertNull(updated.getComment(), "Comment should be cleared to null in database");
    }

    /**
     * Tier 2 Repository Slice Test: Verifies case-insensitive search by comment substring across
     * all search query methods in MailboxRepository.
     */
    @Test
    @DisplayName("Search queries match mailboxes by comment substring case-insensitively")
    public void testSearchByCommentSubstringCaseInsensitive()
    {
        final long futureEpoch = System.currentTimeMillis() + 86_400_000L;

        // Box 1: Expiring mailbox with comment
        final MBox box1 = new MBox("alpha", "xcmailr.test", futureEpoch, false, primaryUser);
        box1.setComment("Kundenportal Abrechnung Staging");
        mailboxRepository.save(box1);

        // Box 2: Unlimited permanent mailbox with comment
        final MBox box2 = new MBox("beta", "xcmailr.test", 0L, false, primaryUser);
        box2.setComment("Produktions-Monitoring E2E");
        mailboxRepository.save(box2);

        // Box 3: Expired mailbox with comment
        final MBox box3 = new MBox("gamma", "xcmailr.test", 0L, true, primaryUser);
        box3.setComment("Archiviertes Projekt Staging");
        mailboxRepository.save(box3);

        // Box 4: Foreign user mailbox with same comment (tenant isolation)
        final MBox foreignBox = new MBox("delta", "xcmailr.test", 0L, false, foreignUser);
        foreignBox.setComment("Kundenportal Abrechnung Staging");
        mailboxRepository.save(foreignBox);

        final Pageable pageable = PageRequest.of(0, 10);

        // 1. findByUsrAndSearch matches case-insensitively and respects tenant boundary
        final Page<MBox> searchLower = mailboxRepository.findByUsrAndSearch(primaryUser, "abrechnung", pageable);
        assertEquals(1, searchLower.getTotalElements(), "Should match primaryUser's box with Abrechnung");
        assertEquals("alpha", searchLower.getContent().get(0).getAddress());

        final Page<MBox> searchUpper = mailboxRepository.findByUsrAndSearch(primaryUser, "ABRECHNUNG", pageable);
        assertEquals(1, searchUpper.getTotalElements(), "Case-insensitive match should succeed with uppercase search query");

        final Page<MBox> searchShared = mailboxRepository.findByUsrAndSearch(primaryUser, "staging", pageable);
        assertEquals(2, searchShared.getTotalElements(), "Should match both active and expired box of primaryUser with 'staging'");

        // 2. findByUsrAndExpiredAndSearch
        final Page<MBox> searchActive = mailboxRepository.findByUsrAndExpiredAndSearch(primaryUser, false, "staging", pageable);
        assertEquals(1, searchActive.getTotalElements(), "Should only return active mailbox matching comment 'staging'");
        assertEquals("alpha", searchActive.getContent().get(0).getAddress());

        final Page<MBox> searchExpired = mailboxRepository.findByUsrAndExpiredAndSearch(primaryUser, true, "staging", pageable);
        assertEquals(1, searchExpired.getTotalElements(), "Should only return expired mailbox matching comment 'staging'");
        assertEquals("gamma", searchExpired.getContent().get(0).getAddress());

        // 3. findExpiringByUsrAndSearch
        final Page<MBox> searchExpiring = mailboxRepository.findExpiringByUsrAndSearch(primaryUser, "abrechnung", pageable);
        assertEquals(1, searchExpiring.getTotalElements(), "Should match expiring mailbox by comment");
        assertEquals("alpha", searchExpiring.getContent().get(0).getAddress());

        // 4. findUnlimitedByUsrAndSearch
        final Page<MBox> searchUnlimited = mailboxRepository.findUnlimitedByUsrAndSearch(primaryUser, "monitoring", pageable);
        assertEquals(1, searchUnlimited.getTotalElements(), "Should match permanent mailbox by comment substring");
        assertEquals("beta", searchUnlimited.getContent().get(0).getAddress());
    }

    /**
     * Tier 2 Repository Slice Test: Verifies that SQL injection payloads in the search string are treated
     * strictly as literal text without altering SQL syntax or exposing data across tenant boundaries.
     */
    @Test
    @DisplayName("Search queries handle SQL injection payloads safely as literal text")
    public void testSearchWithSqlInjectionPayloads_SafeLiteralExecution()
    {
        final MBox legitimateBox = new MBox("legit", "xcmailr.test", 0L, false, primaryUser);
        legitimateBox.setComment("Standard Note");
        mailboxRepository.save(legitimateBox);

        final MBox foreignBox = new MBox("secret", "xcmailr.test", 0L, false, foreignUser);
        foreignBox.setComment("Sensitive Corporate Data");
        mailboxRepository.save(foreignBox);

        final Pageable pageable = PageRequest.of(0, 10);

        final List<String> maliciousPayloads = List.of(
            "' OR '1'='1",
            "'; DROP TABLE mailboxes; --",
            "\" OR \"\"=\"",
            "' UNION SELECT * FROM users --",
            "admin'--",
            "%' OR b.usr IS NOT NULL --"
        );

        for (final String payload : maliciousPayloads)
        {
            final Page<MBox> results = mailboxRepository.findByUsrAndSearch(primaryUser, payload, pageable);
            assertEquals(0, results.getTotalElements(), "SQL injection payload '" + payload + "' must yield zero matches");

            final Page<MBox> expiringResults = mailboxRepository.findExpiringByUsrAndSearch(primaryUser, payload, pageable);
            assertEquals(0, expiringResults.getTotalElements(), "SQL injection payload in expiring search must yield zero matches");

            final Page<MBox> unlimitedResults = mailboxRepository.findUnlimitedByUsrAndSearch(primaryUser, payload, pageable);
            assertEquals(0, unlimitedResults.getTotalElements(), "SQL injection payload in unlimited search must yield zero matches");
        }

        // Verify that after all injection attempts, legitimate data is completely intact
        assertTrue(mailboxRepository.findById(legitimateBox.getId()).isPresent(), "Legitimate mailbox must remain intact");
        assertTrue(mailboxRepository.findById(foreignBox.getId()).isPresent(), "Foreign mailbox must remain intact");
    }
}
