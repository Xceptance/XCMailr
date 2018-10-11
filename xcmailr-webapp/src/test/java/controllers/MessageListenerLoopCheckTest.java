package controllers;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Should test the way the MessageListener Class checks for email loops. It does not use a server and it doesn't check
 * what happens if a loop was detected. Includes tests for the Return-Path Header, a custom Header, the References and
 * the In-Reply-To header. Also includes an example out-of-office mail and an example bounce mail. The mails used are
 * stored in a subdirectory.
 * 
 * @author daniel
 */
public class MessageListenerLoopCheckTest
{
    private MessageListener messageListener = new MessageListener();

    /**
     * Test if a loop is detected if the Return-Path header is empty, and if no loop is detected if it contains a
     * message id.
     * 
     * @throws MessagingException
     * @throws FileNotFoundException
     */
    @Test
    @Ignore
    public void testReturnPath() throws MessagingException, FileNotFoundException
    {
        MimeMessage mail = loadMailFromFile("ReturnPath.eml");
        String expectedMessage = "Return-Path is empty";

        // test if a loop is detected if the Return-Path is empty
        // It should
        String result = messageListener.checkForLoop(mail);
        Assert.assertNotEquals("A possible loop went undetected", null, result);
        Assert.assertEquals(expectedMessage, result);

        // tests if a loop is detected with a normal Return-Path
        // it shouldn't
        mail.setHeader("Return-Path", "<23avjkhsb25s@gmail.com>");
        result = messageListener.checkForLoop(mail);
        Assert.assertEquals("A loop was detected when none was there", null, result);
    }

    /**
     * Tests the custom header. Tests if a loop is detected ... <br>
     * ... if the mail doesn't have the custom header (expected: No) <br>
     * ... if the mail has the custom header with the correct content (expected: Yes) <br>
     * ... if the mail has the custom header, but the content is wrong (expected: No) <br>
     * ... if the mail doesn't have the custom header and the content is elsewhere (expected: No) <br>
     * ... if the mail has multiple instances of the custom header and only one of them has the correct content
     * (expected: yes) <br>
     * ... if the correct custom header and the correct content are somewhere in the content part of the mail (expected:
     * No) <br>
     * 
     * @throws FileNotFoundException
     * @throws MessagingException
     */
    @Test
    public void testCustomHeader() throws FileNotFoundException, MessagingException
    {
        MimeMessage mail = loadMailFromFile("Custom-Header.eml");
        String expectedMessage = "X-Loop header with this email adress present";
        String recipient = "recipient.recipient@recipient.recipient";
        String loopHeaderContent = MessageListener.LOOP_HEADER_VALUE_PREFIX + recipient;

        // tests if a mail without the custom header goes through
        // It should
        mail.removeHeader(MessageListener.LOOP_HEADER_NAME);
        String result = messageListener.checkForLoop(mail);
        Assert.assertEquals("A loop was detected when none was there", null, result);

        // test if a loop is detected if the a different header has the same content
        mail.setHeader("X-BreakLoop", loopHeaderContent);
        result = messageListener.checkForLoop(mail);
        Assert.assertEquals("A loop was detected when none was there", null, result);
        mail.removeHeader("X-BreakLoop");

        // Tests if the correct custom header with the wrong content goes through.
        // It should
        mail.setHeader(MessageListener.LOOP_HEADER_NAME, recipient);
        result = messageListener.checkForLoop(mail);
        Assert.assertEquals("A loop was detected when none was there", null, result);

        // Tests if the message goes through (= no loop is detected) with the correct custom header and
        // the correct custom header content
        // it should
        mail.setHeader(MessageListener.LOOP_HEADER_NAME, loopHeaderContent);
        result = messageListener.checkForLoop(mail);
        Assert.assertNotEquals("A possible loop went undetected", null, result);
        Assert.assertEquals(expectedMessage, result);

        // Tests if the message goes through (= no loop is detected) if there are three instances
        // of the correct header and only the second has the correct content
        // It shoudn't
        mail.setHeader(MessageListener.LOOP_HEADER_NAME, "Wrong Content");
        mail.addHeader(MessageListener.LOOP_HEADER_NAME, loopHeaderContent);
        mail.addHeader(MessageListener.LOOP_HEADER_NAME, "Wrong Content");
        result = messageListener.checkForLoop(mail);
        Assert.assertNotEquals("A possible loop went undetected", null, result);
        Assert.assertEquals(expectedMessage, result);

        // tests if the message goes through if both the custom header and it's content are somewhere in the
        // content part of the message, not the header
        // it should
        mail.removeHeader(MessageListener.LOOP_HEADER_NAME);
        mail.setText(MessageListener.LOOP_HEADER_NAME + ": " + loopHeaderContent);
        result = messageListener.checkForLoop(mail);
        Assert.assertEquals("A loop was detected when none was there", null, result);
    }

