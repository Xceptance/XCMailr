/*
 * Copyright (c) 2013-2023 Xceptance Software Technologies GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        if (input == null)
            return;

        for (String field : input)
        {
            field = StringEscapeUtils.escapeHtml(field);
            buf.append(field);
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
        buf.append("<html><body><header>");
        buf.append("------------------Message forwarded by the XCMAILR---------------");
        buf.append("<h1>HEADER</h1><p>");
        buf.append(headerText);
        buf.append("</p></header><main><h1>MESSAGE</h1><article>");
        msgContent = HtmlUtils.readHTMLData(msgContent);
        buf.append(msgContent).append("</article></main><footer>");
        buf.append("------------------End of forwarded Message------------------</footer></body></html>");
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
