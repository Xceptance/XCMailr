package controllers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Assert;
import org.junit.Test;

public class MessageListenerRawContentTest
{
    @Test
    public void testReadRawContent() throws Exception
    {
        final InputStream is = getClass().getResourceAsStream("multiPart.eml");
        Assert.assertNotNull("Failed to load 'multiPart.eml'", is);

        final byte[] rawContent = MessageListener.readLimitedAmount(is, 25_000_000);
        Assert.assertNotNull("RAW content is null", rawContent);

        final String raw = new String(rawContent, StandardCharsets.UTF_8);

        Assert.assertTrue("CRLF not found", raw.indexOf("\r\n") > -1);
        Assert.assertTrue("'Content-Type' header not found",
                          Pattern.compile("\r\nContent-Type:\\s*multipart/\\S+;").matcher(raw).find());
        Assert.assertTrue("'Subject' header not found", Pattern.compile("\r\nSubject:\\s*\\S+").matcher(raw).find());
    }

    @Test(expected = MessageListener.SizeLimitExceededException.class)
    public void testReadRawContent_LimitExceeded() throws Exception
    {
        final InputStream is = new ByteArrayInputStream(RandomUtils.nextBytes(30));
        MessageListener.readLimitedAmount(is, 25);
    }
}
