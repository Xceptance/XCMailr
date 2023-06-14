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

import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import io.ebean.Ebean;

import models.MBox;
import models.Mail;
import models.User;
import testutils.RestApiClient;
import testutils.RestApiTestUtils;
import testutils.StaticNinjaTest;
import testutils.TestDataUtils;

/**
 * Checks the functionality of the {@link MailApiController}.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MailApiControllerTest extends StaticNinjaTest
{
    // --- test data -----------------------------------

    private static final String unknownAddress = "foo@example.org";

    private static final String invalidAddress = "foo(at)example.org";

    private static final String unknownId = "12345678";

    private static final String invalidId = "totallyInvalidId";

    private static User user;

    private static String emptyMailboxAddress;

    private static MBox mailbox;

    private static String mailboxAddress;

    private static String mailId;

    private static String otherUsersMailboxAddress;

    private static String otherUsersMailId;

    private static RestApiClient apiClient;

    private static Mail mailWithAttachments;

    @BeforeClass
    public static void beforeClass() throws Exception
    {
        // create test user, mailboxes, and mails
        user = TestDataUtils.createUser();
        final String apiToken = user.getApiToken();

        final MBox emptyMailbox = TestDataUtils.createMailbox(user);
        emptyMailboxAddress = emptyMailbox.getFullAddress();

        mailbox = TestDataUtils.createMailbox(user);
        mailboxAddress = mailbox.getFullAddress();

        TestDataUtils.createMultiPartMail(mailbox);
        mailWithAttachments = TestDataUtils.createMailWithAttachments(mailbox);
        mailId = String.valueOf(mailWithAttachments.getId());

        // create another user, mailbox, and mail
        final User otherUser = TestDataUtils.createUser();
        final MBox otherUsersMailbox = TestDataUtils.createMailbox(otherUser);
        otherUsersMailboxAddress = otherUsersMailbox.getFullAddress();

        final Mail otherUsersMail = TestDataUtils.createMultiPartMail(otherUsersMailbox);
        otherUsersMailId = String.valueOf(otherUsersMail.getId());

        // create API client
        apiClient = new RestApiClient(ninjaTestServer, ninjaTestBrowser, apiToken);
    }

    // -----------------------------------------------------------

    /**
     * Checks that listing the mails of an empty mailbox of mine works as expected.
     */
    @Test
    public void listMails_emptyMailbox() throws Exception
    {
        final HttpResponse response = apiClient.listMails(emptyMailboxAddress);

        RestApiTestUtils.validateStatusCode(response, 200);

        final MailData[] mailDatas = RestApiTestUtils.getResponseBodyAs(response, MailData[].class);
        Assert.assertEquals(0, mailDatas.length);
    }

    /**
     * Checks that listing the mails of a non-empty mailbox of mine works as expected.
     */
    @Test
    public void listMails_nonEmptyMailbox() throws Exception
    {
        final HttpResponse response = apiClient.listMails(mailboxAddress);

        RestApiTestUtils.validateStatusCode(response, 200);

        final MailData[] mailDatas = RestApiTestUtils.getResponseBodyAs(response, MailData[].class);
        Assert.assertEquals(2, mailDatas.length);
    }

    /**
     * Checks that listing the mails of a non-empty mailbox of mine with last match set returns a list with a single
     * mail.
     */
    @Test
    public void listMails_withLastMatchOnly() throws Exception
    {
        HttpResponse response = apiClient.listMails(mailboxAddress, true);

        RestApiTestUtils.validateStatusCode(response, 200);

        MailData[] mailDatas = RestApiTestUtils.getResponseBodyAs(response, MailData[].class);
        Assert.assertEquals(1, mailDatas.length);
        Assert.assertEquals(mailWithAttachments.getSubject(), mailDatas[0].subject);
    }

    /**
     * Checks that listing the mails of a non-empty mailbox of mine, which are additionally filtered by a subject
     * pattern, works as expected.
     */
    @Test
    public void listMails_withSubjectFilter() throws Exception
    {
        // select mail with attachments
        HttpResponse response = apiClient.listMails(mailboxAddress, "attachments");

        RestApiTestUtils.validateStatusCode(response, 200);

        MailData[] mailDatas = RestApiTestUtils.getResponseBodyAs(response, MailData[].class);
        Assert.assertEquals(1, mailDatas.length);

        // select multi-part mail
        response = apiClient.listMails(mailboxAddress, "Warenkorb");

        RestApiTestUtils.validateStatusCode(response, 200);

        mailDatas = RestApiTestUtils.getResponseBodyAs(response, MailData[].class);
        Assert.assertEquals(1, mailDatas.length);

        // filter does not match any mail
        response = apiClient.listMails(mailboxAddress, "nonsense");

        RestApiTestUtils.validateStatusCode(response, 200);

        mailDatas = RestApiTestUtils.getResponseBodyAs(response, MailData[].class);
        Assert.assertEquals(0, mailDatas.length);
    }

    /**
     * Checks that listing the mails of another user's mailbox fails with an error.
     */
    @Test
    public void listMails_someoneElsesMailbox() throws Exception
    {
        final HttpResponse response = apiClient.listMails(otherUsersMailboxAddress);

        RestApiTestUtils.validateStatusCode(response, 403);
        RestApiTestUtils.validateErrors(response, "mailboxAddress");
    }

    /**
     * Checks that listing the mails of a mailbox with an invalid subject pattern fails with an error.
     */
    @Test
    public void listMails_invalidSubjectPattern() throws Exception
    {
        final HttpResponse response = apiClient.listMails(mailboxAddress, "[xyz");

        RestApiTestUtils.validateStatusCode(response, 400);
        RestApiTestUtils.validateErrors(response, "subject");
    }

    /**
     * Checks that listing the mails of a mailbox with an invalid mailbox ID fails with an error.
     */
    @Test
    public void listMails_invalidId() throws Exception
    {
        final HttpResponse response = apiClient.listMails(invalidAddress);

        RestApiTestUtils.validateStatusCode(response, 400);
        RestApiTestUtils.validateErrors(response, "mailboxAddress");
    }

    /**
     * Checks that listing the mails of a mailbox with an unknown mailbox ID fails with an error.
     */
    @Test
    public void listMails_unknownMailbox() throws Exception
    {
        final HttpResponse response = apiClient.listMails(unknownAddress);

        // TODO: 404 or empty list?
        RestApiTestUtils.validateStatusCode(response, 404);
    }

    // -----------------------------------------------------------

    /**
     * Checks that retrieving my existing mail works as expected.
     */
    @Test
    public void getMail() throws Exception
    {
        final HttpResponse response = apiClient.getMail(mailId);

        RestApiTestUtils.validateStatusCode(response, 200);
    }

    /**
     * Checks that retrieving another user's mail fails with an error.
     */
    @Test
    public void getMail_someoneElsesMail() throws Exception
    {
        final HttpResponse response = apiClient.getMail(otherUsersMailId);

        RestApiTestUtils.validateStatusCode(response, 403);
        RestApiTestUtils.validateErrors(response, "mailId");
    }

    /**
     * Checks that retrieving a mail by an invalid ID fails with an error.
     */
    @Test
    public void getMail_invalidId() throws Exception
    {
        final HttpResponse response = apiClient.getMail(invalidId);

        RestApiTestUtils.validateStatusCode(response, 400);
        RestApiTestUtils.validateErrors(response, "mailId");
    }

    /**
     * Checks that retrieving a mail by an unknown ID fails with an error.
     */
    @Test
    public void getMail_unknownMail() throws Exception
    {
        final HttpResponse response = apiClient.getMail(unknownId);

        RestApiTestUtils.validateStatusCode(response, 404);
    }

    // -----------------------------------------------------------

    /**
     * Checks that retrieving an existing attachment works as expected.
     */
    @Test
    public void getMailAttachment() throws Exception
    {
        // 1st attachment
        HttpResponse response = apiClient.getMailAttachment(mailId, "test.pdf");

        RestApiTestUtils.validateStatusCode(response, 200);
        RestApiTestUtils.validateResponseContent(response, "application/pdf", 89399);

        // 2nd attachment
        response = apiClient.getMailAttachment(mailId, "test.png");

        RestApiTestUtils.validateStatusCode(response, 200);
        RestApiTestUtils.validateResponseContent(response, "image/png", 5799);
    }

    /**
     * Checks that retrieving an unknown attachment fails with an error
     */
    @Test
    public void getMailAttachment_unknownName() throws Exception
    {
        final HttpResponse response = apiClient.getMailAttachment(mailId, "unknownAttachmentName");

        // validate response
        RestApiTestUtils.validateStatusCode(response, 404);
    }

    // -----------------------------------------------------------

    /**
     * Checks that deleting my mail with a valid ID works as expected.
     */
    @Test
    public void deleteMail() throws Exception
    {
        // prepare a doomed mail and check it's in the DB
        final Mail mail = TestDataUtils.createMailWithAttachments(mailbox);
        Assert.assertNotNull(Ebean.find(Mail.class, mail.getId()));

        // make the API call
        final HttpResponse response = apiClient.deleteMail(String.valueOf(mail.getId()));

        // validate response
        RestApiTestUtils.validateStatusCode(response, 204);

        // validate database
        Assert.assertNull(Ebean.find(Mail.class, mail.getId()));
    }

    /**
     * Checks that deleting another user's mail fails with an error.
     */
    @Test
    public void deleteMail_someoneElsesMail() throws Exception
    {
        final HttpResponse response = apiClient.deleteMail(otherUsersMailId);

        // validate response
        RestApiTestUtils.validateStatusCode(response, 403);
        RestApiTestUtils.validateErrors(response, "mailId");
    }

    /**
     * Checks that deleting a mail with an invalid ID fails with an error.
     */
    @Test
    public void deleteMail_invalidMailId() throws Exception
    {
        final HttpResponse response = apiClient.deleteMail(invalidId);

        // validate response
        RestApiTestUtils.validateStatusCode(response, 400);
        RestApiTestUtils.validateErrors(response, "mailId");
    }

    /**
     * Checks that deleting a mail with an unknown ID fails with an error.
     */
    @Test
    public void deleteMail_unknownMailId() throws Exception
    {
        final HttpResponse response = apiClient.deleteMail(unknownId);

        // validate response
        RestApiTestUtils.validateStatusCode(response, 404);
    }
}
