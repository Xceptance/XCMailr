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
package services;

import java.util.HashMap;
import java.util.HashSet;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.ebean.Ebean;
import models.MBox;
import models.User;
import ninja.lifecycle.Start;

/**
 * Simple one-shot job that checks the configured DB for email address duplicates.
 *
 * @author Hartmut Arlt (Xceptance Software Technologies GmbH)
 */
@Singleton
public class CheckDBForMailAddressDuplicates
{
    @Inject
    Logger log;

    @Start(order = 20)
    public void performConsistencyCheck() throws Throwable
    {
        validateUserAddresses();
        validateMailboxAddresses();

        log.info("Check for mail address duplicates successfully completed");
    }

    private void validateUserAddresses() throws Throwable
    {
        final HashSet<String> mailAddresses = new HashSet<>();
        final HashMap<Long, String> idToMailAddress = new HashMap<>();

        Ebean.find(User.class).select("id, mail").findEach(user -> {
            final String userMailLC = user.getMail().toLowerCase();
            if (mailAddresses.contains(userMailLC))
            {
                idToMailAddress.put(user.getId(), user.getMail());
            }

            mailAddresses.add(userMailLC);
        });

        if (!idToMailAddress.isEmpty())
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("The mail address of the following user(s) is already taken by another user:");
            idToMailAddress.entrySet().forEach(e -> {
                sb.append("\n    ID: ").append(e.getKey()).append("    Mail-Address: ").append(e.getValue());
            });

            log.error(sb.toString());

            throw new RuntimeException("Found at least two users in DB who share the same email address");
        }
    }

    private void validateMailboxAddresses() throws Throwable
    {
        final HashSet<String> mailAddresses = new HashSet<>();
        final HashMap<Long, String> idToMailAddress = new HashMap<>();

        Ebean.find(MBox.class).select("id, address, domain").findEach(mailBox -> {
            final String boxAddressLC = mailBox.getFullAddress().toLowerCase();
            if (mailAddresses.contains(boxAddressLC))
            {
                idToMailAddress.put(mailBox.getId(), mailBox.getFullAddress());
            }

            mailAddresses.add(boxAddressLC);
        });

        if (!idToMailAddress.isEmpty())
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("The mail address of the following mailboxe(s) is already owned by another mailbox:");
            idToMailAddress.entrySet().forEach(e -> {
                sb.append("\nID: ").append(e.getKey()).append("    Mail-Address: ").append(e.getValue());
            });
            log.error(sb.toString());

            throw new RuntimeException("Found at least two mailboxes in DB that share the same email address");

        }
    }
}
