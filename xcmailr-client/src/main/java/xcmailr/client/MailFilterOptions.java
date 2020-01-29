package xcmailr.client;

/**
 * The filter configuration that can be passed into {@link MailApi#listMails(String, MailFilterOptions)}.
 */
public class MailFilterOptions
{
    public String fromPattern;

    public String subjectPattern;

    public String mailHeaderPattern;

    public String htmlContentPattern;

    public String textContentPattern;

    public boolean lastMatchOnly;

    public MailFilterOptions fromPattern(final String senderPattern)
    {
        this.fromPattern = senderPattern;
        return this;
    }

    public MailFilterOptions subjectPattern(final String subjectPattern)
    {
        this.subjectPattern = subjectPattern;
        return this;
    }

    public MailFilterOptions mailHeaderPattern(final String mailHeaderPattern)
    {
        this.mailHeaderPattern = mailHeaderPattern;
        return this;
    }

    public MailFilterOptions htmlContentPattern(final String htmlContentPattern)
    {
        this.htmlContentPattern = htmlContentPattern;
        return this;
    }

    public MailFilterOptions textContentPattern(final String textContentPattern)
    {
        this.textContentPattern = textContentPattern;
        return this;
    }

    public MailFilterOptions lastMatchOnly(final boolean lastMatchOnly)
    {
        this.lastMatchOnly = lastMatchOnly;
        return this;
    }
}
