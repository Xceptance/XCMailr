package controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.google.inject.Inject;

import conf.XCMailrConf;
import models.MBox;
import models.MailTransaction;
import models.User;
import ninja.NinjaTest;
import ninja.utils.NinjaMode;
import ninja.utils.NinjaProperties;
import ninja.utils.NinjaPropertiesImpl;

/**
 * Should test the delivery process of the MessageListener
 * 
 * @author Patrick Thum
 */
public class MessageListenerDeliverTest extends NinjaTest
{
    /**
     * Test the different preconditions that may stop the message delivery, such as:
     * <ul>
     * <li>malformed address</li>
     * <li>not existing mbox</li>
     * <li>disabled mbox</li>
     * <li>disabled user</li>
     * </ul>
     */
    @Test
    public void testDeliveryPreconditions()
    {
        final NinjaProperties ninjaProperties = spy(new NinjaPropertiesImpl(NinjaMode.test));
        final XCMailrConf xcmConf = new XCMailrConf(ninjaProperties);

        final String local = RandomStringUtils.randomAlphabetic(10).toLowerCase();
        final String domain = xcmConf.DOMAIN_LIST[0];
        final String testAddress = local + "@" + domain;

        final MessageListener ml = new MessageListener();
        ml.xcmConfiguration = xcmConf;
        ml.jobController = new JobController();
        final ConcurrentLinkedQueue<MailTransaction> clq = ml.jobController.mtxQueue;

        // create an user
        final String usrLocalMailPart = RandomStringUtils.randomAlphabetic(10);
        final User user = new User("John", "Doe", usrLocalMailPart + "@ccmailr.test", "1234", "en");
        user.setActive(true);
        user.save();

        // check malformed mail address
        MBox result = ml.doMboxPreconditionChecks(local, local);
        assertNull(result);
        List<MailTransaction> mtxs = MailTransaction.getForTarget(local);
        assertEquals(1, mtxs.size());
        assertEquals(0, mtxs.get(0).getStatus());

        // check unexisting mbox
        result = ml.doMboxPreconditionChecks(local, testAddress);
        assertNull(result);
        assertEquals(1, clq.size());
        assertEquals(100, clq.poll().getStatus());

        // create an expired mbox
        MBox mbx = new MBox(local, domain, 0, true, user);
        mbx.save();

        // check expired mbox
        result = ml.doMboxPreconditionChecks(local, testAddress);
        assertNull(result);
        assertEquals(1, clq.size());
        assertEquals(200, clq.poll().getStatus());
        assertEquals(1, MBox.getById(mbx.getId()).getSuppressions());

        // enable mbox, disable user
        mbx.setExpired(false);
        mbx.update();
        user.setActive(false);
        user.update();

        // check with disabled user
        result = ml.doMboxPreconditionChecks(local, testAddress);
        assertNull(result);
        assertEquals(1, clq.size());
        assertEquals(600, clq.poll().getStatus());
        assertEquals(2, MBox.getById(mbx.getId()).getSuppressions());

        // enable user, check for success
        user.setActive(true);
        user.update();
        result = ml.doMboxPreconditionChecks(local, testAddress);
        assertEquals(result.getUsr(), user);
    }
}
