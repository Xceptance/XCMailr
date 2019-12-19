/**
 * Copyright (C) 2012-2019 the original author or authors.
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

package testutils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;

import models.MBox;
import models.Mail;
import models.User;

/**
 * Creates different types of objects for testing purposes in the database. The objects are mostly filled with random
 * data.
 */
public class TestDataUtils
{
    /**
     * Creates a new user in the database.
     */
    public static User createUser()
    {
        final String firstName = RandomStringUtils.randomAlphabetic(8);
        final String lastName = RandomStringUtils.randomAlphabetic(16);
        final String email = firstName + "." + lastName + "@ccmailr.test";

        final String apiToken = RandomStringUtils.randomAlphanumeric(32);

        final User user = new User(firstName, lastName, email, "1234", "en");
        user.setActive(true);
        user.setApiToken(apiToken);
        user.setApiTokenCreationTimestamp(System.currentTimeMillis());
        user.save();

        return user;
    }

    /**
     * Creates a new mailbox for the given user user in the database.
     */
    public static MBox createMailbox(final User user)
    {
        final String localPart = RandomStringUtils.randomAlphabetic(16);
        final long expirationDate = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1);

        final MBox mailbox = new MBox(localPart, "xcmailr.test", expirationDate, false, user);
        mailbox.setForwardEmails(false);
        mailbox.save();

        return mailbox;
    }

    /**
     * Creates a new mail with attachments in the given mailbox.
     */
    public static Mail createMailWithAttachments(final MBox mailbox) throws Exception
    {
        return createMail(mailbox, TestDataUtils.class.getResource("/controllers/MailWithAttachments.eml"));
    }

    /**
     * Creates a new multi-part mail in the given mailbox.
     */
    public static Mail createMultiPartMail(final MBox mailbox) throws Exception
    {
        return createMail(mailbox, TestDataUtils.class.getResource("/controllers/multiPart.eml"));
    }

    private static Mail createMail(final MBox mailbox, final URL mailUrl) throws IOException, MessagingException
    {
        final InputStream inputStream = mailUrl.openStream();
        final byte[] rawContent = IOUtils.toByteArray(inputStream);
        final MimeMessage mimeMessage = parseMessage(rawContent);

        final Mail mail = new Mail();
        mail.setMailbox(mailbox);
        mail.setMessage(rawContent);
        mail.setReceiveTime(System.currentTimeMillis());
        mail.setSender(mimeMessage.getSender().toString());
        mail.setSubject(mimeMessage.getSubject());
        mail.save();

        return mail;
    }

    private static MimeMessage parseMessage(final byte[] rawData) throws IOException, MessagingException
    {
        final Session session = Session.getInstance(new Properties());
        final MimeMessage mail = new MimeMessage(session, new ByteArrayInputStream(rawData));

        return mail;
    }
}
