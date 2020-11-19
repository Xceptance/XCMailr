package services;

import java.sql.Date;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.Ebean;

import conf.XCMailrConf;
import etc.HelperUtils;
import etc.StatisticsEntry;
import models.MBox;
import models.Mail;
import models.MailStatistics;
import models.MailStatisticsKey;
import models.MailTransaction;
import models.User;

public class ExpirationService implements Runnable
{
    Logger log = LoggerFactory.getLogger(ExpirationService.class);

    private ConcurrentLinkedQueue<MailTransaction> mtxQueue;

    private boolean deleteTransactions;

    private XCMailrConf xcmConfiguration;

    public ExpirationService(ConcurrentLinkedQueue<MailTransaction> mtxQueue, boolean deleteTransactions,
        XCMailrConf xcmConfiguration)
    {
        log.info("ExpirationService initialized");
        this.xcmConfiguration = xcmConfiguration;
        this.mtxQueue = mtxQueue;
        this.deleteTransactions = deleteTransactions;
    }

    @Override
    public void run()
    {
        try
        {
            doRun();
        }
        catch (Exception ex)
        {
            log.error("Exception while running expiration service", ex);
        }
    }

    private void doRun()
    {
        log.info("Emailaddress Expiration Task run");

        // get the number of MBox-Elements that will expire in the next "MB_INT"-minutes
        List<MBox> expiringMailBoxesList = MBox.getNextBoxes(xcmConfiguration.MB_INTERVAL);
        ListIterator<MBox> mailBoxIterator = expiringMailBoxesList.listIterator();

        DateTime dt = new DateTime();
        MBox mailBox;
        // disable expired mail-addresses
        while (mailBoxIterator.hasNext())
        {
            mailBox = mailBoxIterator.next();
            if (dt.isAfter(mailBox.getTs_Active()) && (mailBox.getTs_Active() != 0))
            { // this element is now expired
                mailBox.disable();
                log.debug("Mailbox '{}' expired", mailBox.getFullAddress());
            }
        }

        // delete expired mails
        long olderThanRetentionPolicy = System.currentTimeMillis()
                                        - (xcmConfiguration.MAIL_RETENTION_PERIOD * 60 * 1000);
        List<Mail> findList = Ebean.find(Mail.class).where().lt("receiveTime", olderThanRetentionPolicy).findList();

        if (findList.size() > 0)
        {
            findList.forEach((mail) -> {
                mail.delete();
            });
            log.info("Removed {} expired mails", findList.size());
        }

        // set token expiration
        Calendar tokenExpiration = Calendar.getInstance();
        tokenExpiration.add(Calendar.DAY_OF_MONTH, -1 * xcmConfiguration.APITOKEN_EXPIRATION);

        // delete expired API token
        List<User> expiredUserToken = Ebean.find(User.class).where() //
                                           .between("API_TOKEN_CREATION_TIMESTAMP", 1,
                                                    tokenExpiration.getTimeInMillis())
                                           .findList();

        expiredUserToken.forEach((user) -> {
            user.setApiToken(null);
            user.setApiTokenCreationTimestamp(0);
            user.save();
            log.info("User API token expired for '{}'", user.getMail());
        });

        // add the new Mailtransactions
        final List<MailTransaction> mtxToSave = new LinkedList<MailTransaction>();

        final Map<MailStatisticsKey, StatisticsEntry> statistics = new HashMap<>();

        // add all transactions from the queue to a list
        MailTransaction mt;
        log.info("Start processing mail transaction queue [size: {}]", mtxQueue.size());
        while ((mt = mtxQueue.poll()) != null)
        {
            final int status = mt.getStatus();
            // mails will be silently dropped if there is no forward address but target address domain is
            // configured to be valid / handled
            if (status == 100 || status == 300)
            {
                final MailStatisticsKey mailStatisticsKey = createMailStatisticsKey(mt);
                if (mailStatisticsKey != null)
                {
                    StatisticsEntry mailStats = statistics.get(mailStatisticsKey);
                    if (mailStats == null)
                    {
                        mailStats = new StatisticsEntry();
                        statistics.put(mailStatisticsKey, mailStats);
                    }

                    if (status == 100)
                    {
                        mailStats.incrementDropCount();
                    }
                    else if (status == 300)
                    {
                        mailStats.incrementForwardCount();
                    }
                }
            }
            else
            {
                mtxToSave.add(mt);
            }
        }
        log.info("Finished processing mail transaction queue [size: {}]", mtxQueue.size());

        try
        {
            log.info("Write mail statistics to DB [size: {}]", statistics.size());

            for (Entry<MailStatisticsKey, StatisticsEntry> statisticEntry : statistics.entrySet())
            {
                MailStatisticsKey mailStatisticsKey = statisticEntry.getKey();
                int additionalMailDropCount = statisticEntry.getValue().getDropCount();
                int additionalMailForwardCount = statisticEntry.getValue().getForwardCount();

                MailStatistics entry = Ebean.find(MailStatistics.class).where() //
                                            .eq("DATE", mailStatisticsKey.getDate()) //
                                            .eq("QUARTER_HOUR", mailStatisticsKey.getQuarterHour()) //
                                            .eq("FROM_DOMAIN", mailStatisticsKey.getFromDomain()) //
                                            .eq("TARGET_DOMAIN", mailStatisticsKey.getTargetDomain()) //
                                            .findUnique();

                if (entry == null)
                {
                    // there is no entry, we need to create a new ones
                    MailStatistics mailStatisticEntry = new MailStatistics();
                    mailStatisticEntry.setKey(mailStatisticsKey);
                    mailStatisticEntry.setDropCount(additionalMailDropCount);
                    mailStatisticEntry.setForwardCount(additionalMailForwardCount);

                    try
                    {
                        Ebean.save(mailStatisticEntry);
                    }
                    catch (Exception e)
                    {
                        log.error("Couldn't create new message statistics entry: " + statisticEntry.toString(), e);
                    }
                }
                else
                {
                    entry.setDropCount(entry.getDropCount() + additionalMailDropCount);
                    entry.setForwardCount(entry.getForwardCount() + additionalMailForwardCount);
                    Ebean.update(entry);
                }
            }
            log.info("Finished writing mail statistics to DB");
        }
        catch (Exception e)
        {
            log.error("Error while persisting mail statistics", e);
        }

        // and save all entries of this list in one transaction to the list
        MailTransaction.saveMultipleTx(mtxToSave);
        log.info("Stored {} entries in the database", mtxToSave.size());

        // remove old MailTransactions
        if (deleteTransactions)
        { // execute only if a value has been set
            log.debug("Cleanup Mailtransaction-list");
            long removalTS = dt.minusHours(xcmConfiguration.MTX_MAX_AGE).getMillis();

            MailTransaction.deleteTxInPeriod(removalTS);
            log.debug("Finished Mailtransaction cleanup");
        }

        // remove old MailStatistics entries
        deleteExpiredMailStatistics(xcmConfiguration.MAIL_STATISTICS_MAX_DAYS);
    }

