package etc;

public class MailboxEntry
{
    public String sender;

    public String subject;

    public long receivedTime;

    public MailboxEntry(String sender, String subject, long receivedTime)
    {
        this.sender = sender;
        this.subject = subject;
        this.receivedTime = receivedTime;
    }
}
