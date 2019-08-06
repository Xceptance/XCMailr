package etc;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

import javax.activation.DataSource;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.apache.commons.mail.util.MimeMessageUtils;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import models.Mail;

public class MailboxEntry
{
    public final String mailAddress;

    public final String sender;

    public final String subject;

    public final long receivedTime;

    @JsonIgnore
    public final Content mailContent;

    public final List<AttachmentEntry> attachments = new LinkedList<>();

    @JsonIgnore
    private final byte[] rawContent;

    @JsonIgnore
    public final String mailHeader;

    public final String downloadToken;

    public MailboxEntry(String mailAddress, Mail mail) throws Exception
    {
        this.mailAddress = mailAddress;
        this.sender = mail.getSender();
        this.rawContent = mail.getMessage();

        this.subject = StringUtils.defaultString(mail.getSubject());
        this.receivedTime = mail.getReceiveTime();
        this.downloadToken = mail.getUuid();

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
                attachments.add(new AttachmentEntry(attachment));
            }
        }
        else
        {
            this.mailHeader = "";
            mailContent = null;
        }

    }

    public boolean matchesSearchPhrase(String phrase)
    {
        phrase = phrase.toLowerCase();
        return mailAddress.toLowerCase().contains(phrase) || sender.toLowerCase().contains(phrase)
               || subject.toLowerCase().contains(phrase) || contentContainsIgnoreCase(phrase);
    }

    private boolean contentContainsIgnoreCase(final String phrase)
    {
        if (mailContent != null)
        {
            return mailContent.text.toLowerCase().contains(phrase) || mailContent.html.toLowerCase().contains(phrase);
        }
        return false;
    }

    public String getRawContent()
    {
        return getRawContent(StandardCharsets.UTF_8);
    }

    public String getRawContent(final Charset cs)
    {
        return new String(rawContent, cs);
    }

    @JsonGetter
    public String getHtmlContent()
    {
        if (mailContent == null)
        {
            return "";
        }
        return Base64.getEncoder().encodeToString(mailContent.html.getBytes(StandardCharsets.UTF_8));
    }

    @JsonGetter
    public String getTextContent()
    {
        if (mailContent == null)
        {
            return "";
        }
        return Base64.getEncoder().encodeToString(mailContent.text.getBytes(StandardCharsets.UTF_8));
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
