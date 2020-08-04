/*
 *  Copyright 2020 by the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package controllers.restapi;

import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import models.MBox;
import models.User;
import testutils.RestApiClient;
import testutils.RestApiTestUtils;
import testutils.StaticNinjaTest;
import testutils.TestDataUtils;

/**
 * Checks the functionality of the {@link MailboxApiController}.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MailboxApiControllerTest extends StaticNinjaTest
{
    // --- test data -----------------------------------

    private static final String invalidAddress = "totallyInvalidAddress";

    private static final String unknownAddress = "foo@example.org";

    private User user;

    private RestApiClient apiClient;

    @Before
    public void before()
    {
        user = TestDataUtils.createUser();
        apiClient = new RestApiClient(ninjaTestServer, ninjaTestBrowser, user.getApiToken());
    }

    // -----------------------------------------------------------

    @Test
    public void listMailboxes_emptyList() throws Exception
    {
        // execute request
        final HttpResponse response = apiClient.listMailboxes();

        // validate response
        RestApiTestUtils.validateStatusCode(response, 200);

        final MailboxData[] mailboxData = RestApiTestUtils.getResponseBodyAs(response, MailboxData[].class);
        Assert.assertEquals(0, mailboxData.length);
    }

    @Test
    public void listMailboxes_nonEmptyList() throws Exception
    {
        // prepare data
        final MBox mailbox0 = TestDataUtils.createMailbox(user);
        final MBox mailbox1 = TestDataUtils.createMailbox(user);

        // execute request
        final HttpResponse response = apiClient.listMailboxes();

        // validate response
        RestApiTestUtils.validateStatusCode(response, 200);

        final MailboxData[] mailboxData = RestApiTestUtils.getResponseBodyAs(response, MailboxData[].class);
        Assert.assertEquals(2, mailboxData.length);
        RestApiTestUtils.validateMailboxData(mailboxData[0], mailbox0);
        RestApiTestUtils.validateMailboxData(mailboxData[1], mailbox1);
    }

    // -----------------------------------------------------------

    /**
     * Checks that creating a new mailbox works as expected.
     */
    @Test
    public void createMailbox() throws Exception
    {
        // execute request
        final String mailboxAddress = "foo@xcmailr.test";
        final long expirationDate = System.currentTimeMillis();
        final boolean forwardEnabled = true;

        final HttpResponse response = apiClient.createMailbox(mailboxAddress, expirationDate, forwardEnabled);

        // validate response
        RestApiTestUtils.validateStatusCode(response, 201);

        final MailboxData mailboxData = RestApiTestUtils.getResponseBodyAs(response, MailboxData.class);
        RestApiTestUtils.validateMailboxData(mailboxData, mailboxAddress, expirationDate, forwardEnabled);

        // validate database
        MBox mailbox = MBox.getByAddress(mailboxData.address);
        RestApiTestUtils.validateMailbox(mailbox, mailboxData);
    }

    /**
     * Checks that creating a mailbox with an address that is already taken by another mailbox of mine fails with an
     * error.
     */
    @Test
    public void createMailbox_emailAlreadyTakenBySameUser() throws Exception
    {
        // prepare data
        MBox mailbox = TestDataUtils.createMailbox(user);

        // execute request
        final String mailboxAddress = mailbox.getFullAddress();
        final long expirationDate = System.currentTimeMillis();
        final boolean forwardEnabled = true;

        final HttpResponse response = apiClient.createMailbox(mailboxAddress, expirationDate, forwardEnabled);

        // validate response
        RestApiTestUtils.validateStatusCode(response, 200);

        final MailboxData mailboxData = RestApiTestUtils.getResponseBodyAs(response, MailboxData.class);
        RestApiTestUtils.validateMailboxData(mailboxData, mailboxAddress, expirationDate, forwardEnabled);

        // validate database
        mailbox = MBox.getById(mailbox.getId());
        RestApiTestUtils.validateMailbox(mailbox, mailboxData);
    }

    /**
     * Checks that creating a mailbox with an address that is already taken by another user's mailbox fails with an
     * error.
     */
    @Test
    public void createMailbox_emailAlreadyTakenByOtherUser() throws Exception
    {
        // prepare data
        final User otherUser = TestDataUtils.createUser();
        final MBox otherUsersMailbox = TestDataUtils.createMailbox(otherUser);

        final String mailboxAddress = otherUsersMailbox.getFullAddress();
        final long expirationDate = System.currentTimeMillis();
        final boolean forwardEnabled = true;

        // execute request
        final HttpResponse response = apiClient.createMailbox(mailboxAddress, expirationDate, forwardEnabled);

        // validate response
        RestApiTestUtils.validateStatusCode(response, 403);
    }

    /**
     * Checks that creating a mailbox with an invalid email address fails with an error.
     */
    @Test
    public void createMailbox_invalidEmail() throws Exception
    {
        // execute request
        final HttpResponse response = apiClient.createMailbox("foo(at)bar.com", System.currentTimeMillis(), false);

        // validate response
        RestApiTestUtils.validateStatusCode(response, 400);
        RestApiTestUtils.validateErrors(response, "address");
    }

    /**
     * Checks that creating a mailbox with an address the domain of which is unknown to XCMailr fails with an error.
     */
    @Test
    public void createMailbox_unknownMailDomain() throws Exception
    {
        // execute request
        final HttpResponse response = apiClient.createMailbox("foo@bar.com", System.currentTimeMillis(), false);

        // validate response
        RestApiTestUtils.validateStatusCode(response, 403);
    }

    // -----------------------------------------------------------

    /**
     * Checks that retrieving the details of my mailbox works as expected.
     */
    @Test
    public void getMailbox() throws Exception
    {
        // prepare data
        final MBox mailbox = TestDataUtils.createMailbox(user);

        // execute request
        final HttpResponse response = apiClient.getMailbox(mailbox.getFullAddress());

        // validate response
        RestApiTestUtils.validateStatusCode(response, 200);

        final MailboxData mailboxData = RestApiTestUtils.getResponseBodyAs(response, MailboxData.class);
        RestApiTestUtils.validateMailboxData(mailboxData, mailbox);
    }

    /**
     * Checks that retrieving the details of another user's mailbox fails with an error.
     */
    @Test
    public void getMailbox_someoneElsesMailbox() throws Exception
    {
        // prepare data
        final User otherUser = TestDataUtils.createUser();
        final MBox otherUsersMailbox = TestDataUtils.createMailbox(otherUser);

        // execute request
        final HttpResponse response = apiClient.getMailbox(otherUsersMailbox.getFullAddress());

        // validate response
        RestApiTestUtils.validateStatusCode(response, 403);
        RestApiTestUtils.validateErrors(response, "mailboxAddress");
    }

    /**
     * Checks that retrieving the details of a mailbox with an invalid ID fails with an error.
     */
    // @Test
    // public void getMailbox_invalidId() throws Exception
    // {
    // // execute request
    // final HttpResponse response = apiClient.getMailbox(invalidId);
    //
    // // validate response
    // RestApiTestUtils.validateStatusCode(response, 400);
    // RestApiTestUtils.validateErrors(response, "id");
    // }

    /**
     * Checks that retrieving the details of a mailbox with an invalid address fails with an error.
     */
    @Test
    public void getMailbox_invalidAddress() throws Exception
    {
        // execute request
        final HttpResponse response = apiClient.getMailbox(invalidAddress);

        // validate response
        RestApiTestUtils.validateStatusCode(response, 400);
        RestApiTestUtils.validateErrors(response, "mailboxAddress");
    }

    /**
     * Checks that retrieving the details of a mailbox with an unknown ID fails with an error.
     */
    @Test
    public void getMailbox_unknownMailbox() throws Exception
    {
        // execute request
        final HttpResponse response = apiClient.getMailbox(unknownAddress);

        // validate response
        RestApiTestUtils.validateStatusCode(response, 404);
    }

    // -----------------------------------------------------------

    /**
     * Checks that updating my mailbox works as expected.
     */
    @Test
    public void updateMailbox() throws Exception
    {
        // prepare data
        MBox mailbox = TestDataUtils.createMailbox(user);
        final String mailboxAddress = "updated@xcmailr.test";
        final long expirationDate = System.currentTimeMillis();
        final boolean forwardEnabled = true;

        // execute request
        final HttpResponse response = apiClient.updateMailbox(mailbox.getFullAddress(), mailboxAddress, expirationDate,
                                                              forwardEnabled);

        // validate response
        RestApiTestUtils.validateStatusCode(response, 200);

        final MailboxData mailboxData = RestApiTestUtils.getResponseBodyAs(response, MailboxData.class);
        RestApiTestUtils.validateMailboxData(mailboxData, mailboxAddress, expirationDate, forwardEnabled);

        // validate database
        mailbox = MBox.getById(mailbox.getId());
        RestApiTestUtils.validateMailbox(mailbox, mailboxData);
    }

    /**
     * Checks that updating my mailbox with the same email address works as expected.
     */
    @Test
    public void updateMailbox_sameEmail() throws Exception
    {
        // prepare data
        MBox mailbox = TestDataUtils.createMailbox(user);
        final String mailboxAddress = mailbox.getFullAddress();
        final long expirationDate = System.currentTimeMillis();
        final boolean forwardEnabled = true;

        // execute request
        final HttpResponse response = apiClient.updateMailbox(mailboxAddress, mailboxAddress, expirationDate,
                                                              forwardEnabled);

        // validate response
        RestApiTestUtils.validateStatusCode(response, 200);

        final MailboxData mailboxData = RestApiTestUtils.getResponseBodyAs(response, MailboxData.class);
        RestApiTestUtils.validateMailboxData(mailboxData, mailboxAddress, expirationDate, forwardEnabled);

        // validate database
        mailbox = MBox.getById(mailbox.getId());
        RestApiTestUtils.validateMailbox(mailbox, mailboxData);
    }

    /**
     * Checks that updating my mailbox with an address that is already taken by another mailbox of mine fails with an
     * error.
     */
    @Test
    public void updateMailbox_emailAlreadyTakenBySameUser() throws Exception
    {
        // prepare data
        MBox mailbox = TestDataUtils.createMailbox(user);
        MBox otherMailbox = TestDataUtils.createMailbox(user);

        // execute request
        final HttpResponse response = apiClient.updateMailbox(mailbox.getFullAddress(), otherMailbox.getFullAddress(),
                                                              System.currentTimeMillis(), false);

        // validate response
        RestApiTestUtils.validateStatusCode(response, 403);
        RestApiTestUtils.validateErrors(response, "mailboxAddress");
    }

    /**
     * Checks that updating my mailbox with an address that is already taken by another user's mailbox fails with an
     * error.
     */
    @Test
    public void updateMailbox_emailAlreadyTakenByOtherUser() throws Exception
    {
        // prepare data
        final MBox mailbox = TestDataUtils.createMailbox(user);

        final User otherUser = TestDataUtils.createUser();
        final MBox otherUsersMailbox = TestDataUtils.createMailbox(otherUser);

        // execute request
        final HttpResponse response = apiClient.updateMailbox(mailbox.getFullAddress(),
                                                              otherUsersMailbox.getFullAddress(),
                                                              System.currentTimeMillis(), false);

        // validate response
        RestApiTestUtils.validateStatusCode(response, 403);
        RestApiTestUtils.validateErrors(response, "mailboxAddress");
    }

    /**
     * Checks that updating another users's mailbox fails with an error.
     */
    @Test
    public void updateMailbox_someoneElsesMailbox() throws Exception
    {
        // prepare data
        final User otherUser = TestDataUtils.createUser();
        final MBox otherUsersMailbox = TestDataUtils.createMailbox(otherUser);

        // execute request
        final HttpResponse response = apiClient.updateMailbox(otherUsersMailbox.getFullAddress(), "foo@bar.com",
                                                              System.currentTimeMillis(), false);

        // validate response
        RestApiTestUtils.validateStatusCode(response, 403);
        RestApiTestUtils.validateErrors(response, "mailboxAddress");
    }

    /**
     * Checks that updating a mailbox with an invalid ID fails with an error.
     */
    // @Test
    // public void updateMailbox_invalidId() throws Exception
    // {
    // // execute request
    // final HttpResponse response = apiClient.updateMailbox(invalidId, "foo@bar.com", System.currentTimeMillis(),
    // false);
    //
    // // validate response
    // RestApiTestUtils.validateStatusCode(response, 400);
    // RestApiTestUtils.validateErrors(response, "id");
    // }

    /**
     * Checks that updating a mailbox with invalid email address fails with an error.
     */
    @Test
    public void updateMailbox_invalidEmail() throws Exception
    {
        // prepare data
        final MBox mailbox = TestDataUtils.createMailbox(user);

        // execute request
        final HttpResponse response = apiClient.updateMailbox(mailbox.getFullAddress(), "foo(at)bar.com",
                                                              System.currentTimeMillis(), false);

        // validate response
        RestApiTestUtils.validateStatusCode(response, 400);
        RestApiTestUtils.validateErrors(response, "address");
    }

    /**
     * Checks that updating a mailbox with an address the domain of which is unknown to XCMailr fails with an error.
     */
    @Test
    public void updateMailbox_unknownMailDomain() throws Exception
    {
        // prepare data
        final MBox mailbox = TestDataUtils.createMailbox(user);

        // execute request
        final HttpResponse response = apiClient.updateMailbox(mailbox.getFullAddress(), "foo@bar.com",
                                                              System.currentTimeMillis(), false);

        // validate response
        RestApiTestUtils.validateStatusCode(response, 403);
        RestApiTestUtils.validateErrors(response, "mailboxAddress");
    }

    // -----------------------------------------------------------

    /**
     * Checks that deleting my mailbox works as expected.
     */
    @Test
    public void deleteMailbox() throws Exception
    {
        // prepare data
        final MBox mailbox = TestDataUtils.createMailbox(user);

        // validate database
        Assert.assertNotNull(MBox.getById(mailbox.getId()));

        // execute request
        final HttpResponse response = apiClient.deleteMailbox(mailbox.getFullAddress());

        // validate response
        RestApiTestUtils.validateStatusCode(response, 204);

        // validate database
        Assert.assertNull(MBox.getById(mailbox.getId()));
    }

    /**
     * Checks that deleting another user's mailbox fails with an error.
     */
    @Test
    public void deleteMailbox_someoneElsesMailbox() throws Exception
    {
        // prepare data
        final User otherUser = TestDataUtils.createUser();
        final MBox otherUsersMailbox = TestDataUtils.createMailbox(otherUser);

        // execute request
        final HttpResponse response = apiClient.deleteMailbox(otherUsersMailbox.getFullAddress());

        // validate response
        RestApiTestUtils.validateStatusCode(response, 403);
    }

    /**
     * Checks that deleting a mailbox by an invalid address fails with an error.
     */
    @Test
    public void deleteMailbox_invalidMailboxAddress() throws Exception
    {
        // execute request
        final HttpResponse response = apiClient.deleteMailbox(invalidAddress);

        // validate response
        RestApiTestUtils.validateStatusCode(response, 400);
        RestApiTestUtils.validateErrors(response, "mailboxAddress");
    }

    /**
     * Checks that deleting a mailbox by an unknown ID fails with an error.
     */
    @Test
    public void deleteMailbox_unknownMailbox() throws Exception
    {
        // execute request
        final HttpResponse response = apiClient.deleteMailbox(unknownAddress);

        // validate response
        RestApiTestUtils.validateStatusCode(response, 404);
    }
}
