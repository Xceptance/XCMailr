/**  
 *  Copyright 2013 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
package models;

import java.util.ArrayList;
import java.util.List;

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
    private List<T> allEntrys;

    /**
     * number of entries per page
     */
    private int pagesize;

    /**
     * Default Constructor
     */
    public PageList()
    {
        this.allEntrys = new ArrayList<T>();
        this.pagesize = 0;
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
        this.allEntrys = in;
        this.pagesize = size;
    }

    /**
     * @return the complete list which has been initialized
     */
    public List<T> getAllEntrys()
    {
        return allEntrys;
    }

    /**
     * Sets the List to handle
     * 
     * @param allEntrys
     *            the List with Entries
     */
    public void setAllEntrys(List<T> allEntrys)
    {
        this.allEntrys = allEntrys;
    }

    /**
     * @return the Number of Entries on a Page
     */

    public int getPagesize()
    {
        return this.pagesize;
    }

    /**
     * Sets the Number of Entries on a Page
     * 
     * @param size
     *            the Number of Entries
     */

    public void setPagesize(int size)
    {
        this.pagesize = size;
    }

    /**
     * @return the Number of Elements in the List
     */
    public int getEntryCount()
    {
        return allEntrys.size();
    }

    /**
     * @return the Number of Pages
     */
    public int getPageCount()
    {
        if (pagesize <= 0)
        { // return 1 if the number of entries on a page is below 1
            return 1;
        }

        int entrys = allEntrys.size();
        if (entrys < pagesize)
        { // return 1 if the number of entries in the list is below the number of entries on a page
            return 1;
        }
        else
        { // calculate the number of pages
            return (int) Math.ceil((entrys / new Double(pagesize)));
        }
    }

    /**
     * @param page
     *            the Page-number
     * @return a List which contains all Entries of the specified Page
     */
    public List<T> getPage(int page)
    {
        if (pagesize <= 0)
        { // the number of entries is less than 0, return all
            return allEntrys;
        }
        if (page > getPageCount())
        { // the page-number which is called is greater than the number of existing pages, return nothing
            return new ArrayList<T>();
        }
        // get the index-number of the last entry on the page
        int endIndex = (page * pagesize) - 1;
        // get the index-number of the first entry on the page
        int startIndex = endIndex - pagesize + 1;

        if (endIndex >= getEntryCount())
        { // the calculated last entry is greater than the number of existing entries
            endIndex = getEntryCount();
        }
        else
        { // the last entry exists, but the sublist takes the endIndex as EXCLUSIVE value
            endIndex += 1;
        }
        return allEntrys.subList(startIndex, endIndex);
    }
}
