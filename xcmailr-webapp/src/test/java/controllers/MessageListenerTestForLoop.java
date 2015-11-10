package controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import controllers.MessageListener;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Should test the way the MessageListener Class checks for email loops. It does not use a server and it doesn't check
 * what happens if a loop was detected. Includes tests for the Return-Path Header, a custom Header, the References and
 * the In-Reply-To header. Also includes an example out-of-office mail and an example bounce mail. The mails used are
 * stored in a subdirectory. <br>
 * 
 * @author daniel
 */
public class MessageListenerTestForLoop
{
    private static String loopHeader;

    private static String loopHeaderContent;

    private final static String recipient = "recipient.recipient@recipient.recipient";

    static MessageListener obj = new MessageListener();

    static Class cls = obj.getClass();

    static Method checkLoop;

    /**
     * Make the checkForLoop method of MessageListener.java accessible so the tests can test it. Also set the fields for
     * the custom headers name and content inside the MessageListener object, so it has valid values to check. <br>
     * 
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @BeforeClass
    public static void makeMethodAccessible()
        throws NoSuchMethodException, SecurityException, NoSuchFieldException, IllegalArgumentException,
        IllegalAccessException
    {
        // make the private method checkForLoop accessible for this test
        checkLoop = cls.getDeclaredMethod("checkForLoop", MimeMessage.class);
        checkLoop.setAccessible(true);

        // Set loop header constants and it's content.
        // This code was copied from MessageListener.java on the 10. October 2015
        loopHeader = "X-Loop";
        loopHeaderContent = "loopbreaker" + recipient;

        // make the loopHeader and loopHeaderContent fields accessible and set them
        Field loopHeaderField = cls.getDeclaredField("loopHeader");
        loopHeaderField.setAccessible(true);
        Field loopHeaderContentField = cls.getDeclaredField("loopHeaderContent");
        loopHeaderContentField.setAccessible(true);
        loopHeaderField.set(obj, loopHeader);
        loopHeaderContentField.set(obj, loopHeaderContent);
    }

    /**
     * Test if a loop is detected if the Return-Path header is empty, and if no loop is detected if it contains a
     * message id. <br>
     * 
     * @throws MessagingException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws FileNotFoundException
     */
    @Test
    public void testReturnPath()
        throws MessagingException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
        FileNotFoundException
    {
        File mailFile = new File("./src/test/java/controllers/exampleMails/ReturnPath.eml");
        InputStream inputStream = new FileInputStream(mailFile);
        Properties properties = new Properties();
        Session session = Session.getInstance(properties);
        MimeMessage mail = new MimeMessage(session, inputStream);

        String expectedMessage = "Return-Path is empty";

        // test if a loop is detected if the Return-Path is empty
        // It should
        String result = (String) checkLoop.invoke(obj, mail);
        Assert.assertNotEquals("A possible loop went undetected", null, result);
        Assert.assertEquals(expectedMessage, result);

        // tests if a loop is detected with a normal Return-Path
        // it shouldn't
        mail.setHeader("Return-Path", "<23avjkhsb25s@gmail.com>");
        result = (String) checkLoop.invoke(obj, mail);
        Assert.assertEquals("A loop was detected when none was there", null, result);
    }

