package models;

import java.util.ArrayList;
import java.util.List;

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
