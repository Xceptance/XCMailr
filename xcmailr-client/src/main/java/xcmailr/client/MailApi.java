package xcmailr.client;

import java.io.InputStream;
import java.util.List;

/**
 * Operations to manage mails in a remote XCMailr instance.
 */
public interface MailApi
{
    /**
     * Lists the details of all mails in the mailbox that is given by its address.
     * 
     * @param mailboxAddress
     *            the mailbox address, such as "john.doe@xcmailr.test"
     * @param options
     *            the filter options (may be <code>null</code>)
     * @return the list of mails that match the filter options
     * @throws Exception
     *             if anything went wrong
     */
    public List<Mail> listMails(String mailboxAddress, MailFilterOptions options) throws Exception;

    /**
     * Returns the details of the mail with the given ID.
     * 
     * @param mailId
     *            the mailbox address, such as "john.doe@xcmailr.test"
     * @return the details of the mail
     * @throws Exception
     *             if anything went wrong
     */
    public Mail getMail(String mailId) throws Exception;

    /**
     * Opens a stream to download the named attachment of a certain mail.
     * 
     * @param mailId
     *            the ID of the mail
     * @param attachmentName
     *            the (file) name of the attachment
     * @return a stream from which the attachment can be read
     * @throws Exception
     *             if anything went wrong
     */
    public InputStream downloadAttachment(String mailId, String attachmentName) throws Exception;

    /**
     * Deletes a mail from XCMailr.
     * 
     * @param mailId
     *            the ID of the mail
     * @throws Exception
     *             if anything went wrong
     */
    public void deleteMail(String mailId) throws Exception;
}
