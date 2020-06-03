package xcmailr.client;

/**
 * The data object that represents the details of a mail attachment.
 * 
 * @see Mail
 * @see MailApi
 */
public class Attachment
{
    /**
     * The name of the attachment.
     */
    public String name;

    /**
     * The content type of the attachment.
     */
    public String contentType;

    /**
     * The size (in bytes) of the attachment.
     */
    public int size;

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("%s:{ name: '%s', contentType: '%s', size: '%d' }", super.toString(), name, contentType, size);
    }
}
