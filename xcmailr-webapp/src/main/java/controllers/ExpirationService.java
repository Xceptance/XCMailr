package controllers;

import java.sql.Date;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.Ebean;

import conf.XCMailrConf;
import etc.StatisticsEntry;
import models.MBox;
import models.Mail;
import models.MailStatistics;
import models.MailStatisticsKey;
import models.MailTransaction;

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

    // stores statistic data about emails that have been received that won't be forwarded
    HashMap<MailStatisticsKey, StatisticsEntry> mailStatisticsCache = new HashMap<>();

    @Override
    public void run()
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
                log.debug("now expired: " + mailBox.getFullAddress());
            }
        }

        // delete expired mails
        long olderThan10Minutes = System.currentTimeMillis() - (10 * 60 * 1000);
        List<Mail> findList = Ebean.find(Mail.class).where().lt("recieveTime", olderThan10Minutes).findList();
        findList.forEach((mail) -> {
            mail.delete();
        });

        // add the new Mailtransactions
        List<MailTransaction> mtxToSave = new LinkedList<MailTransaction>();

        // add all transactions from the queue to a list
        MailTransaction mt;
        log.info("Start processing mail transaction queue, length: " + mtxQueue.size());
        while ((mt = mtxQueue.poll()) != null)
        {
            // mails will be silently dropped if there is no forward address but target address domain is
            // configured to be valid / handled
            if (mt.getStatus() == 100)
            {
                MailStatisticsKey mailStatisticsKey = createMailStatisticsKey(mt);
                if (!mailStatisticsCache.containsKey(mailStatisticsKey))
                {
                    mailStatisticsCache.put(mailStatisticsKey, new StatisticsEntry());
                }
                mailStatisticsCache.get(mailStatisticsKey).incrementDropCount();
            }
            else if (mt.getStatus() == 300)
            {
                // forwarded mail
                MailStatisticsKey mailStatisticsKey = createMailStatisticsKey(mt);

                if (!mailStatisticsCache.containsKey(mailStatisticsKey))
                {
                    mailStatisticsCache.put(mailStatisticsKey, new StatisticsEntry());
                }
                mailStatisticsCache.get(mailStatisticsKey).incrementForwardCount();
            }
            else
            {
                mtxToSave.add(mt);
            }
        }
        log.info("Finished processing mail transaction queue, length: " + mtxQueue.size());

        try
        {
            log.info("Write mail statistics to DB, length: " + mailStatisticsCache.size());

            for (Entry<MailStatisticsKey, StatisticsEntry> statisticEntry : mailStatisticsCache.entrySet())
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
                    // there is no entry, we need to create a new one
                    MailStatistics mailStatisticEntry = new MailStatistics();
                    mailStatisticEntry.setId((long) 0);
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
                    entry.setDropCount(entry.getDropCount() + additionalMailForwardCount);
                    Ebean.update(entry);
                }
            }
            mailStatisticsCache.clear();
            log.info("Finished writing mail statistics to DB");
        }
        catch (Exception e)
        {
            log.error("Error while persisting mail statistics", e);
        }

        // and save all entries of this list in one transaction to the list
        MailTransaction.saveMultipleTx(mtxToSave);
        log.info("stored " + mtxToSave.size() + " entries in the database");

        // remove old MailTransactions
        if (deleteTransactions)
        { // execute only if a value has been set
            log.debug("Cleanup Mailtransaction-list");
            long removalTS = dt.minusHours(xcmConfiguration.MTX_MAX_AGE).getMillis();

            MailTransaction.deleteTxInPeriod(removalTS);
            log.debug("finished Mailtransaction cleanup");
        }

    }

    private MailStatisticsKey createMailStatisticsKey(MailTransaction mt)
    {
        String sourceDomain = getDomainOfEmail(mt.getSourceaddr());
        String targetDomain = getDomainOfEmail(mt.getRelayaddr());
        Date mailDate = new Date(mt.getTs());
        int quarterHourOfDay = getQuarterHour(mt.getTs());

        MailStatisticsKey mailStatisticsKey = new MailStatisticsKey(mailDate, quarterHourOfDay, sourceDomain,
                                                                    targetDomain);
        return mailStatisticsKey;
    }

    private String getDomainOfEmail(String email)
    {
        if (email == null || email.trim().length() == 0)
        {
            return null;
        }

        String[] split = email.split("\\@");
        if (split.length != 2)
        {
            return null;
        }

        return split[1];
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
