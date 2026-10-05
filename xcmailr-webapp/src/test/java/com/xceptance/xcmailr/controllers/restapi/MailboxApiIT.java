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
package com.xceptance.xcmailr.controllers.restapi;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.restapi.MailboxData;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

import models.MBox;
import models.User;

/**
 * Integration characterization tests for {@code /api/v1/mailboxes} endpoint verifying
 * compatibility with legacy JSON schemas, validation rules, status codes, and security.
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest(classes = com.xceptance.xcmailr.XcmailrApplication.class)
@ActiveProfiles("test")
@Transactional
public class MailboxApiIT
{
    private static final String API_PREFIX = "/api/v1/mailboxes";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailboxRepository mailboxRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;
    private User testUser;
    private String token;

    @BeforeEach
    public void setUp()
    {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                                 .apply(springSecurity())
                                 .build();

        testUser = new User("John", "Doe", "john.doe@xcmailr.test", "$2a$10$abcdefghijklmnopqrstuv", "en");
        testUser.setActive(true);
        testUser.setApiToken("test-api-token-" + System.nanoTime());
        testUser = userRepository.save(testUser);
        token = testUser.getApiToken();
    }

    @Test
    @DisplayName("listMailboxes returns empty array when user has no mailboxes")
    public void testListMailboxesEmpty() throws Exception
    {
        mockMvc.perform(get(API_PREFIX)
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("listMailboxes returns mailboxes owned by the user")
    public void testListMailboxesNonEmpty() throws Exception
    {
        final MBox box1 = new MBox("box1", "xcmailr.test", System.currentTimeMillis() + 60000, false, testUser);
        final MBox box2 = new MBox("box2", "xcmailr.test", System.currentTimeMillis() + 60000, false, testUser);
        mailboxRepository.save(box1);
        mailboxRepository.save(box2);

        mockMvc.perform(get(API_PREFIX)
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(2)))
               .andExpect(jsonPath("$[0].address", is("box1@xcmailr.test")))
               .andExpect(jsonPath("$[1].address", is("box2@xcmailr.test")));
    }

    @Test
    @DisplayName("createMailbox creates new mailbox and returns 201 Created")
    public void testCreateMailboxSuccess() throws Exception
    {
        final MailboxData data = new MailboxData();
        data.address = "newuserbox@xcmailr.test";
        data.deactivationTime = System.currentTimeMillis() + 3600000;
        data.forwardEnabled = true;

        mockMvc.perform(post(API_PREFIX)
               .header("Authorization", "Bearer " + token)
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(data)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.address", is("newuserbox@xcmailr.test")))
               .andExpect(jsonPath("$.forwardEnabled", is(true)));

        assertTrue(mailboxRepository.existsByAddressIgnoreCaseAndDomainIgnoreCase("newuserbox", "xcmailr.test"));
    }

    @Test
    @DisplayName("createMailbox with same user's existing address reactivates and returns 200 OK")
    public void testCreateMailboxReactivateSameUser() throws Exception
    {
        final MBox existing = new MBox("reactivate", "xcmailr.test", System.currentTimeMillis() - 1000, true, testUser);
        mailboxRepository.save(existing);

        final MailboxData data = new MailboxData();
        data.address = "reactivate@xcmailr.test";
        final long futureTime = System.currentTimeMillis() + 7200000;
        data.deactivationTime = futureTime;
        data.forwardEnabled = false;

        mockMvc.perform(post(API_PREFIX)
               .header("Authorization", "Bearer " + token)
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(data)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.address", is("reactivate@xcmailr.test")))
               .andExpect(jsonPath("$.forwardEnabled", is(false)));

        final MBox updated = mailboxRepository.findById(existing.getId()).orElseThrow();
        assertFalse(updated.isExpired());
    }

    @Test
    @DisplayName("createMailbox with address taken by other user returns 403 Forbidden")
    public void testCreateMailboxTakenByOtherUser() throws Exception
    {
        final User otherUser = new User("Other", "User", "other@xcmailr.test", "hash", "en");
        otherUser.setActive(true);
        otherUser.setApiToken("other-token-" + System.nanoTime());
        userRepository.save(otherUser);

        final MBox otherBox = new MBox("taken", "xcmailr.test", System.currentTimeMillis() + 60000, false, otherUser);
        mailboxRepository.save(otherBox);

        final MailboxData data = new MailboxData();
        data.address = "taken@xcmailr.test";
        data.deactivationTime = System.currentTimeMillis() + 3600000;
        data.forwardEnabled = true;

        mockMvc.perform(post(API_PREFIX)
               .header("Authorization", "Bearer " + token)
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(data)))
               .andExpect(status().isForbidden())
               .andExpect(jsonPath("$.errors[0].parameter", is("mailboxAddress")));
    }

    @Test
    @DisplayName("createMailbox with invalid email returns 400 Bad Request")
    public void testCreateMailboxInvalidEmail() throws Exception
    {
        final MailboxData data = new MailboxData();
        data.address = "invalid-email-address";
        data.deactivationTime = System.currentTimeMillis() + 3600000;
        data.forwardEnabled = true;

        mockMvc.perform(post(API_PREFIX)
               .header("Authorization", "Bearer " + token)
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(data)))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.errors[0].parameter", is("address")));
    }

    @Test
    @DisplayName("createMailbox with disallowed domain returns 403 Forbidden")
    public void testCreateMailboxDisallowedDomain() throws Exception
    {
        final MailboxData data = new MailboxData();
        data.address = "foo@unauthorized-domain.com";
        data.deactivationTime = System.currentTimeMillis() + 3600000;
        data.forwardEnabled = true;

        mockMvc.perform(post(API_PREFIX)
               .header("Authorization", "Bearer " + token)
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(data)))
               .andExpect(status().isForbidden())
               .andExpect(jsonPath("$.errors[0].parameter", is("address")));
    }

    @Test
    @DisplayName("getMailbox returns mailbox details for valid address")
    public void testGetMailboxSuccess() throws Exception
    {
        final MBox box = new MBox("mygetbox", "xcmailr.test", System.currentTimeMillis() + 60000, false, testUser);
        mailboxRepository.save(box);

        mockMvc.perform(get(API_PREFIX + "/mygetbox@xcmailr.test")
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.address", is("mygetbox@xcmailr.test")));
    }

    @Test
    @DisplayName("getMailbox for non-existent mailbox returns 404 Not Found")
    public void testGetMailboxNotFound() throws Exception
    {
        mockMvc.perform(get(API_PREFIX + "/unknown@xcmailr.test")
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("getMailbox for another user's mailbox returns 403 Forbidden")
    public void testGetMailboxOtherUser() throws Exception
    {
        final User otherUser = new User("Other2", "User", "other2@xcmailr.test", "hash", "en");
        otherUser.setActive(true);
        otherUser.setApiToken("other2-token-" + System.nanoTime());
        userRepository.save(otherUser);

        final MBox otherBox = new MBox("otherget", "xcmailr.test", System.currentTimeMillis() + 60000, false, otherUser);
        mailboxRepository.save(otherBox);

        mockMvc.perform(get(API_PREFIX + "/otherget@xcmailr.test")
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isForbidden())
               .andExpect(jsonPath("$.errors[0].parameter", is("mailboxAddress")));
    }

    @Test
    @DisplayName("getMailbox with invalid address returns 400 Bad Request")
    public void testGetMailboxInvalidAddress() throws Exception
    {
        mockMvc.perform(get(API_PREFIX + "/not-an-email")
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.errors[0].parameter", is("mailboxAddress")));
    }

    @Test
    @DisplayName("updateMailbox successfully updates mailbox details")
    public void testUpdateMailboxSuccess() throws Exception
    {
        final MBox box = new MBox("oldname", "xcmailr.test", System.currentTimeMillis() + 60000, false, testUser);
        mailboxRepository.save(box);

        final MailboxData update = new MailboxData();
        update.address = "newname@xcmailr.test";
        update.deactivationTime = System.currentTimeMillis() + 120000;
        update.forwardEnabled = true;

        mockMvc.perform(put(API_PREFIX + "/oldname@xcmailr.test")
               .header("Authorization", "Bearer " + token)
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(update)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.address", is("newname@xcmailr.test")))
               .andExpect(jsonPath("$.forwardEnabled", is(true)));

        assertTrue(mailboxRepository.existsByAddressIgnoreCaseAndDomainIgnoreCase("newname", "xcmailr.test"));
        assertFalse(mailboxRepository.existsByAddressIgnoreCaseAndDomainIgnoreCase("oldname", "xcmailr.test"));
    }

    @Test
    @DisplayName("updateMailbox with address taken by other user returns 403 Forbidden")
    public void testUpdateMailboxAddressTaken() throws Exception
    {
        final User otherUser = new User("Other3", "User", "other3@xcmailr.test", "hash", "en");
        otherUser.setActive(true);
        otherUser.setApiToken("other3-token-" + System.nanoTime());
        userRepository.save(otherUser);

        final MBox otherBox = new MBox("existingtaken", "xcmailr.test", System.currentTimeMillis() + 60000, false, otherUser);
        mailboxRepository.save(otherBox);

        final MBox myBox = new MBox("myupdatebox", "xcmailr.test", System.currentTimeMillis() + 60000, false, testUser);
        mailboxRepository.save(myBox);

        final MailboxData update = new MailboxData();
        update.address = "existingtaken@xcmailr.test";
        update.deactivationTime = System.currentTimeMillis() + 120000;
        update.forwardEnabled = true;

        mockMvc.perform(put(API_PREFIX + "/myupdatebox@xcmailr.test")
               .header("Authorization", "Bearer " + token)
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(update)))
               .andExpect(status().isForbidden())
               .andExpect(jsonPath("$.errors[0].parameter", is("mailboxAddress")));
    }

    @Test
    @DisplayName("deleteMailbox successfully deletes mailbox and returns 204 No Content")
    public void testDeleteMailboxSuccess() throws Exception
    {
        final MBox box = new MBox("deleteme", "xcmailr.test", System.currentTimeMillis() + 60000, false, testUser);
        mailboxRepository.save(box);

        mockMvc.perform(delete(API_PREFIX + "/deleteme@xcmailr.test")
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isNoContent());

        assertFalse(mailboxRepository.existsByAddressIgnoreCaseAndDomainIgnoreCase("deleteme", "xcmailr.test"));
    }

    @Test
    @DisplayName("deleteMailbox on non-existent mailbox returns 404 Not Found")
    public void testDeleteMailboxNotFound() throws Exception
    {
        mockMvc.perform(delete(API_PREFIX + "/nonexistent@xcmailr.test")
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("deleteMailbox on other user's mailbox returns 403 Forbidden")
    public void testDeleteMailboxOtherUser() throws Exception
    {
        final User otherUser = new User("Other4", "User", "other4@xcmailr.test", "hash", "en");
        otherUser.setActive(true);
        otherUser.setApiToken("other4-token-" + System.nanoTime());
        userRepository.save(otherUser);

        final MBox otherBox = new MBox("otherdelete", "xcmailr.test", System.currentTimeMillis() + 60000, false, otherUser);
        mailboxRepository.save(otherBox);

        mockMvc.perform(delete(API_PREFIX + "/otherdelete@xcmailr.test")
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isForbidden())
               .andExpect(jsonPath("$.errors[0].parameter", is("mailboxAddress")));
    }
}
