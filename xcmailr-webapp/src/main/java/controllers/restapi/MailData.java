package controllers.restapi;

import java.util.LinkedList;
import java.util.List;

import javax.activation.DataSource;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.apache.commons.mail.util.MimeMessageUtils;

import etc.HelperUtils;
import models.Mail;

/**
 * The data object that represents the details of a mail.
 */
public class MailData
{
    public final long id;

    public final String sender;

    public final String recipient;

    public final String subject;

    public final Content mailContent;

    public final List<AttachmentData> attachments = new LinkedList<>();

    public final long receiveTime;

    public final String mailHeader;

    public MailData(Mail mail) throws Exception
    {
        this.id = mail.getId();
        this.recipient = mail.getMailbox().getFullAddress();
        this.sender = mail.getSender();
        this.subject = StringUtils.defaultString(mail.getSubject());
        this.receiveTime = mail.getReceiveTime();

        byte[] rawContent = mail.getMessage();

        if (rawContent != null && rawContent.length > 0)
        {
            MimeMessage mimeMessage = MimeMessageUtils.createMimeMessage(null, rawContent);
            MimeMessageParser mimeMessageParser = new MimeMessageParser(mimeMessage);
            mimeMessageParser.parse();

            this.mailHeader = HelperUtils.getHeaderText(mimeMessage);

            final String textContentDecoded = StringUtils.defaultString(mimeMessageParser.getPlainContent());
            final String htmlContentDecoded = StringUtils.defaultString(mimeMessageParser.getHtmlContent());

            this.mailContent = new Content(textContentDecoded, htmlContentDecoded);

            for (DataSource attachment : mimeMessageParser.getAttachmentList())
            {
                attachments.add(new AttachmentData(attachment));
            }
        }
        else
        {
            this.mailHeader = "";
            mailContent = null;
        }
    }

    public static class Content
    {
        public final String text;

        public final String html;

        private Content(final String aText, final String aHtml)
        {
            text = aText;
            html = aHtml;
        }
    }
}
