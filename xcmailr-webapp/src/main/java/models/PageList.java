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
    private List<T> allEntrys;

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
     *            the Size of one Page
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
     * @return the Number of Entries of a Page
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
        {
            return 1;
        }
        int entrys = allEntrys.size();
        if (entrys < pagesize)
        {
            return 1;
        }
        else
        {
            return (int) Math.ceil((entrys / new Double(pagesize)));
        }
    }

    /**
     * @param page the Page-number
     * @return a List which contains all Entries of the specified Page
     */
    public List<T> getPage(int page)
    {
        if (pagesize <= 0)
        {
            return allEntrys;
        }
        if (page > getPageCount())
        {
            return new ArrayList<T>();
        }
        int endIdx = (page * pagesize) - 1;

        int startIdx = endIdx - pagesize + 1;
        if (endIdx >= getEntryCount())
        {
            endIdx = getEntryCount();
        }
        else
        {
            endIdx += 1;
        }
        return allEntrys.subList(startIdx, endIdx);
    }
}