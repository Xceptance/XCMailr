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

    public final String textContent;

    public final String htmlContent;

    public final List<AttachmentData> attachments = new LinkedList<>();

    public final long receivedTime;

    public final String headers;

    public MailData(final Mail mail) throws Exception
    {
        id = mail.getId();
        recipient = mail.getMailbox().getFullAddress();
        sender = mail.getSender();
        subject = StringUtils.defaultString(mail.getSubject());
        receivedTime = mail.getReceiveTime();

        final byte[] rawContent = mail.getMessage();

        if (rawContent != null && rawContent.length > 0)
        {
            final MimeMessage mimeMessage = MimeMessageUtils.createMimeMessage(null, rawContent);
            final MimeMessageParser mimeMessageParser = new MimeMessageParser(mimeMessage);
            mimeMessageParser.parse();

            headers = HelperUtils.getHeaderText(mimeMessage);

            textContent = mimeMessageParser.getPlainContent();
            htmlContent = mimeMessageParser.getHtmlContent();

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
