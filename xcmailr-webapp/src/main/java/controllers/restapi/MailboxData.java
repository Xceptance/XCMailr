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

import org.hibernate.validator.constraints.Email;

import models.MBox;

/**
 * The data object that represents the details of a mailbox.
 */
public class MailboxData
{
    @Email
    public String address;

    public long deactivationTime;

    public boolean forwardEnabled;

    public MailboxData()
    {
    }

    public MailboxData(final MBox mailbox)
    {
        address = mailbox.getFullAddress();
        deactivationTime = mailbox.getTs_Active();
        forwardEnabled = mailbox.isForwardEmails();
    }
}
