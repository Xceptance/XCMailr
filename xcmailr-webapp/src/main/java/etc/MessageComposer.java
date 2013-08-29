package etc;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

public class MessageComposer
{

    public static MimeMessage createQuotedMessage(MimeMessage message) throws MessagingException, IOException
    {
        MimeMessage message2 = new MimeMessage(message);

        if (message.isMimeType("text/plain"))
        {
            message2 = createQuotedPlainText(message2);
        }
        else if (message.isMimeType("text/html"))
        {
            message2 = createQuotedHtmlText(message2);
        }
        else
        {
            message2 = createQuotedMultipart(message2);
        }
        return message2;
    }

    @SuppressWarnings("unchecked")
    public static MimeMessage createQuotedPlainText(MimeMessage message) throws MessagingException, IOException
    {
        // build "quoted" message
        StringBuffer buf = new StringBuffer();
        buf.append("\n------------------Message forwarded by the XCMAILR------------------\n\nHEADER:\n\n");
        // add the header lines
        Enumeration<String> enumer = message.getAllHeaderLines();
        for (String en : Collections.list(enumer))
        {
            buf.append("\n\t" + en);
        }
        buf.append("\n\n MESSAGE:\n\n");
        buf.append(message.getContent().toString());
        buf.append("\n------------------End of forwarded Message------------------");
        String quotationString = buf.toString().replace("\n", "\n>");
        message.setContent(quotationString, "text/plain");
        message.setHeader("Content-Transfer-Encoding", "8bit");
        return message;
    }

    @SuppressWarnings("unchecked")
    public static MimeMessage createQuotedHtmlText(MimeMessage message) throws MessagingException, IOException
    {
        // build "quoted" message
        StringBuffer buf = new StringBuffer();
        buf.append("<html><body><p>");
        buf.append("------------------Message forwarded by the XCMAILR---------------<br/><br/>HEADER:<br/><br/>");
        // add the header lines
        Enumeration<Header> enumer = message.getAllHeaders();
        
        buf.append("<table>");
        for (Header en : Collections.list(enumer))
        {
            buf.append("<tr><td>");
            buf.append(en.getName());
            buf.append("</td><td>");
            buf.append(en.getName());
            buf.append("</td></tr>");
        }
        buf.append("</table>");
        buf.append("<br/><br/> MESSAGE:<br/><br/>");
        buf.append(message.getContent().toString());

        buf.append("<br/>------------------End of forwarded Message------------------<br/></p></body></html>");
        // quotationString = buf.toString().replace("\n", "<br/>");

        message.setContent(buf.toString(), "text/html");
        message.setHeader("Content-Transfer-Encoding", "8bit");
        return message;
    }

    @SuppressWarnings("unchecked")
    public static MimeMessage createQuotedMultipart(MimeMessage message) throws MessagingException, IOException
    {
        // build "quoted" message
        Multipart multiPart = (Multipart) message.getContent();

        int j = multiPart.getCount();
        MimeBodyPart bodyPart;

        // create the different bodyparts
        for (int i = 0; i < j; i++)
        {
            bodyPart = (MimeBodyPart) multiPart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain"))
            {
                createQuotedPlainTextBody(bodyPart, message.getAllHeaderLines());
            }

            String disposition = bodyPart.getDisposition();
            boolean isAttachment = (disposition != null && disposition.equals(BodyPart.ATTACHMENT));
            if (bodyPart.isMimeType("text/html") && !isAttachment)
            {
                createQuotedHtmlTextBody(bodyPart, message.getAllHeaders());
            }
            if (bodyPart.isMimeType("multipart/related"))
            {
                Multipart newMulti = (Multipart) bodyPart.getContent();
                createQuotedMultipart(newMulti, message);
            }

        }
        return message;
    }

    @SuppressWarnings("unchecked")
    public static MimeMessage createQuotedMultipart(Multipart multiPart, MimeMessage message)
        throws MessagingException, IOException
    {
        // build "quoted" message

        int j = multiPart.getCount();
        MimeBodyPart bodyPart;

        // create the different bodyparts
        for (int i = 0; i < j; i++)
        {
            bodyPart = (MimeBodyPart) multiPart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain"))
            {
                createQuotedPlainTextBody(bodyPart, message.getAllHeaderLines());
            }

            String disposition = bodyPart.getDisposition();
            boolean isAttachment = (disposition != null && disposition.equals(BodyPart.ATTACHMENT));
            if (bodyPart.isMimeType("text/html") && !isAttachment)
            {
                createQuotedHtmlTextBody(bodyPart, message.getAllHeaders());
            }
            if (bodyPart.isMimeType("multipart/alternative"))
            {
                Multipart newMulti = (Multipart) bodyPart.getContent();
                createQuotedMultipart(newMulti, message);
            }

        }
        return message;
    }

    public static MimeBodyPart createQuotedPlainTextBody(MimeBodyPart body, Enumeration<String> headers)
        throws MessagingException, IOException
    {
        // build "quoted" message

        StringBuffer buf = new StringBuffer();
        buf.append("\n------------------Message forwarded by the XCMAILR------------------\n\nHEADER:\n\n");
        // add the header lines

        for (String en : Collections.list(headers))
        {
            buf.append("\n\t" + en);
        }
        buf.append("\n\n MESSAGE:\n\n");
        buf.append(body.getContent().toString());
        buf.append("\n------------------End of forwarded Message------------------");
        String quotationString = buf.toString().replace("\n", "\n>");
        body.setContent(quotationString, "text/plain");
        body.setHeader("Content-Transfer-Encoding", "8bit");
        return body;
    }

    public static MimeBodyPart createQuotedHtmlTextBody(MimeBodyPart body, Enumeration<Header> headers)
        throws MessagingException, IOException
    {
        // build "quoted" message
        StringBuffer buf = new StringBuffer();
        buf.append("<html><body><p>");
        buf.append("------------------Message forwarded by the XCMAILR---------------<br/><br/>HEADER:<br/><br/>");
        // add the header lines
        // add the header lines

        buf.append("<table>");
        for (Header en : Collections.list(headers))
        {
            buf.append("<tr><td>");
            buf.append(en.getName());
            buf.append("</td><td>");
            buf.append(en.getValue());
            buf.append("</td></tr>");
        }
        buf.append("</table>");
        buf.append("<br/><br/> <b>MESSAGE:</b><br/><br/>");
        String content = body.getContent().toString();
        content = HtmlUtils.readHTMLData(content);
        buf.append(content);
        buf.append("<br/>------------------End of forwarded Message------------------<br/></p></body></html>");
        String quotationString = buf.toString().replace("\n", "<br/>");

        body.setContent(quotationString, "text/html");
        body.setHeader("Content-Transfer-Encoding", "8bit");
        return body;
    }

}
