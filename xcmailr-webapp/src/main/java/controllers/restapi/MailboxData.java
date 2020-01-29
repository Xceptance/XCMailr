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

    public MailboxData(MBox mailbox)
    {
        address = mailbox.getFullAddress();
        deactivationTime = mailbox.getTs_Active();
        forwardEnabled = mailbox.isForwardEmails();
    }
}