    /**
     * Deletes all {@link MailStatistics} database entries that are older than the given number of days.
     * 
     * @param days
     *            the days
     */
    private void deleteExpiredMailStatistics(int days)
    {
        log.debug("Delete MailStatistics entries older than {} days", days);

        Date date = new Date(new DateTime().minusDays(days).getMillis());
        int deletedCount = MailStatistics.deleteAllOlderThan(date);
        
        log.debug("Finished MailStatistics cleanup ({} entries deleted)", deletedCount);
    }

    private MailStatisticsKey createMailStatisticsKey(MailTransaction mt)
    {
        final String targetDomain = getDomainOfEmail(mt.getRelayaddr());
        final String sourceDomain = getDomainOfEmail(mt.getSourceaddr());
        // neither 'targetDomain' nor 'sourceDomain' must be null
        if (targetDomain == null || sourceDomain == null)
        {
            return null;
        }

        final Date mailDate = new Date(mt.getTs());
        final int quarterHourOfDay = getQuarterHour(mt.getTs());

        return new MailStatisticsKey(mailDate, quarterHourOfDay, sourceDomain, targetDomain);

    }

    private String getDomainOfEmail(String email)
    {
        final String[] parts = HelperUtils.splitMailAddress(email);
        if (parts != null && parts.length > 1)
        {
            return parts[1];
        }

        return null;
    }

    private int getQuarterHour(long timestamp)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);

        int currentQuarterHour = calendar.get(Calendar.HOUR_OF_DAY) * 4;
        currentQuarterHour += (calendar.get(Calendar.MINUTE) / 15);

        return currentQuarterHour;
    }

}
