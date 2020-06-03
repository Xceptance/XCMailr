package xcmailr.client.demo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import xcmailr.client.Mail;
import xcmailr.client.MailFilterOptions;
import xcmailr.client.Mailbox;
import xcmailr.client.XCMailrClient;

/**
 *
 */
public class Demo
{
    private XCMailrClient api;

    /**
     */
    public void init(final String xcmailrUrl, final String apiToken) throws Exception
    {
        // init API client
        api = new XCMailrClient(xcmailrUrl, apiToken);

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
        final InputStream is = api.mails().openAttachment(mail.id, attachmentName);
        final byte[] data = is.readAllBytes();

        // check attachment
        // ...

        // delete the mail
        api.mails().deleteMail(mail.id);
    }

    // -----------------------------------------------------------------------------

    public static void main(final String[] args) throws Exception
    {
        final XCMailrClient api = new XCMailrClient("http://localhost:8080/", "0oFsLgV7yG3gMgCPZV0Lp2YzEvXykm5Lsd73eTNHyyMyIUoP7Z");

        // api.mailboxes().createMailbox("foo@xcmailr.test", 10, true);
        // api.mailboxes().createMailbox("bar@xcmailr.test", 10, true);
        // api.mailboxes().updateMailbox("bar@xcmailr.test", "bar2@xcmailr.test", 10, true);
        // System.err.printf("### %s\n", api.mailboxes().listMailboxes());

        // retrieve mail
        MailFilterOptions options = null;
        options = new MailFilterOptions();
        options = options.subjectPattern("Test = äöü");
        options = options.senderPattern("wer");
        options = options.lastMatchOnly(true);

        final List<Mail> mails = api.mails().listMails("foo@xcmailr.test", options);
        System.err.printf("### %s\n", mails.size());

        final Mail mail = mails.get(0);
        System.err.printf("### %s\n", mail);
        System.err.printf("### %s\n", mail.textContent);
        System.err.printf("### %s\n", mail.htmlContent);
        System.err.printf("### %s\n", mail.attachments);
        System.err.printf("### %s\n", mail.headers);

        final InputStream input = api.mails().openAttachment(mail.id, "test - Copy.pdf");

        final BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(new File("attachment.out")));

        final byte[] buffer = new byte[4096];
        int n;
        while ((n = input.read(buffer)) != -1)
        {
            output.write(buffer, 0, n);
        }

        output.close();

        // api.mails().deleteMail(mail.id);
    }
}
