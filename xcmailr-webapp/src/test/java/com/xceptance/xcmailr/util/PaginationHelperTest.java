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
package com.xceptance.xcmailr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests verifying 7-slot sliding window pagination sequence generation
 * and defensive page size clamping in {@link PaginationHelper}.
 * Follows the testing pyramid covering happy path, error boundaries, and special transition cases.
 *
 * @author Xceptance Software Technologies GmbH
 */
public class PaginationHelperTest
{
    private static final int ELLIPSIS = PaginationHelper.ELLIPSIS;

    @Test
    @DisplayName("Happy path: Returns all pages sequentially when total pages is seven or fewer")
    public void testSevenOrFewerPages()
    {
        final PaginationHelper helper = new PaginationHelper();

        // 1 page
        final List<Integer> singlePage = PaginationHelper.calculatePageSequence(1, 1);
        assertEquals(List.of(1), singlePage);
        assertEquals(singlePage, helper.getPageSequence(1, 1));

        // 3 pages
        final List<Integer> threePages = PaginationHelper.calculatePageSequence(2, 3);
        assertEquals(List.of(1, 2, 3), threePages);

        // Exactly 7 pages
        final List<Integer> sevenPages = PaginationHelper.calculatePageSequence(4, 7);
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7), sevenPages);
    }

    @Test
    @DisplayName("Happy path: Case 1 - Start sliding window when currentPage <= 4 and totalPages > 7")
    public void testStartSlidingWindow()
    {
        final int totalPages = 10;

        for (int p = 1; p <= 4; p++)
        {
            final List<Integer> sequence = PaginationHelper.calculatePageSequence(p, totalPages);
            assertEquals(7, sequence.size(), "Sequence must always have exactly 7 slots");
            assertEquals(List.of(1, 2, 3, 4, 5, ELLIPSIS, 10), sequence, "Failed for page " + p);
        }
    }

    @Test
    @DisplayName("Happy path: Case 2 - End sliding window when currentPage >= totalPages - 3 and totalPages > 7")
    public void testEndSlidingWindow()
    {
        final int totalPages = 10;

        for (int p = 7; p <= 10; p++)
        {
            final List<Integer> sequence = PaginationHelper.calculatePageSequence(p, totalPages);
            assertEquals(7, sequence.size(), "Sequence must always have exactly 7 slots");
            assertEquals(List.of(1, ELLIPSIS, 6, 7, 8, 9, 10), sequence, "Failed for page " + p);
        }
    }

    @Test
    @DisplayName("Happy path: Case 3 - Middle sliding window with double ellipsis when 4 < currentPage < totalPages - 3")
    public void testMiddleSlidingWindow()
    {
        final int totalPages = 12;

        // Current page 5 -> [1, ..., 4, 5, 6, ..., 12]
        final List<Integer> page5 = PaginationHelper.calculatePageSequence(5, totalPages);
        assertEquals(7, page5.size());
        assertEquals(List.of(1, ELLIPSIS, 4, 5, 6, ELLIPSIS, 12), page5);

        // Current page 7 -> [1, ..., 6, 7, 8, ..., 12]
        final List<Integer> page7 = PaginationHelper.calculatePageSequence(7, totalPages);
        assertEquals(7, page7.size());
        assertEquals(List.of(1, ELLIPSIS, 6, 7, 8, ELLIPSIS, 12), page7);

        // Current page 8 -> [1, ..., 7, 8, 9, ..., 12]
        final List<Integer> page8 = PaginationHelper.calculatePageSequence(8, totalPages);
        assertEquals(7, page8.size());
        assertEquals(List.of(1, ELLIPSIS, 7, 8, 9, ELLIPSIS, 12), page8);
    }

    @Test
    @DisplayName("Error boundary case: Zero or negative total pages returns empty list")
    public void testZeroOrNegativeTotalPages()
    {
        final List<Integer> zeroPages = PaginationHelper.calculatePageSequence(1, 0);
        assertTrue(zeroPages.isEmpty());

        final List<Integer> negativePages = PaginationHelper.calculatePageSequence(1, -10);
        assertTrue(negativePages.isEmpty());
    }

    @Test
    @DisplayName("Error boundary case: Out-of-bounds current page is clamped into [1, totalPages]")
    public void testOutOfBoundsCurrentPageClamping()
    {
        final int totalPages = 10;

        // Negative or zero current page clamped to page 1
        final List<Integer> zeroPage = PaginationHelper.calculatePageSequence(0, totalPages);
        assertEquals(List.of(1, 2, 3, 4, 5, ELLIPSIS, 10), zeroPage);

        final List<Integer> negativePage = PaginationHelper.calculatePageSequence(-5, totalPages);
        assertEquals(List.of(1, 2, 3, 4, 5, ELLIPSIS, 10), negativePage);

        // Excessive current page clamped to page totalPages
        final List<Integer> excessivePage = PaginationHelper.calculatePageSequence(999, totalPages);
        assertEquals(List.of(1, ELLIPSIS, 6, 7, 8, 9, 10), excessivePage);
    }

    @Test
    @DisplayName("Special transition case: Exactly 8 total pages flips cleanly between Start and End without middle")
    public void testEightPagesTransitionBoundary()
    {
        final int totalPages = 8;

        // Pages 1..4: Start window [1, 2, 3, 4, 5, ..., 8]
        for (int p = 1; p <= 4; p++)
        {
            final List<Integer> sequence = PaginationHelper.calculatePageSequence(p, totalPages);
            assertEquals(7, sequence.size());
            assertEquals(List.of(1, 2, 3, 4, 5, ELLIPSIS, 8), sequence);
        }

        // Pages 5..8: End window [1, ..., 4, 5, 6, 7, 8]
        for (int p = 5; p <= 8; p++)
        {
            final List<Integer> sequence = PaginationHelper.calculatePageSequence(p, totalPages);
            assertEquals(7, sequence.size());
            assertEquals(List.of(1, ELLIPSIS, 4, 5, 6, 7, 8), sequence);
        }
    }

    @Test
    @DisplayName("Special transition case: Exactly 9 total pages has exactly one middle position at page 5")
    public void testNinePagesTransitionBoundary()
    {
        final int totalPages = 9;

        // Page 4: Start window
        assertEquals(List.of(1, 2, 3, 4, 5, ELLIPSIS, 9), PaginationHelper.calculatePageSequence(4, totalPages));

        // Page 5: Middle window [1, ..., 4, 5, 6, ..., 9]
        assertEquals(List.of(1, ELLIPSIS, 4, 5, 6, ELLIPSIS, 9), PaginationHelper.calculatePageSequence(5, totalPages));

        // Page 6: End window
        assertEquals(List.of(1, ELLIPSIS, 5, 6, 7, 8, 9), PaginationHelper.calculatePageSequence(6, totalPages));
    }

    @Test
    @DisplayName("Special case: Very large total pages (N = 100)")
    public void testLargeDatasetPagination()
    {
        final int totalPages = 100;

        final List<Integer> page50 = PaginationHelper.calculatePageSequence(50, totalPages);
        assertEquals(7, page50.size());
        assertEquals(List.of(1, ELLIPSIS, 49, 50, 51, ELLIPSIS, 100), page50);
    }

    @Test
    @DisplayName("Defensive page size clamping: Handles 0 (All), negative fallback, valid bounds, and maximum limit")
    public void testClampPageSize()
    {
        final int defaultSize = 15;

        // 0 indicates "All" preset -> clamps to 1000
        assertEquals(1000, PaginationHelper.clampPageSize(0, defaultSize));

        // Negative size falls back to default size
        assertEquals(15, PaginationHelper.clampPageSize(-1, defaultSize));
        assertEquals(15, PaginationHelper.clampPageSize(-100, defaultSize));

        // Valid sizes
        assertEquals(5, PaginationHelper.clampPageSize(5, defaultSize));
        assertEquals(15, PaginationHelper.clampPageSize(15, defaultSize));
        assertEquals(25, PaginationHelper.clampPageSize(25, defaultSize));
        assertEquals(50, PaginationHelper.clampPageSize(50, defaultSize));
        assertEquals(500, PaginationHelper.clampPageSize(500, defaultSize));
        assertEquals(1000, PaginationHelper.clampPageSize(1000, defaultSize));

        // Upper limit exceeding 1000 clamped to 1000
        assertEquals(1000, PaginationHelper.clampPageSize(1001, defaultSize));
        assertEquals(1000, PaginationHelper.clampPageSize(99999, defaultSize));
    }
}
