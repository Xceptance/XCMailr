/*
 * Copyright (c) 2013-2026 Xceptance Software Technologies GmbH
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
package com.xceptance.xcmailr.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import jakarta.activation.DataSource;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.ContentType;
import jakarta.mail.internet.MimeMessage;

/**
 * Parser for MIME messages using the modern Jakarta Mail API (Jakarta EE 10+).
 * Extracts plain text, HTML body, and binary attachments.
 *
 * @author Xceptance Software Technologies GmbH
 */
public final class JakartaMimeMessageParser
{
    private final MimeMessage message;
    private String plainContent;
    private String htmlContent;
    private final List<DataSource> attachments = new ArrayList<>();

    /**
     * Constructs a parser for the given {@link MimeMessage}.
     *
     * @param message Jakarta MIME message
     */
    public JakartaMimeMessageParser(final MimeMessage message)
    {
        this.message = message;
    }

    /**
     * Parses the MIME message and its multi-part children.
     *
     * @return this parser instance
     * @throws Exception if reading the message fails
     */
    public JakartaMimeMessageParser parse() throws Exception
    {
        parsePart(this.message);
        return this;
    }

    /**
     * Returns default {@link Properties} configured for comprehensive UTF-8 header decoding,
     * attachment filename resolution, and resilient encoded-word parsing.
     *
     * @return populated mail session properties
     */
    public static Properties getDefaultSessionProperties()
    {
        final Properties props = new Properties();
        props.put("mail.mime.allowutf8", "true");
        props.put("mail.mime.charset", "UTF-8");
        props.put("mail.mime.decodefilename", "true");
        props.put("mail.mime.decodetext.strict", "false");
        return props;
    }

    /**
     * Creates a {@link MimeMessage} from raw bytes.
     *
     * @param rawContent raw email byte stream
     * @return constructed {@link MimeMessage}
     * @throws Exception if parsing fails
     */
    public static MimeMessage createMimeMessage(final byte[] rawContent) throws Exception
    {
        final Session session = Session.getInstance(getDefaultSessionProperties());
        try (final InputStream in = new ByteArrayInputStream(rawContent))
        {
            return new MimeMessage(session, in);
        }
    }

    private void parsePart(final Part part) throws Exception
    {
        if (part == null)
        {
            return;
        }

        final String disposition = part.getDisposition();
        final String fileName = part.getFileName();
        final boolean isAttachment = Part.ATTACHMENT.equalsIgnoreCase(disposition)
                                     || (fileName != null && !fileName.isBlank());

        if (isAttachment)
        {
            attachments.add(part.getDataHandler().getDataSource());
            return;
        }

        if (part.isMimeType("text/plain"))
        {
            if (plainContent == null)
            {
                plainContent = extractTextContent(part);
            }
        }
        else if (part.isMimeType("text/html"))
        {
            if (htmlContent == null)
            {
                htmlContent = extractTextContent(part);
            }
        }
        else if (part.isMimeType("multipart/*"))
        {
            final Object content = part.getContent();
            if (content instanceof Multipart multipart)
            {
                for (int i = 0; i < multipart.getCount(); i++)
                {
                    parsePart(multipart.getBodyPart(i));
                }
            }
        }
    }

    private String extractTextContent(final Part part)
    {
        try
        {
            String declaredCharset = null;
            final String[] contentTypes = part.getHeader("Content-Type");
            if (contentTypes != null && contentTypes.length > 0 && contentTypes[0] != null)
            {
                try
                {
                    final ContentType ct = new ContentType(contentTypes[0]);
                    declaredCharset = ct.getParameter("charset");
                }
                catch (final Exception ignored)
                {
                    // Ignore unparseable Content-Type header
                }
            }

            try (final InputStream in = part.getInputStream())
            {
                final byte[] bytes = in.readAllBytes();
                if (declaredCharset != null && !declaredCharset.isBlank() && !"us-ascii".equalsIgnoreCase(declaredCharset.trim()))
                {
                    try
                    {
                        final Charset charset = Charset.forName(declaredCharset.trim());
                        return new String(bytes, charset);
                    }
                    catch (final Exception ignored)
                    {
                        // Fall back to UTF-8 / ISO-8859-1 decoding below
                    }
                }

                try
                {
                    final CharsetDecoder utf8Decoder = StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT);
                    return utf8Decoder.decode(ByteBuffer.wrap(bytes)).toString();
                }
                catch (final CharacterCodingException e)
                {
                    return new String(bytes, StandardCharsets.ISO_8859_1);
                }
            }
        }
        catch (final Exception e)
        {
            try
            {
                final Object content = part.getContent();
                return content != null ? content.toString() : null;
            }
            catch (final Exception ignored)
            {
                return null;
            }
        }
    }

    /**
     * @return plain text body content if present, or {@code null}
     */
    public String getPlainContent()
    {
        return plainContent;
    }

    /**
     * @return HTML body content if present, or {@code null}
     */
    public String getHtmlContent()
    {
        return htmlContent;
    }

    /**
     * @return unmodifiable list of extracted attachments
     */
    public List<DataSource> getAttachmentList()
    {
        return Collections.unmodifiableList(attachments);
    }

    /**
     * Extracts all raw header lines from the given MIME message as CRLF-separated text.
     *
     * @param message Jakarta MIME message
     * @return header text
     * @throws jakarta.mail.MessagingException on mail error
     */
    public static String getHeaderText(final MimeMessage message) throws jakarta.mail.MessagingException
    {
        final StringBuilder sb = new StringBuilder();
        final java.util.Enumeration<String> e = message.getAllHeaderLines();

        boolean first = true;
        while (e.hasMoreElements())
        {
            if (!first)
            {
                sb.append("\r\n");
            }
            first = false;
            sb.append(e.nextElement());
        }
        return sb.toString();
    }
}
