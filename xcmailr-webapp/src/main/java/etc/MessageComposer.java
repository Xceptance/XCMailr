package etc;

import java.io.IOException;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.StringEscapeUtils;

public class MessageComposer
{

    private static void appendFieldValuesToStringBuffer(String[] input, StringBuffer buf)
    {
        if (input != null)
        {
            for (String field : input)
            {
                field = StringEscapeUtils.escapeHtml(field);
                buf.append(field);
            }
        }
    }

    private static String generateTextHeader(MimeMessage message) throws MessagingException
    {
        StringBuffer buf = new StringBuffer();
        appendTextRow("Subject", message.getHeader("Subject"), buf);
        appendTextRow("Date", message.getHeader("Date"), buf);
        appendTextRow("From", message.getHeader("From"), buf);
        appendTextRow("To", message.getHeader("To"), buf);
        return buf.toString();
    }

    private static String generateHtmlHeader(MimeMessage message) throws MessagingException
    {
        StringBuffer buf = new StringBuffer();
        buf.append("<table>");
        appendHtmlTableRow("Subject", message.getHeader("Subject"), buf);
        appendHtmlTableRow("Date", message.getHeader("Date"), buf);
        appendHtmlTableRow("From", message.getHeader("From"), buf);
        appendHtmlTableRow("To", message.getHeader("To"), buf);
        buf.append("</table>");
        return buf.toString();
    }

    private static void appendTextRow(String headerField, String[] input, StringBuffer buf)
    {
        buf.append("\n\t");
        buf.append(headerField).append("\t");
        appendFieldValuesToStringBuffer(input, buf);
    }

    private static void appendHtmlTableRow(String headerField, String[] input, StringBuffer buf)
    {
        buf.append("<tr><td><b>");
        buf.append(headerField);
        buf.append("</b></td><td>");
        appendFieldValuesToStringBuffer(input, buf);
        buf.append("</td></tr>");
    }

    public static MimeMessage createQuotedMessage(MimeMessage message) throws MessagingException, IOException
    {
        String msgContent = "";
        if (message.isMimeType("text/plain"))
        {
            msgContent = createQuotedPlainText(message.getContent().toString(), generateTextHeader(message));
            message.setContent(msgContent, "text/plain");
        }
        else if (message.isMimeType("text/html"))
        {
            msgContent = createQuotedHtmlText(message.getContent().toString(), generateHtmlHeader(message));
            message.setContent(msgContent, "text/html");
        }
        else
        {
            createQuotedMultipart(message, null);
        }
        return message;
    }

    private static String createQuotedPlainText(String msgContent, String headerText)
        throws MessagingException, IOException
    {
        // build "quoted" message
        StringBuffer buf = new StringBuffer();
        buf.append("\n------------------Message forwarded by the XCMAILR------------------\n\nHEADER:\n\n");
        // add the header lines
        buf.append(headerText);
        buf.append("\n\n MESSAGE:\n\n");
        buf.append(msgContent);
        return buf.toString().replace("\n", "\n>");
    }

    private static String createQuotedHtmlText(String msgContent, String headerText)
        throws MessagingException, IOException
    {
        // build "quoted" message
        StringBuffer buf = new StringBuffer();
        buf.append("<html><body><p>");
        buf.append("------------------Message forwarded by the XCMAILR---------------<br/><br/>HEADER:<br/><br/>");
        buf.append(headerText);
        buf.append("<br/><br/> MESSAGE:<br/><br/>");
        msgContent = HtmlUtils.readHTMLData(msgContent);
        buf.append(msgContent);
        buf.append("<br/>------------------End of forwarded Message------------------<br/></p></body></html>");
        return buf.toString();
    }

    private static MimeMessage createQuotedMultipart(MimeMessage message, Multipart multiPart)
        throws MessagingException, IOException
    {// build "quoted" message

        if (multiPart == null)
        {
            multiPart = (Multipart) message.getContent();
        }

        int j = multiPart.getCount();
        MimeBodyPart bodyPart;
        String msgContent = "";
        boolean isAttachment;
        String disposition;
        // create the different bodyparts
        for (int i = 0; i < j; i++)
        {
            bodyPart = (MimeBodyPart) multiPart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain"))
            {
                msgContent = createQuotedPlainText(bodyPart.getContent().toString(), generateTextHeader(message));
                bodyPart.setContent(msgContent, "text/plain");
            }

            disposition = bodyPart.getDisposition();
            isAttachment = (disposition != null && disposition.equals(BodyPart.ATTACHMENT));

            if (bodyPart.isMimeType("text/html") && !isAttachment)
            {
                msgContent = createQuotedHtmlText(bodyPart.getContent().toString(), generateHtmlHeader(message));
                bodyPart.setContent(msgContent, "text/html");

            }
            if (bodyPart.isMimeType("multipart/alternative") || bodyPart.isMimeType("multipart/related"))
            {
                Multipart newMulti = (Multipart) bodyPart.getContent();
                createQuotedMultipart(message, newMulti);
            }
        }
        return message;
    }

}
