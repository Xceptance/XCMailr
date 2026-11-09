/*
 * Copyright (c) Xceptance Software Technologies GmbH
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
package com.xceptance.xcmailr.dto;

import java.util.LinkedList;
import java.util.List;

import jakarta.activation.DataSource;
import jakarta.mail.internet.MimeMessage;

import org.apache.commons.lang3.StringUtils;

import com.xceptance.xcmailr.models.MBox;
import com.xceptance.xcmailr.models.Mail;
import com.xceptance.xcmailr.util.JakartaMimeMessageParser;

/**
 * Data transfer object representing the full details of an email
 * returned by the REST API, including parsed headers, body contents, and attachments.
 */
public class MailData
{
    /**
     * Unique database identifier of the stored email.
     */
    public final long id;

    /**
     * The sender email address as recorded in the SMTP envelope or header.
     */
    public final String sender;

    /**
     * The recipient mailbox address, or {@code null} if no mailbox association was provided.
     */
    public final String recipient;

    /**
     * The email subject line.
     */
    public final String subject;

    /**
     * The parsed plain-text body content, or {@code null} if none is present.
     */
    public final String textContent;

    /**
     * The parsed HTML body content, or {@code null} if none is present.
     */
    public final String htmlContent;

    /**
     * List of attachments contained within the email message.
     */
    public final List<AttachmentData> attachments = new LinkedList<>();

    /**
     * The timestamp in epoch milliseconds when the email was received by XCMailr.
     */
    public final long receivedTime;

    /**
     * The raw RFC 822 / MIME header block of the message.
     */
    public final String headers;

    /**
     * Constructs a {@code MailData} DTO from a persisted {@link Mail} entity without an explicit mailbox entity.
     *
     * @param mail the persisted mail domain entity, must not be {@code null}
     * @throws Exception if an error occurs parsing the raw MIME message bytes
     */
    public MailData(final Mail mail) throws Exception
    {
        this(mail, null);
    }

    /**
     * Constructs a {@code MailData} DTO from a persisted {@link Mail} entity and its parent {@link MBox} entity.
     *
     * @param mail the persisted mail domain entity, must not be {@code null}
     * @param mailbox the parent mailbox entity, or {@code null}
     * @throws Exception if an error occurs parsing the raw MIME message bytes
     */
    public MailData(final Mail mail, final MBox mailbox) throws Exception
    {
        id = mail.getId();
        recipient = mailbox != null ? mailbox.getFullAddress() : null;
        sender = mail.getSender();
        subject = StringUtils.defaultString(mail.getSubject());
        receivedTime = mail.getReceiveTime();

        // Extract raw MIME bytes and parse message contents
        final byte[] rawContent = mail.getMessage();

        if (rawContent != null && rawContent.length > 0)
        {
            final MimeMessage mimeMessage = JakartaMimeMessageParser.createMimeMessage(rawContent);
            final JakartaMimeMessageParser mimeMessageParser = new JakartaMimeMessageParser(mimeMessage);
            mimeMessageParser.parse();

            headers = JakartaMimeMessageParser.getHeaderText(mimeMessage);
            textContent = mimeMessageParser.getPlainContent();
            htmlContent = mimeMessageParser.getHtmlContent();

            // Populate attachment metadata items
            for (final DataSource attachment : mimeMessageParser.getAttachmentList())
            {
                attachments.add(new AttachmentData(attachment));
            }
        }
        else
        {
            headers = null;
            textContent = null;
            htmlContent = null;
        }
    }
}
