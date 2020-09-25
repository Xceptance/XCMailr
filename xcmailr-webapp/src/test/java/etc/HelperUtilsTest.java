package etc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomUtils;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Maps;

@RunWith(MockitoJUnitRunner.class)
public class HelperUtilsTest
{
    Map<String, String> returnedData = Maps.newHashMap();

    String result;

    @Test
    public void testHasCorrectFormat()
    {
        boolean correctFormat = HelperUtils.hasCorrectFormat("2013-12-12 12:12");
        assertTrue(correctFormat);

        correctFormat = HelperUtils.hasCorrectFormat("2013-2-12 12:12");
        assertTrue(correctFormat);

        correctFormat = HelperUtils.hasCorrectFormat("2013-12-2 12:12");
        assertTrue(correctFormat);

        correctFormat = HelperUtils.hasCorrectFormat("2013-2-2 1:12");
        assertTrue(correctFormat);

        correctFormat = HelperUtils.hasCorrectFormat("2013-12-12 12:2");
        assertTrue(correctFormat);

        correctFormat = HelperUtils.hasCorrectFormat("13-12-12 12:21");
        assertFalse(correctFormat);

        correctFormat = HelperUtils.hasCorrectFormat("2013-- 12:21");
        assertFalse(correctFormat);

        correctFormat = HelperUtils.hasCorrectFormat("2013-12-12 :21");
        assertFalse(correctFormat);

        correctFormat = HelperUtils.hasCorrectFormat("2013-12-12    12:21");
        assertFalse(correctFormat);

        correctFormat = HelperUtils.hasCorrectFormat("2x, 2h");
        assertFalse(correctFormat);
    }

    @Test
    public void testParseTimeString()
    {
        long ts = HelperUtils.parseTimeString("2013-12-12 12:12");
        DateTime dt = new DateTime(2013, 12, 12, 12, 12);
        assertEquals(dt.getMillis(), ts);

        ts = HelperUtils.parseTimeString(" 2013-12-12 12:12 ");
        assertEquals(dt.getMillis(), ts);

        ts = HelperUtils.parseTimeString("12:12:12 12:12");
        assertEquals(-1L, ts);

        ts = HelperUtils.parseTimeString("2013-12-12   12:12");
        assertEquals(-1L, ts);

        ts = HelperUtils.parseTimeString("2x, 2h");
        assertEquals(-1L, ts);
    }

    @Test
    public void testSplitMailAddress()
    {
        assertNull(HelperUtils.splitMailAddress(null));
        assertNull(HelperUtils.splitMailAddress(""));
        assertNull(HelperUtils.splitMailAddress("  "));

        String[] parts = HelperUtils.splitMailAddress("foo");
        assertNotNull(parts);
        assertEquals(1, parts.length);

        parts = HelperUtils.splitMailAddress("foo@bar");
        assertNotNull(parts);
        assertEquals(2, parts.length);
    }

    @Test
    public void testCheckEmailAddressValidness()
    {
        assertFalse(HelperUtils.checkEmailAddressValidness(null, null));
        assertFalse(HelperUtils.checkEmailAddressValidness(null, ArrayUtils.EMPTY_STRING_ARRAY));
        assertFalse(HelperUtils.checkEmailAddressValidness(ArrayUtils.EMPTY_STRING_ARRAY,
                                                           ArrayUtils.EMPTY_STRING_ARRAY));

        String[] domainList = ArrayUtils.toArray("test.localhost");
        String[] mailParts = ArrayUtils.toArray("foo");
        assertFalse(HelperUtils.checkEmailAddressValidness(mailParts, domainList));

        mailParts = ArrayUtils.add(mailParts, "bar");
        assertFalse(HelperUtils.checkEmailAddressValidness(mailParts, domainList));

        mailParts[1] = "test.localhost";
        assertTrue(HelperUtils.checkEmailAddressValidness(mailParts, domainList));

        mailParts[1] = "TesT.LOCALHosT";
        assertTrue(HelperUtils.checkEmailAddressValidness(mailParts, domainList));
    }

    @Test(expected = SizeLimitExceededException.class)
    public void testReadRawContent_LimitExceeded() throws Exception
    {
        final InputStream is = new ByteArrayInputStream(RandomUtils.nextBytes(30));
        HelperUtils.readLimitedAmount(is, 25);
    }
}
