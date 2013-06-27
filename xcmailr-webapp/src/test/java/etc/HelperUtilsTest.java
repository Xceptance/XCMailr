package etc;

import static org.junit.Assert.*;
import java.util.Map;

import org.joda.time.DateTime;
import org.junit.Test;
import ninja.utils.NinjaProperties;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

public class HelperUtilsTest
{
    @Inject
    NinjaProperties ninjaProp;

    Map<String, String> returnedData = Maps.newHashMap();

    String result;

    @Test
    public void testGetRndString()
    {

        /*
         * TEST: generate a random string with a length that will highly probable contain all chars and check whether
         * they are contained in the returned string
         */
        String returned = HelperUtils.getRandomString(1000);

        assertNotNull(returned);
        assertTrue(returned.contains("a"));
        assertTrue(returned.contains("b"));
        assertTrue(returned.contains("c"));
        assertTrue(returned.contains("d"));
        assertTrue(returned.contains("e"));
        assertTrue(returned.contains("f"));
        assertTrue(returned.contains("g"));
        assertTrue(returned.contains("h"));
        assertTrue(returned.contains("i"));
        assertTrue(returned.contains("j"));
        assertTrue(returned.contains("k"));
        assertTrue(returned.contains("l"));
        assertTrue(returned.contains("m"));
        assertTrue(returned.contains("n"));
        assertTrue(returned.contains("o"));
        assertTrue(returned.contains("p"));
        assertTrue(returned.contains("q"));
        assertTrue(returned.contains("r"));
        assertTrue(returned.contains("s"));
        assertTrue(returned.contains("t"));
        assertTrue(returned.contains("u"));
        assertTrue(returned.contains("v"));
        assertTrue(returned.contains("w"));
        assertTrue(returned.contains("x"));
        assertTrue(returned.contains("y"));
        assertTrue(returned.contains("z"));
        assertTrue(returned.contains("Q"));
        assertTrue(returned.contains("W"));
        assertTrue(returned.contains("E"));
        assertTrue(returned.contains("R"));
        assertTrue(returned.contains("T"));
        assertTrue(returned.contains("Z"));
        assertTrue(returned.contains("U"));
        assertTrue(returned.contains("I"));
        assertTrue(returned.contains("O"));
        assertTrue(returned.contains("P"));
        assertTrue(returned.contains("A"));
        assertTrue(returned.contains("S"));
        assertTrue(returned.contains("D"));
        assertTrue(returned.contains("F"));
        assertTrue(returned.contains("G"));
        assertTrue(returned.contains("H"));
        assertTrue(returned.contains("J"));
        assertTrue(returned.contains("K"));
        assertTrue(returned.contains("L"));
        assertTrue(returned.contains("Y"));
        assertTrue(returned.contains("X"));
        assertTrue(returned.contains("C"));
        assertTrue(returned.contains("V"));
        assertTrue(returned.contains("B"));
        assertTrue(returned.contains("N"));
        assertTrue(returned.contains("M"));
        assertTrue(returned.contains("1"));
        assertTrue(returned.contains("2"));
        assertTrue(returned.contains("3"));
        assertTrue(returned.contains("4"));
        assertTrue(returned.contains("5"));
        assertTrue(returned.contains("6"));
        assertTrue(returned.contains("7"));
        assertTrue(returned.contains("8"));
        assertTrue(returned.contains("9"));
        assertTrue(returned.contains("0"));

    }

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

    }

}
