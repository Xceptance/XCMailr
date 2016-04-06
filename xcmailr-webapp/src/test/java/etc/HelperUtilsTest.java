package etc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

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

}
