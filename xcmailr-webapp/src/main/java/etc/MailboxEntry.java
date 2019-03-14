package etc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.LinkedList;
import java.util.List;

import javax.activation.DataSource;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.apache.commons.mail.util.MimeMessageUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

import models.Mail;

public class MailboxEntry
{
    public final String mailAddress;

    public final String sender;

    public final String subject;

    public final long receivedTime;

    public final String textContent;

    public final String htmlContent;

    public final List<AttachmentEntry> attachments = new LinkedList<>();

    @JsonIgnore
    public final String rawContent;

    public final String downloadToken;

    public MailboxEntry(String mailAddress, Mail mail) throws Exception
    {
        this.mailAddress = mailAddress;
        this.sender = mail.getSender();
        this.rawContent = mail.getMessage();
        this.subject = StringUtils.defaultString(mail.getSubject());
        this.receivedTime = mail.getReceiveTime();
        this.downloadToken = mail.getUuid();

        MimeMessage mimeMessage = MimeMessageUtils.createMimeMessage(null, rawContent);
        MimeMessageParser mimeMessageParser = new MimeMessageParser(mimeMessage);
        mimeMessageParser.parse();

        final String plainText = StringUtils.defaultString(mimeMessageParser.getPlainContent());
        final String htmlText = StringUtils.defaultString(mimeMessageParser.getHtmlContent());

        Encoder base64encoder = Base64.getEncoder();

        this.textContent = base64encoder.encodeToString(plainText.getBytes(StandardCharsets.UTF_8));
        this.htmlContent = base64encoder.encodeToString(htmlText.getBytes(StandardCharsets.UTF_8));

        for (DataSource attachment : mimeMessageParser.getAttachmentList())
        {
            attachments.add(new AttachmentEntry(attachment));
        }
    }
}
