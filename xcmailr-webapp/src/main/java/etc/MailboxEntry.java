package etc;

public class MailboxEntry
{
    public String mailAddress;

    public String sender;

    public String subject;

    public long receivedTime;

    public MailboxEntry(String mailAddress, String sender, String subject, long receivedTime)
    {
        this.mailAddress = mailAddress;
        this.sender = sender;
        this.subject = subject;
        this.receivedTime = receivedTime;
    }
}
