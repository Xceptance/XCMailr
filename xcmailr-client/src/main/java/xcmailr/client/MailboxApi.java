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

import java.util.List;

/**
 * Operations to manage mailboxes in a remote XCMailr instance.
 * 
 * @see XCMailrClient
 */
public interface MailboxApi
{
    /**
     * Lists the details of all mailboxes that belong to the current user.
     * 
     * @return the list of mailboxes
     * @throws Exception
     *             if anything went wrong
     */
    public List<Mailbox> listMailboxes() throws Exception;

    /**
     * Creates a new mailbox at XCMailr.
     * 
     * @param address
     *            the address of the mailbox
     * @param minutesActive
     *            the time (in minutes) from now when the mailbox will automatically be deactivated (not deleted!)
     * @param forwardEnabled
     *            whether XCMailr should automatically forward mails put into the new mailbox to the mailbox owner
     * @return the mailbox details returned from the server
     * @throws Exception
     *             if anything went wrong
     */
    public Mailbox createMailbox(final String address, final int minutesActive, final boolean forwardEnabled) throws Exception;

    /**
     * Creates a new mailbox at XCMailr.
     * 
     * @param mailbox
     *            the details of the new mailbox
     * @return the mailbox details as returned from the server
     * @throws Exception
     *             if anything went wrong
     */
    public Mailbox createMailbox(final Mailbox mailbox) throws Exception;

    /**
     * Returns the details of the mailbox with the given address.
     * 
     * @param address
     *            the address of the mailbox to look up
     * @return the mailbox details as returned from the server
     * @throws Exception
     *             if anything went wrong
     */
    public Mailbox getMailbox(final String address) throws Exception;

    /**
     * Updates the mailbox with the given address.
     * 
     * @param address
     *            the address of the mailbox to update
     * @param newAddress
     *            the new address of the mailbox
     * @param minutesActive
     *            the new address of the mailbox
     * @param forwardEnabled
     *            the new address of the mailbox
     * @return the updated mailbox details as returned from the server
     * @throws Exception
     *             if anything went wrong
     */
    public Mailbox updateMailbox(final String address, final String newAddress, final int minutesActive, final boolean forwardEnabled)
        throws Exception;

    /**
     * Updates the mailbox with the given address.
     * 
     * @param address
     *            the address of the mailbox to update
     * @param mailbox
     *            the details of the new mailbox
     * @return the updated mailbox details as returned from the server
     * @throws Exception
     *             if anything went wrong
     */
    public Mailbox updateMailbox(final String address, final Mailbox mailbox) throws Exception;

    /**
     * Deletes the mailbox with the given address.
     * 
     * @param address
     *            the address of the mailbox to delete
     * @throws Exception
     *             if anything went wrong
     */
    public void deleteMailbox(final String address) throws Exception;
}
