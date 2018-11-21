package etc;

import java.text.SimpleDateFormat;
import java.util.Date;
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

    public String textContent;

    public String htmlContent;

    public List<AttachmentEntry> attachments = new LinkedList<>();

    public String rawContent;

    public MailboxEntry(String mailAddress, String sender, String subject, long receivedTime, String rawContent)
        throws Exception
    {
        this.mailAddress = mailAddress;
        this.sender = sender;
        this.rawContent = rawContent;
        this.subject = subject == null ? "" : subject;
        this.receivedTime = receivedTime;

        MimeMessage mimeMessage = MimeMessageUtils.createMimeMessage(null, rawContent);
        MimeMessageParser mimeMessageParser = new MimeMessageParser(mimeMessage);
        mimeMessageParser.parse();

        this.textContent = mimeMessageParser.getPlainContent() == null ? "" : mimeMessageParser.getPlainContent();
        this.htmlContent = mimeMessageParser.getHtmlContent() == null ? "" : mimeMessageParser.getHtmlContent();

        for (DataSource attachment : mimeMessageParser.getAttachmentList())
        {
            attachments.add(new AttachmentEntry(attachment));
        }
    }

    /**
     * @return the Timestamp as String in the Format "dd.MM.yyyy hh:mm"
     */
    public String getReceivedTimeAsString()
    {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(this.receivedTime)).toString();
    }

}