    /**
     * Tests the custom header. Tests if a loop is detected ... 
     * ... if the mail doesn't have the custom header
     * (expected: No) <br>
     * ... if the mail has the custom header with the correct content (expected: Yes) <br>
     * ... if the mail has the custom header, but the content is wrong (expected: No) <br>
     * ... if the mail doesn't have the custom header and the content is elsewhere (expected: No) <br>
     * ... if the mail has multiple instances of the custom header and only one of
     * them has the correct content (expected: yes) <br>
     * ... if the correct custom header and he correct content are somewhere in the content part of the mail
     * (expected: No) <br>
     * 
     * @throws FileNotFoundException
     * @throws MessagingException
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    @Test
    public void testCustomHeader()
        throws FileNotFoundException, MessagingException, NoSuchFieldException, SecurityException,
        IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        File mailFile = new File("./src/test/java/controllers/exampleMails/Custom-Header.eml");
        InputStream inputStream = new FileInputStream(mailFile);
        Properties properties = new Properties();
        Session session = Session.getInstance(properties);
        MimeMessage mail = new MimeMessage(session, inputStream);
        String expectedMessage = "X-Loop header with this email adress present";

        // tests if a mail without the custom header goes through
        // It should
        mail.removeHeader(loopHeader);
        String result = (String) checkLoop.invoke(obj, mail);
        Assert.assertEquals("A loop was detected when none was there", null, result);

        // test if a loop is detected if the a different header has the same content
        mail.addHeader("X-BreakLoop", loopHeaderContent);
        result = (String) checkLoop.invoke(obj, mail);
        Assert.assertEquals("A loop was detected when none was there", null, result);
        mail.removeHeader("X-BreakLoop");

        // Tests if the correct custom header with the wrong content goes through.
        // It should
        mail.addHeader(loopHeader, recipient);
        result = (String) checkLoop.invoke(obj, mail);
        Assert.assertEquals("A loop was detected when none was there", null, result);

        // Tests if the message goes through (= no loop is detected) with the correct custom header and
        // the correct custom header content
        // it should
        mail.addHeader(loopHeader, loopHeaderContent);
        result = (String) checkLoop.invoke(obj, mail);
        Assert.assertNotEquals("A possible loop went undetected", null, result);
        Assert.assertEquals(expectedMessage, result);

        // Tests if the message goes through (= no loop is detected) if there are three instances
        // of the correct header and only the second has the correct content
        // It shoudn't
        mail.setHeader(loopHeader, "Wrong Content");
        mail.addHeader(loopHeader, loopHeaderContent);
        mail.addHeader(loopHeader, "Wrong Content");
        result = (String) checkLoop.invoke(obj, mail);
        Assert.assertNotEquals("A possible loop went undetected", null, result);
        Assert.assertEquals(expectedMessage, result);
        
        // tests if the message goes through if both the custom header and it's content are somewhere in the 
        // content part of the message, not the header
        // it should
        mail.removeHeader(loopHeader);
        mail.setText(loopHeader + ": " + loopHeaderContent);
        result = (String) checkLoop.invoke(obj, mail);
        Assert.assertEquals("A loop was detected when none was there", null, result);
    }

    /**
     * Tests if the checkForLoop method detects a loop if the "@domain" part of one of the message ids in the References
     * Header is identical to the "@domain" part in the message id of this very message. <br>
     * 
     * Also checks if the checkForLoop method detects a loop if the References header isn't present (no), if it contains
     * some other message id (no) and if there are multiple Reference headers and only one of them contains the same 
     * @domain part (yes). <br>
     *  
     * @throws FileNotFoundException
     * @throws MessagingException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    @Test
    public void testReferencesHeader()
        throws FileNotFoundException, MessagingException, IllegalAccessException, IllegalArgumentException,
        InvocationTargetException
    {
        File mailFile = new File("./src/test/java/controllers/exampleMails/Reference.eml");
        InputStream inputStream = new FileInputStream(mailFile);
        Properties properties = new Properties();
        Session session = Session.getInstance(properties);
        MimeMessage mail = new MimeMessage(session, inputStream);

        String id = mail.getMessageID();
        String[] splitString = id.split("@");
        String domain = splitString[1];
        domain = domain.toLowerCase();

        String expectedMessage = "References field references the domain of this email " + "adress: " + domain;

        // tests if it detects a loop if no Reference Header is there. It shoudn't
        mail.removeHeader("Reference");
        String result = (String) checkLoop.invoke(obj, mail);
        Assert.assertEquals("A loop was detected when none was there", null, result);

        // tests if it detects a loop if only a single message id is in the References Header and
        // that message id has the same domain as the id of the mail.
        // It should.
        mail.addHeader("References", "<2345j.a2s3dfgh8j98kmail@" + domain);
        result = (String) checkLoop.invoke(obj, mail);
        Assert.assertNotEquals("A possible loop went undetected", null, result);
        Assert.assertEquals(expectedMessage, result);

        // tests if it detects a loop if the Reference Header contains an unrelated message id.
        // It shoudn't
        mail.setHeader("References", "<2345r6.mailx4cvb@mail.xcmailr.com>");
        result = (String) checkLoop.invoke(obj, mail);
        Assert.assertEquals("A loop was detected when none was there", null, result);

        // tests if it detects a loop if the Reference Header contains two message ids and the first
        // of them has the same domain as the id of the mail while the second does not.
        // It should.
        mail.setHeader("References", "<2345r6.mailx4cvb@" + domain + "   " + "<2345r6.mailxjavamail@021465>");
        result = (String) checkLoop.invoke(obj, mail);
        Assert.assertNotEquals("A possible loop went undetected", null, result);
        Assert.assertEquals(expectedMessage, result);

        // tests if it detects a loop if the Reference Header contains two message ids and the second
        // of them has the same domain as the id of the mail while the first does not.
        // It should.
        mail.setHeader("References", "<2345r6.mailxjavamail@021465>" + "   " + "<2345r6.mailx4cvb@" + domain);
        result = (String) checkLoop.invoke(obj, mail);
        Assert.assertNotEquals("A possible loop went undetected", null, result);
        Assert.assertEquals(expectedMessage, result);
    }

    /**
     * Tests if a loop is detected if there is no In-Reply-To Header (no), if there is one and it contains an unrelated 
     * message id (no) and if there is one and it contains the same @domain part (yes).
     * 
     * @throws FileNotFoundException
     * @throws MessagingException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    @Test
    public void testInReplyToHeader()
        throws FileNotFoundException, MessagingException, IllegalAccessException, IllegalArgumentException,
        InvocationTargetException
    {
        File mailFile = new File("./src/test/java/controllers/exampleMails/In-Reply-To.eml");
        InputStream inputStream = new FileInputStream(mailFile);
        Properties properties = new Properties();
        Session session = Session.getInstance(properties);
        MimeMessage mail = new MimeMessage(session, inputStream);

        String id = mail.getMessageID();
        String[] splitString = id.split("@");
        String domain = splitString[1];
        domain = domain.toLowerCase();

        String expectedMessage = "In-Reply-To field mentions the domain of this email adress: " + domain;

        // test if an email without an In-Reply-To header goes through.
        // it should
        mail.removeHeader("In-Reply-To");
        String result = (String) checkLoop.invoke(obj, mail);
        Assert.assertEquals("A loop was detected when none was there", null, result);

        // test if an email with an In-Reply-To header containing an unrelated message id goes through.
        // it should
        mail.addHeader("In-Reply-To", "<2345j.a2s3dfgh8j98kmail@googlemail.com");
        result = (String) checkLoop.invoke(obj, mail);
        Assert.assertEquals("A loop was detected when none was there", null, result);

        // tests if an email with an In-Reply-To header containing a message id with the same @domain as
        // the message id of this very email goes through.
        // it shoudn't.
        mail.setHeader("In-Reply-To", "<2345j.a2s3dfgh8j98kmail@" + domain);
        result = (String) checkLoop.invoke(obj, mail);
        Assert.assertNotEquals("A possible loop went undetected", null, result);
        Assert.assertEquals(expectedMessage, result);
    }

    /**
     * tests if a loop is detected with an example out-of-office email.
     * 
     * @throws FileNotFoundException
     * @throws MessagingException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    @Test
    public void testOutofOffice()
        throws FileNotFoundException, MessagingException, IllegalAccessException, IllegalArgumentException,
        InvocationTargetException
    {
        File mailFile = new File("./src/test/java/controllers/exampleMails/outofoffice.eml");
        InputStream inputStream = new FileInputStream(mailFile);
        Properties properties = new Properties();
        Session session = Session.getInstance(properties);
        MimeMessage mail = new MimeMessage(session, inputStream);

        String result = (String) checkLoop.invoke(obj, mail);
        Assert.assertNotEquals("A possible loop went undetected", null, result);
    }

    /**
     * tests if a loop is detected with an example bounce mail.
     * 
     * @throws FileNotFoundException
     * @throws MessagingException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    @Test
    public void testBounceMail()
        throws FileNotFoundException, MessagingException, IllegalAccessException, IllegalArgumentException,
        InvocationTargetException
    {
        File mailFile = new File("./src/test/java/controllers/exampleMails/bouncemail.eml");
        InputStream inputStream = new FileInputStream(mailFile);
        Properties properties = new Properties();
        Session session = Session.getInstance(properties);
        MimeMessage mail = new MimeMessage(session, inputStream);

        String result = (String) checkLoop.invoke(obj, mail);
        Assert.assertNotEquals("A possible loop went undetected", null, result);
    }
}
