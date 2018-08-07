package controllers;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.joda.time.DateTime;
import org.slf4j.Logger;

import com.google.inject.Inject;

import conf.XCMailrConf;
import models.MBox;
import models.MailTransaction;

public class ExpirationService implements Runnable
{

    @Inject
    Logger log;

    @Inject
    XCMailrConf xcmConfiguration;

    private ConcurrentLinkedQueue<MailTransaction> mtxQueue;

    private boolean deleteTransactions;

    public ExpirationService(ConcurrentLinkedQueue<MailTransaction> mtxQueue, boolean deleteTransactions)
    {
        this.mtxQueue = mtxQueue;
        this.deleteTransactions = deleteTransactions;
    }

    @Override
    public void run()
    {
        log.debug("Emailaddress Expiration Task run");

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
                mailBox.enable();
                log.debug("now expired: " + mailBox.getFullAddress());
            }
        }

        // add the new Mailtransactions
        List<MailTransaction> mtxToSave = new LinkedList<MailTransaction>();

        // add all transactions from the queue to a list

        MailTransaction mt;
        while ((mt = mtxQueue.poll()) != null)
        {
            // mails will be silently dropped if there is no forward address but target address domain is
            // configured to be valid / handled
            if (mt.getStatus() == 100)
            {
                mt.getSourceaddr();
                mt.getTargetaddr();
            }
            else
            {
                mtxToSave.add(mt);
            }
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

}
