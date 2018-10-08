package etc;

import java.util.LinkedList;
import java.util.List;

import javax.activation.DataSource;
import javax.mail.internet.MimeMessage;

import org.apache.commons.mail.util.MimeMessageParser;
import org.apache.commons.mail.util.MimeMessageUtils;

public class MailboxEntry
{
    public String mailAddress;

    public String sender;

    public String subject;

    public long receivedTime;

    public String rawContent;

    public String plainContent;

    public String htmlContent;

    public List<AttachmentEntry> attachments = new LinkedList<>();

    public MailboxEntry(String mailAddress, String sender, String subject, long receivedTime, String rawContent)
        throws Exception
    {
        this.mailAddress = mailAddress;
        this.sender = sender;
        this.subject = subject;
        this.receivedTime = receivedTime;
        this.rawContent = rawContent;

        MimeMessage mimeMessage = MimeMessageUtils.createMimeMessage(null, rawContent);
        MimeMessageParser mimeMessageParser = new MimeMessageParser(mimeMessage);
        mimeMessageParser.parse();

        this.plainContent = mimeMessageParser.getPlainContent();
        this.htmlContent = mimeMessageParser.getHtmlContent();

        for (DataSource attachment : mimeMessageParser.getAttachmentList())
        {
            attachments.add(new AttachmentEntry(attachment));
        }
    }
}
