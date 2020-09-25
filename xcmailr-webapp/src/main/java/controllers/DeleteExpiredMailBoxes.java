package controllers;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.google.inject.Singleton;

import models.MBox;
import ninja.scheduler.Schedule;

/**
 * Simple job that runs on a regular basis and removes at most 100,000 expired mail boxes that have their
 * {@code autoRemove} flag set to {@code true}.
 *
 * @author Hartmut Arlt (Xceptance Software Technologies GmbH)
 */
@Singleton
public class DeleteExpiredMailBoxes
{
    private static final Logger LOG = LoggerFactory.getLogger(DeleteExpiredMailBoxes.class);

    private static final int MAX_ROUNDS = 100;

    @Schedule(initialDelay = 5, delay = 30, timeUnit = TimeUnit.MINUTES)
    public void deleteExpired()
    {
        LOG.debug("Cleanup Mailboxes");

        final Query<MBox> q = Ebean.find(MBox.class).where("auto_remove = true and expired = true");
        int rounds = 0;
        // Run at least once
        do
        {
            final List<Object> ids = q.setMaxRows(1000).findIds();
            // no such mailbox found -> nothing to done
            if (ids.isEmpty())
            {
                break;
            }

            Ebean.delete(MBox.class, ids);
            LOG.info("Removed {} expired mailboxes from database", ids.size());
        }
        while (++rounds < MAX_ROUNDS);

        LOG.debug("Finished Mailbox cleanup");

    }
}
