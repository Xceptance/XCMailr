package etc;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.LinkedList;
import java.util.List;

import javax.activation.DataSource;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.apache.commons.io.IOUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.apache.commons.mail.util.MimeMessageUtils;

import models.Mail;

public class MailboxEntry
{
    public int id;

    public String mailAddress;

    public String sender;

    public String subject;

    public long receivedTime;

    public String textContent;

    public String htmlContent;

    public List<AttachmentEntry> attachments = new LinkedList<>();

    public String rawContent;

    public String downloadToken;

    public MailboxEntry(String mailAddress, Mail mail) throws Exception
    {
        this.mailAddress = mailAddress;
        this.sender = mail.getSender();
        this.rawContent = mail.getMessage();
        this.subject = mail.getSubject() == null ? "" : mail.getSubject();
        this.receivedTime = mail.getReceiveTime();
        this.downloadToken = mail.getUuid();

        MimeMessage mimeMessage = MimeMessageUtils.createMimeMessage(null, rawContent);
        MimeMessageParser mimeMessageParser = new MimeMessageParser(mimeMessage);
        mimeMessageParser.parse();

        this.textContent = mimeMessageParser.getPlainContent() == null ? "" : mimeMessageParser.getPlainContent();
        this.htmlContent = mimeMessageParser.getHtmlContent() == null ? "" : mimeMessageParser.getHtmlContent();

        Encoder base64encoder = Base64.getEncoder();

        BufferedInputStream bufferedInputStream = new BufferedInputStream(MimeUtility.decode(new ByteArrayInputStream(this.htmlContent.getBytes(StandardCharsets.UTF_8)),
                                                                                             "quoted-printable"));
        String unqoutedPrintableHtmlText = IOUtils.toString(bufferedInputStream, StandardCharsets.UTF_8);
        bufferedInputStream.close();

        this.textContent = base64encoder.encodeToString(this.textContent.getBytes());
        this.htmlContent = base64encoder.encodeToString(MimeUtility.decodeText(unqoutedPrintableHtmlText).getBytes());

        for (DataSource attachment : mimeMessageParser.getAttachmentList())
        {
            attachments.add(new AttachmentEntry(attachment));
        }
    }
}
