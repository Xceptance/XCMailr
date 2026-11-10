package com.xceptance.xcmailr.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Utility and Spring bean component for table pagination calculations and page size clamping.
 * Computes a strictly bounded 7-slot sliding window sequence for pagination bars,
 * inserting ellipsis markers (-1) for omitted page number ranges when total pages exceed 7.
 *
 * @author Xceptance Software Technologies GmbH
 */
@Component("paginationHelper")
public final class PaginationHelper
{
    /**
     * Marker value representing an unclickable ellipsis in the pagination bar sequence.
     */
    public static final int ELLIPSIS = -1;

    /**
     * Maximum number of visible page slots rendered when total pages exceed 7.
     */
    public static final int MAX_VISIBLE_SLOTS = 7;

    /**
     * Default maximum page size permitted before clamping to prevent excessive memory allocation.
     */
    public static final int MAX_PAGE_SIZE = 1000;

    /**
     * Default minimum page size permitted before clamping.
     */
    public static final int MIN_PAGE_SIZE = 1;

    /**
     * Public default constructor for Spring component instantiation.
     */
    public PaginationHelper()
    {
    }

    /**
     * Computes the 7-slot page sequence for the specified current page and total pages.
     * Delegating instance method for Spring Expression Language (SpEL) in Thymeleaf templates:
     * {@code ${@paginationHelper.getPageSequence(page.number + 1, page.totalPages)}}.
     *
     * @param currentPage the 1-based current page number
     * @param totalPages the total number of available pages
     * @return an unmodifiable list of page numbers and ellipsis markers (-1)
     */
    public List<Integer> getPageSequence(final int currentPage, final int totalPages)
    {
        return calculatePageSequence(currentPage, totalPages);
    }

    /**
     * Static utility method computing the 7-slot page sequence for the specified current page and total pages.
     *
     * @param currentPage the 1-based current page number
     * @param totalPages the total number of available pages
     * @return an unmodifiable list of page numbers and ellipsis markers (-1)
     */
    public static List<Integer> calculatePageSequence(final int currentPage, final int totalPages)
    {
        if (totalPages <= 0)
        {
            return Collections.emptyList();
        }

        // When total pages is 7 or fewer, display all pages sequentially without ellipsis
        if (totalPages <= MAX_VISIBLE_SLOTS)
        {
            final List<Integer> pages = new ArrayList<>(totalPages);
            for (int i = 1; i <= totalPages; i++)
            {
                pages.add(i);
            }
            return Collections.unmodifiableList(pages);
        }

        // Clamp current page into [1, totalPages]
        final int normalizedCurrentPage = Math.min(Math.max(1, currentPage), totalPages);
        final List<Integer> pages = new ArrayList<>(MAX_VISIBLE_SLOTS);

        // Case 1: Near the start (P <= 4) -> [1, 2, 3, 4, 5, ..., N]
        if (normalizedCurrentPage <= 4)
        {
            for (int i = 1; i <= 5; i++)
            {
                pages.add(i);
            }
            pages.add(ELLIPSIS);
            pages.add(totalPages);
        }
        // Case 2: Near the end (P >= N - 3) -> [1, ..., N-4, N-3, N-2, N-1, N]
        else if (normalizedCurrentPage >= totalPages - 3)
        {
            pages.add(1);
            pages.add(ELLIPSIS);
            for (int i = totalPages - 4; i <= totalPages; i++)
            {
                pages.add(i);
            }
        }
        // Case 3: In the middle (4 < P < N - 3) -> [1, ..., P-1, P, P+1, ..., N]
        else
        {
            pages.add(1);
            pages.add(ELLIPSIS);
            pages.add(normalizedCurrentPage - 1);
            pages.add(normalizedCurrentPage);
            pages.add(normalizedCurrentPage + 1);
            pages.add(ELLIPSIS);
            pages.add(totalPages);
        }

        return Collections.unmodifiableList(pages);
    }

    /**
     * Clamps the requested page size defensively within safe bounds [1, 1000].
     * A size of 0 represents the "All" preset and is clamped to 1000.
     * Negative values fall back to the provided default size.
     *
     * @param requestedSize the inbound page size query parameter
     * @param defaultSize the fallback default size if the requested size is negative or invalid
     * @return the clamped effective page size
     */
    public static int clampPageSize(final int requestedSize, final int defaultSize)
    {
        if (requestedSize == 0)
        {
            return MAX_PAGE_SIZE;
        }
        if (requestedSize < 0)
        {
            return Math.min(Math.max(MIN_PAGE_SIZE, defaultSize), MAX_PAGE_SIZE);
        }
        return Math.min(Math.max(MIN_PAGE_SIZE, requestedSize), MAX_PAGE_SIZE);
    }
}
