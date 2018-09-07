package etc;

public class StatisticsEntry
{
    private int dropCount;

    private int forwardCount;

    public void incrementDropCount()
    {
        dropCount++;
    }

    public int getDropCount()
    {
        return dropCount;
    }

    public void setDropCount(int newDropCount)
    {
        this.dropCount = newDropCount;
    }

    public void incrementForwardCount()
    {
        forwardCount++;
    }

    public int getForwardCount()
    {
        return forwardCount;
    }

    public void setForwardCount(int newForwardCount)
    {
        this.forwardCount = newForwardCount;
    }
}
