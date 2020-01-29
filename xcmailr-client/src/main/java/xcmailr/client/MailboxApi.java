package xcmailr.client;

import java.util.List;

/**
 * Operations to manage mailboxes in a remote XCMailr instance.
 */
public interface MailboxApi
{
    /**
     * Lists the details of all mailboxes that belong to the current user.
     * 
     * @return the list of mailboxes
     * @throws Exception
     *             if anything went wrong
     */
    public List<Mailbox> listMailboxes() throws Exception;

    /**
     * Creates a new mailbox at XCMailr.
     * 
     * @param address
     *            the address of the mailbox
     * @param minutesActive
     *            the time (in minutes) from now when the mailbox will automatically be deactivated
     * @param forwardEnabled
     * @return
     * @throws Exception
     *             if anything went wrong
     */
    public Mailbox createMailbox(final String address, final int minutesActive, final boolean forwardEnabled) throws Exception;

    /**
     * Creates a new mailbox at XCMailr.
     * 
     * @param mailbox
     *            the details of the new mailbox
     * @return the mailbox as returned from the server
     * @throws Exception
     *             if anything went wrong
     */
    public Mailbox createMailbox(final Mailbox mailbox) throws Exception;

    /**
     * @param address
     *            the address of the mailbox
     * @return
     * @throws Exception
     *             if anything went wrong
     */
    public Mailbox getMailbox(final String address) throws Exception;

    /**
     * @param address
     *            the address of the mailbox
     * @param newAddress
     * @param address
     *            the address of the mailbox
     * @param minutesActive
     * @param forwardEnabled
     * @return
     * @throws Exception
     *             if anything went wrong
     */
    public Mailbox updateMailbox(final String address, final String newAddress, final int minutesActive, final boolean forwardEnabled)
        throws Exception;

    /**
     * @param address
     *            the address of the mailbox
     * @param mailbox
     * @return
     * @throws Exception
     *             if anything went wrong
     */
    public Mailbox updateMailbox(final String address, final Mailbox mailbox) throws Exception;

    /**
     * @param address
     *            the address of the mailbox
     * @throws Exception
     *             if anything went wrong
     */
    public void deleteMailbox(final String address) throws Exception;
}
