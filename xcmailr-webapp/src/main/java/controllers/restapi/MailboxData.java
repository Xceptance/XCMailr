package controllers.restapi;

import org.hibernate.validator.constraints.Email;

import models.MBox;

/**
 * The data object that represents the details of a mailbox.
 */
public class MailboxData
{
    public long id;

    @Email
    public String email;

    public long expirationDate;

    public boolean forwardEnabled;

    public MailboxData()
    {
    }

    public MailboxData(MBox mailbox)
    {
        id = mailbox.getId();
        email = mailbox.getFullAddress();
        expirationDate = mailbox.getTs_Active();
        forwardEnabled = mailbox.isForwardEmails();
    }
}
