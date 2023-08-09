/*
 * Copyright (c) 2013-2023 Xceptance Software Technologies GmbH
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
package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A generic Wrapper-Class for a List to provide Pagination
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 * @param <T>
 *            the Type of the List
 */

public class PageList<T>
{
    /**
     * the complete list with all entries
     */
    private List<T> entries;

    /**
     * number of entries per page
     */
    private int pageSize;

    /**
     * Default Constructor
     */
    public PageList()
    {
        this.entries = new ArrayList<T>();
        this.pageSize = 0;
    }

    /**
     * Constructor to initialize the Object
     * 
     * @param in
     *            the List with Entries
     * @param size
     *            the Size of Entries on one Page
     */
    public PageList(List<T> in, int size)
    {
        this.entries = Objects.requireNonNull(in);
        this.pageSize = size;
    }

    /**
     * @return copy of the complete list of entries
     */
    public List<T> getEntries()
    {
        return List.copyOf(entries);
    }

    /**
     * Sets the List to handle
     * 
     * @param entries
     *            the List with Entries
     */
    public void setEntriess(List<T> entries)
    {
        this.entries = Objects.requireNonNull(entries);
    }

    /**
     * @return the Number of Entries on a Page
     */
    public int getPagesize()
    {
        return this.pageSize;
    }

    /**
     * Sets the Number of Entries on a Page
     * 
     * @param size
     *            the Number of Entries
     */
    public void setPagesize(int size)
    {
        this.pageSize = Math.max(0, size);
    }

    /**
     * @return the Number of Elements in the List
     */
    public int getEntryCount()
    {
        return entries.size();
    }

    /**
     * @return the Number of Pages
     */
    public int getPageCount()
    {
        if (pageSize <= 0)
        { // return 1 if the number of entries on a page is below 1
            return 1;
        }

        int entriesTotal = entries.size();
        if (entriesTotal <= pageSize)
        { // return 1 if the number of entries in the list is below (or equal to) the number of entries on a page
            return 1;
        }
        else
        { // calculate the number of pages
            return (int) Math.ceil((entriesTotal / Double.valueOf(pageSize)));
        }
    }

    /**
     * @param page
     *            the Page-number (starting at 1)
     * @return a List which contains all Entries of the specified Page
     */
    public List<T> getPage(int page)
    {
        if (pageSize <= 0)
        { // the number of entries is less than 0, return all
            return entries;
        }
        if (page <= 0 || page > getPageCount())
        { // the page-number is less than one or greater than the number of existing pages -> return empty list
            return new ArrayList<T>();
        }

        int startIndex = (page -1 ) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, getEntryCount());

        return entries.subList(startIndex, endIndex);
    }
}
