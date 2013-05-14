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
 * 
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 * 
 * @param <T> the Type of the List
 */

public class PageList<T>
{
    private List<T> allEntrys;

    private int pagesize;

    public PageList()
    {
        this.allEntrys = new ArrayList<T>();
        this.pagesize = 0;
    }

    public PageList(List<T> in, int size)
    {
        this.allEntrys = in;
        this.pagesize = size;
    }

    public List<T> getAllEntrys()
    {
        return allEntrys;
    }

    public void setAllEntrys(List<T> allEntrys)
    {
        this.allEntrys = allEntrys;
    }

    public int getPagesize()
    {
        return pagesize;
    }

    public void setPagesize(int size)
    {
        this.pagesize = size;
    }

    public int getEntryCount()
    {
        return allEntrys.size();
    }

    public int getPageCount()
    {
        int entrys = allEntrys.size() - 1;
        if (entrys < pagesize)
        {
            return 1;
        }
        else
        {
            return (entrys) / pagesize;
        }
    }

    public List<T> getPage(int page)
    {
        if (page > pagesize)
        {
            return new ArrayList<T>();
        }
        int endIdx = (page * pagesize) - 1;

        int startIdx = endIdx - pagesize + 1;
        if (endIdx > getEntryCount())
        {
            endIdx = getEntryCount() - 1;
        }
        return allEntrys.subList(startIdx, endIdx);
    }

    public List<T> getPageByString(String page)
    {
        int i = Integer.parseInt(page);
        return getPage(i);
    }

}
