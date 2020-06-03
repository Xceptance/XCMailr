package xcmailr.client;

import java.util.Date;

/**
 * The data object that represents the details of a mailbox.
 * 
 * @see MailboxApi
 */
public class Mailbox
{
    /**
     * The mailbox address, for example "john.doe@xcmailr.test".
     */
    public String address;

    /**
     * The time (in milliseconds since epoch) when the mailbox will automatically be deactivated. After this time, mails
     * arriving at XCMailr will neither be stored nor forwarded, but simply ignored.
     */
    public long deactivationTime;

    /**
     * Whether or not XCMailr should forward mails arriving at this mailbox.
     */
    public boolean forwardEnabled;

    /**
     * Creates a new empty {@link Mailbox} instance.
     */
    public Mailbox()
    {
    }

    /**
     * Creates a new {@link Mailbox} instance and initializes it with the passed values.
     * 
     * @param address
     *            the address of the mailbox
     * @param deactivationTime
     *            the time (in milliseconds since epoch) when the mailbox will automatically be deactivated
     * @param forwardEnabled
     *            whether or not XCMailr should forward mails arriving at this mailbox
     */
    public Mailbox(final String address, final long deactivationTime, final boolean forwardEnabled)
    {
        this.address = address;
        this.deactivationTime = deactivationTime;
        this.forwardEnabled = forwardEnabled;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("%s:{ address: '%s', deactivationTime: '%s', forwardEnabled: '%b' }",
                             super.toString(),
                             address,
                             new Date(deactivationTime),
                             forwardEnabled);
    }
}
