package xcmailr.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The data object that represents the details of a mail.
 */
public class Mail
{
    /**
     * The mail ID.
     */
    public String id;

    /**
     * The sender's address.
     */
    public String sender;

    /**
     * The recipient's address.
     */
    public String recipient;

    /**
     * The mail subject.
     */
    public String subject;

    /**
     * The time (in milliseconds since epoch) when the mail was received.
     */
    public long receiveTime;

    /**
     * 
     */
    public Content mailContent;

    /**
     * The mail attachments.
     */
    public List<Attachment> attachments = new ArrayList<>();

    /**
     * The mail headers as a single string, separated by newlines.
     */
    public String mailHeader;

    // TODO
    public static class Content
    {
        public String text;

        public String html;

        private Content(final String aText, final String aHtml)
        {
            text = aText;
            html = aHtml;
        }
    }

    public String toString()
    {
        return String.format("%s:{ id: '%s', sender: '%s', recipient: '%s', subject: '%s', receiveTime: '%s' }",
                             super.toString(),
                             id,
                             sender,
                             recipient,
                             subject,
                             new Date(receiveTime));
    }
}
