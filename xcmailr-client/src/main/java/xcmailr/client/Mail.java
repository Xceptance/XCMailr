package xcmailr.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The data object that represents the details of a mail.
 * 
 * @see MailApi
 */
public class Mail
{
    /**
     * The mail ID.
     */
    public long id;

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
     * The time (in milliseconds since epoch) at which the mail was received.
     */
    public long receivedTime;

    /**
     * The plain-text content.
     */
    public String textContent;

    /**
     * The HTML content.
     */
    public String htmlContent;

    /**
     * The mail attachments.
     */
    public List<Attachment> attachments = new ArrayList<>();

    /**
     * The mail headers as a single string, separated by newlines.
     */
    public String headers;

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("%s:{ id: '%s', sender: '%s', recipient: '%s', subject: '%s', receivedTime: '%s' }",
                             super.toString(),
                             id,
                             sender,
                             recipient,
                             subject,
                             new Date(receivedTime));
    }
}
