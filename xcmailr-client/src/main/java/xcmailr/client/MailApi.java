/*
 * Copyright (c) 2020 Xceptance Software Technologies GmbH
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
package xcmailr.client;

import java.io.InputStream;
import java.util.List;

/**
 * Operations to manage mails in a remote XCMailr instance.
 * 
 * @see XCMailrClient
 */
public interface MailApi
{
    /**
     * Lists the details of all mails in a mailbox identified by the given address.
     * 
     * @param mailboxAddress
     *            the mailbox address, such as "john.doe@xcmailr.test"
     * @param options
     *            the filter options (may be <code>null</code>)
     * @return the list of mails that match the filter options
     * @throws Exception
     *             if anything went wrong
     */
    public List<Mail> listMails(String mailboxAddress, MailFilterOptions options) throws Exception;

    /**
     * Returns the details of the mail with the given ID.
     * 
     * @param mailId
     *            the ID of the mail
     * @return the details of the mail
     * @throws Exception
     *             if anything went wrong
     */
    public Mail getMail(long mailId) throws Exception;

    /**
     * Opens a stream to download the named attachment of a certain mail.
     * 
     * @param mailId
     *            the ID of the mail
     * @param attachmentName
     *            the (file) name of the attachment
     * @return a stream from which the attachment can be read
     * @throws Exception
     *             if anything went wrong
     */
    public InputStream openAttachment(long mailId, String attachmentName) throws Exception;

    /**
     * Deletes a mail from XCMailr.
     * 
     * @param mailId
     *            the ID of the mail
     * @throws Exception
     *             if anything went wrong
     */
    public void deleteMail(long mailId) throws Exception;
}
