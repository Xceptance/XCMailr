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
        for (String field : input)
        {
            field = StringEscapeUtils.escapeHtml(field);
            buf.append(field);
        }
    }

    private static void appendTextHeader(MimeMessage message, StringBuffer buf) throws MessagingException
    {
        appendTextRow("Subject", message.getHeader("Subject"), buf);
        appendTextRow("Date", message.getHeader("Date"), buf);
        appendTextRow("From", message.getHeader("From"), buf);
        appendTextRow("To", message.getHeader("To"), buf);
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
        appendHtmlTableRow("Subject", message.getHeader("Subject"), buf);
        appendHtmlTableRow("Date", message.getHeader("Date"), buf);
        appendHtmlTableRow("From", message.getHeader("From"), buf);
        appendHtmlTableRow("To", message.getHeader("To"), buf);
        return buf.toString();
    }

    private static void appendTextRow(String headerField, String[] input, StringBuffer buf)
    {
        buf.append("\n\t");
        buf.append(headerField).append("\n");
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

    private static void appendHtmlHeaderTable(MimeMessage message, StringBuffer buf) throws MessagingException
    {
        buf.append("<table>");
        appendHtmlTableRow("Subject: ", message.getHeader("Subject"), buf);
        appendHtmlTableRow("Date: ", message.getHeader("Date"), buf);
        appendHtmlTableRow("From: ", message.getHeader("From"), buf);
        appendHtmlTableRow("To: ", message.getHeader("To"), buf);
        buf.append("</table>");
    }

    public static MimeMessage createQuotedMessage(MimeMessage message) throws MessagingException, IOException
    {
        MimeMessage message2 = new MimeMessage(message);
        String msgContent = "";
        if (message.isMimeType("text/plain"))
        {
            msgContent = createQuotedPlainText2(message2.getContent().toString(), generateTextHeader(message2));
            message2.setContent(msgContent, "text/plain");
        }
        else if (message.isMimeType("text/html"))
        {
            msgContent = createQuotedHtmlText2(message2.getContent().toString(), generateHtmlHeader(message2));
            message.setContent(msgContent, "text/html");
        }
        else
        {
            createQuotedMultipart(message2, null);
        }
        return message2;
    }

    private static MimeMessage createQuotedPlainText(MimeMessage message) throws MessagingException, IOException
    {
        // build "quoted" message
        StringBuffer buf = new StringBuffer();
        buf.append("\n------------------Message forwarded by the XCMAILR------------------\n\nHEADER:\n\n");
        // add the header lines
        appendTextHeader(message, buf);
        buf.append("\n\n MESSAGE:\n\n");
        buf.append(message.getContent().toString());
        String quotationString = buf.toString().replace("\n", "\n>");
        message.setContent(quotationString, "text/plain");
        return message;
    }

    private static String createQuotedPlainText2(String msgContent, String headerText)
        throws MessagingException, IOException
    {
        // build "quoted" message
        StringBuffer buf = new StringBuffer();
        buf.append("\n------------------Message forwarded by the XCMAILR------------------\n\nHEADER:\n\n");
        // add the header lines
        buf.append(headerText);
        buf.append("\n\n MESSAGE:\n\n");
        buf.append(msgContent);
        //String quotationString = buf.toString().replace("\n", "\n>");
        return buf.toString().replace("\n", "\n>");
    }

    private static String createQuotedHtmlText2(String msgContent, String headerText)
        throws MessagingException, IOException
    {
        // build "quoted" message
        StringBuffer buf = new StringBuffer();
        buf.append("<html><body><p>");
        buf.append("------------------Message forwarded by the XCMAILR---------------<br/><br/>HEADER:<br/><br/>");
        buf.append(headerText);
        buf.append("<br/><br/> MESSAGE:<br/><br/>");
        buf.append(msgContent);
        buf.append("<br/>------------------End of forwarded Message------------------<br/></p></body></html>");
        return buf.toString();
    }

    private static MimeMessage createQuotedHtmlText(MimeMessage message) throws MessagingException, IOException
    {
        // build "quoted" message
        StringBuffer buf = new StringBuffer();
        buf.append("<html><body><p>");
        buf.append("------------------Message forwarded by the XCMAILR---------------<br/><br/>HEADER:<br/><br/>");

        // add the header lines
        appendHtmlHeaderTable(message, buf);

        buf.append("<br/><br/> MESSAGE:<br/><br/>");
        buf.append(message.getContent().toString());

        buf.append("<br/>------------------End of forwarded Message------------------<br/></p></body></html>");
        message.setContent(buf.toString(), "text/html");
        return message;
    }

    private static MimeMessage createQuotedMultipart(MimeMessage message, Multipart multiPart)
        throws MessagingException, IOException
    {
        // build "quoted" message
        if (multiPart == null)
        {
            multiPart = (Multipart) message.getContent();
        }

        int j = multiPart.getCount();
        MimeBodyPart bodyPart;

        // create the different bodyparts
        for (int i = 0; i < j; i++)
        {
            bodyPart = (MimeBodyPart) multiPart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain"))
            {
                createQuotedPlainTextBody(bodyPart, message);
            }

            String disposition = bodyPart.getDisposition();
            boolean isAttachment = (disposition != null && disposition.equals(BodyPart.ATTACHMENT));
            if (bodyPart.isMimeType("text/html") && !isAttachment)
            {
                createQuotedHtmlTextBody(bodyPart, message);
            }
            if (bodyPart.isMimeType("multipart/alternative") || bodyPart.isMimeType("multipart/related"))
            {
                Multipart newMulti = (Multipart) bodyPart.getContent();
                createQuotedMultipart(message, newMulti);
            }

        }
        return message;
    }

    private static MimeBodyPart createQuotedPlainTextBody(MimeBodyPart body, MimeMessage message)
        throws MessagingException, IOException
    {
        // build "quoted" message

        StringBuffer buf = new StringBuffer();
        buf.append("\n------------------Message forwarded by the XCMAILR------------------\n\nHEADER:\n\n");
        // add the header lines
        appendTextHeader(message, buf);

        buf.append("\n\n MESSAGE:\n\n");
        buf.append(body.getContent().toString());
        buf.append("\n------------------End of forwarded Message------------------");
        String quotationString = buf.toString().replace("\n", "\n>");
        body.setContent(quotationString, "text/plain");
        return body;
    }

    private static MimeBodyPart createQuotedHtmlTextBody(MimeBodyPart body, MimeMessage message)
        throws MessagingException, IOException
    {
        // build "quoted" message
        StringBuffer buf = new StringBuffer();
        buf.append("<html><body><p>");
        buf.append("------------------Message forwarded by the XCMAILR---------------<br/><br/>HEADER:<br/><br/>");
        // add the header lines
        appendHtmlHeaderTable(message, buf);

        buf.append("<br/><br/> <b>MESSAGE:</b><br/><br/>");
        String content = body.getContent().toString();
        content = HtmlUtils.readHTMLData(content);
        buf.append(content);
        buf.append("<br/>------------------End of forwarded Message------------------<br/>");
        buf.append("</p></body></html>");
        body.setContent(buf.toString(), "text/html");
        return body;
    }

}