    /**
     * Tests if the checkForLoop method detects a loop if the "@domain" part of one of the message IDs in the References
     * header is identical to the "@domain" part in the message ID of this very message. Also checks if the checkForLoop
     * method detects a loop if the References header isn't present (no), if it contains some other message ID (no) and
     * if there are multiple Reference headers and only one of them contains the same domain part (yes).
     * 
     * @throws FileNotFoundException
     * @throws MessagingException
     */
    @Test
    public void testReferencesHeader() throws FileNotFoundException, MessagingException
    {
        MimeMessage mail = loadMailFromFile("Reference.eml");

        String id = mail.getMessageID();
        String[] splitString = id.split("@");
        String domain = splitString[1];
        domain = domain.toLowerCase();

        String expectedMessage = "References header references the domain of this email adress: " + domain;

        // tests if it detects a loop if no Reference Header is there. It shoudn't
        mail.removeHeader("References");
        String result = messageListener.checkForLoop(mail);
        Assert.assertEquals("A loop was detected when none was there", null, result);

        // tests if it detects a loop if only a single message id is in the References Header and
        // that message id has the same domain as the id of the mail.
        // It should.
        mail.setHeader("References", "<2345j.a2s3dfgh8j98kmail@" + domain);
        result = messageListener.checkForLoop(mail);
        Assert.assertNotEquals("A possible loop went undetected", null, result);
        Assert.assertEquals(expectedMessage, result);

        // tests if it detects a loop if the Reference Header contains an unrelated message id.
        // It shoudn't
        mail.setHeader("References", "<2345r6.mailx4cvb@mail.xcmailr.com>");
        result = messageListener.checkForLoop(mail);
        Assert.assertEquals("A loop was detected when none was there", null, result);

        // tests if it detects a loop if the Reference Header contains two message ids and the first
        // of them has the same domain as the id of the mail while the second does not.
        // It should.
        mail.setHeader("References", "<2345r6.mailx4cvb@" + domain + "   " + "<2345r6.mailxjavamail@021465>");
        result = messageListener.checkForLoop(mail);
        Assert.assertNotEquals("A possible loop went undetected", null, result);
        Assert.assertEquals(expectedMessage, result);

        // tests if it detects a loop if the Reference Header contains two message ids and the second
        // of them has the same domain as the id of the mail while the first does not.
        // It should.
        mail.setHeader("References", "<2345r6.mailxjavamail@021465>" + "   " + "<2345r6.mailx4cvb@" + domain);
        result = messageListener.checkForLoop(mail);
        Assert.assertNotEquals("A possible loop went undetected", null, result);
        Assert.assertEquals(expectedMessage, result);
    }

    /**
     * Tests if a loop is detected if there is no In-Reply-To Header (no), if there is one and it contains an unrelated
     * message id (no) and if there is one and it contains the same @domain part (yes).
     * 
     * @throws FileNotFoundException
     * @throws MessagingException
     */
    @Test
    public void testInReplyToHeader() throws FileNotFoundException, MessagingException
    {
        MimeMessage mail = loadMailFromFile("In-Reply-To.eml");

        String id = mail.getMessageID();
        String[] splitString = id.split("@");
        String domain = splitString[1];
        domain = domain.toLowerCase();

        String expectedMessage = "In-Reply-To header mentions the domain of this email adress: " + domain;

        // test if an email without an In-Reply-To header goes through.
        // it should
        mail.removeHeader("In-Reply-To");
        String result = messageListener.checkForLoop(mail);
        Assert.assertEquals("A loop was detected when none was there", null, result);

        // test if an email with an In-Reply-To header containing an unrelated message id goes through.
        // it should
        mail.setHeader("In-Reply-To", "<2345j.a2s3dfgh8j98kmail@googlemail.com");
        result = messageListener.checkForLoop(mail);
        Assert.assertEquals("A loop was detected when none was there", null, result);

        // tests if an email with an In-Reply-To header containing a message id with the same @domain as
        // the message id of this very email goes through.
        // it shoudn't.
        mail.setHeader("In-Reply-To", "<2345j.a2s3dfgh8j98kmail@" + domain);
        result = messageListener.checkForLoop(mail);
        Assert.assertNotEquals("A possible loop went undetected", null, result);
        Assert.assertEquals(expectedMessage, result);
    }

    /**
     * Tests if a loop is detected with an example out-of-office email.
     * 
     * @throws FileNotFoundException
     * @throws MessagingException
     */
    @Test
    @Ignore
    public void testOutOfOffice() throws FileNotFoundException, MessagingException
    {
        MimeMessage mail = loadMailFromFile("outofoffice.eml");

        String result = messageListener.checkForLoop(mail);
        Assert.assertNotEquals("A possible loop went undetected", null, result);
    }

    /**
     * Tests if a loop is detected with an example bounce mail.
     * 
     * @throws FileNotFoundException
     * @throws MessagingException
     */
    @Test
    @Ignore
    public void testBounceMail() throws FileNotFoundException, MessagingException
    {
        MimeMessage mail = loadMailFromFile("bouncemail.eml");

        String result = messageListener.checkForLoop(mail);
        Assert.assertNotEquals("A possible loop went undetected", null, result);
    }

    /**
     * Tests if the program crashes should an incoming mail happen to lack a message id.
     * 
     * @throws FileNotFoundException
     * @throws MessagingException
     */
    @Test
    public void testNoHeaders() throws FileNotFoundException, MessagingException
    {
        MimeMessage mail = loadMailFromFile("noHeaders.eml");

        String result = messageListener.checkForLoop(mail);
        Assert.assertNotEquals("A possible loop went undetected", null, result);
    }

    /**
     * Loads the file with the given name and returns it as a mail message.
     * 
     * @param mailFile
     *            the file
     * @return the mail message
     * @throws FileNotFoundException
     * @throws MessagingException
     */
    private MimeMessage loadMailFromFile(String mailFile) throws FileNotFoundException, MessagingException
    {
        InputStream inputStream = new FileInputStream("./src/test/java/controllers/exampleMails/" + mailFile);
        Properties properties = new Properties();
        Session session = Session.getInstance(properties);
        MimeMessage mail = new MimeMessage(session, inputStream);
        return mail;
    }
}
