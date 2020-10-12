package services;

import java.util.HashMap;
import java.util.HashSet;

import org.slf4j.Logger;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.PagingList;
import com.google.inject.Inject;
import com.google.inject.Singleton;

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
        final PagingList<User> users = Ebean.find(User.class).select("id, mail").findPagingList(1_000);
        for (final User user : users.getAsList())
        {
            final String userMailLC = user.getMail().toLowerCase();
            if (mailAddresses.contains(userMailLC))
            {
                idToMailAddress.put(user.getId(), user.getMail());
            }

            mailAddresses.add(userMailLC);
        }

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
        final PagingList<MBox> boxes = Ebean.find(MBox.class).select("id, address, domain").findPagingList(1_000);
        for (final MBox box : boxes.getAsList())
        {
            final String boxAddressLC = box.getFullAddress().toLowerCase();
            if (mailAddresses.contains(boxAddressLC))
            {
                idToMailAddress.put(box.getId(), box.getFullAddress());
            }

            mailAddresses.add(boxAddressLC);

        }

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
