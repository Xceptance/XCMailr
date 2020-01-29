package xcmailr.client;

import java.io.InputStream;
import java.util.List;

/**
 *
 */
public class Demo
{
    private XCMailrApi api;

    /**
     */
    public void init(final String xcmailrUrl, final String apiToken) throws Exception
    {
        // init API client
        api = new XCMailrApi(xcmailrUrl, apiToken);

        // do something with the API client
        // ...
    }

    /**
     */
    public void createAndDeleteMailbox(final String mailboxAddress) throws Exception
    {
        // create a mailbox
        final Mailbox mailbox = api.mailboxes().createMailbox(mailboxAddress, 10, false);

        // do something with the mailbox
        // ...

        // delete the mailbox
        api.mailboxes().deleteMailbox(mailboxAddress);
    }

    /**
     */
    public void retrieveAndProcessMail(final String mailboxAddress, final String subjectPattern, final String attachmentName)
        throws Exception
    {
        // retrieve mail
        final List<Mail> mails = api.mails()
                                    .listMails(mailboxAddress, new MailFilterOptions().lastMatchOnly(true).subjectPattern(subjectPattern));

        final Mail mail = mails.get(0);

        // check mail content
        // ...

        // download attachment
        final InputStream is = api.mails().downloadAttachment(mail.id, attachmentName);
        final byte[] data = is.readAllBytes();

        // check attachment
        // ...

        // delete the mail
        api.mails().deleteMail(mail.id);
    }

    // -----------------------------------------------------------------------------

    public static void main(final String[] args) throws Exception
    {
        final XCMailrApi api = new XCMailrApi("http://localhost:8080/", "hoS7QZUackT1fUUB7VK4a3Bj8175M7vXbVqH827DHS7aCUs9Vg");

        // api.mailboxes().createMailbox("foo@example.org", now().plus(10, MINUTES).toEpochMilli(), false);
        System.err.printf("### %s\n", api.mailboxes().listMailboxes());

        // retrieve mail
        final List<Mail> mails = api.mails().listMails("aaa@xcmailr.test", null);
        // new MailFilterOptions().lastMatchOnly(true).subjectPattern(subjectPattern));

        System.err.printf("### %s\n", mails);

        // final Mail mail = mails.get(0);

        // System.err.printf("### %s\n", mail);
    }
}
