package xcmailr.client;

/**
 * The data object that represents the details of a mail attachment.
 */
public class Attachment
{
    public String name;

    public String contentType;

    public int size;

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("%s:{ name: '%s', contentType: '%s', size: '%d' }", super.toString(), name, contentType, size);
    }
}
