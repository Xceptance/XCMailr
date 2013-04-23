package etc;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.joda.time.DateTime;
import org.junit.Test;

import ninja.utils.NinjaProperties;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import controllers.TestUtils;

public class HelperUtilsTest
{
    @Inject
    NinjaProperties ninjaProp;

    Map<String, String> returnedData = Maps.newHashMap();

    String result;

    @Test
    public void testParseDuration()
    {

        /*
         * TEST: check request for a unlimited mailbox
         */
        Long returned = HelperUtils.parseDuration("0");
        assertEquals(new Long(0), returned);
        
        returned = HelperUtils.parseDuration("0 ");
        assertEquals(new Long(0), returned);
        
        returned = HelperUtils.parseDuration(" 0");
        assertEquals(new Long(0), returned);
        
        returned = HelperUtils.parseDuration(" 0 ");
        assertEquals(new Long(0), returned);

        /*
         * TEST: check input with format like "2h"
         */

        returned = HelperUtils.parseDuration("2h");
        DateTime dt = new DateTime();
        TestUtils.testTimeEqualityNearMinutes(dt.plusHours(2).getMillis(), returned);

        /*
         * TEST: check input with format like "2d"
         */

        returned = HelperUtils.parseDuration("2d");
        dt = new DateTime();
        TestUtils.testTimeEqualityNearMinutes(dt.plusDays(2).getMillis(), returned);

        /*
         * TEST: check input with format like "2d,2h"
         */

        returned = HelperUtils.parseDuration("2d,2h");
        dt = new DateTime();
        TestUtils.testTimeEqualityNearMinutes(dt.plusDays(2).plusHours(2).getMillis(), returned);
        /*
         * TEST: check input with format like "2h,2d"
         */

        returned = HelperUtils.parseDuration("2h,2d");
        dt = new DateTime();
        TestUtils.testTimeEqualityNearMinutes(dt.plusDays(2).plusHours(2).getMillis(), returned);

        /*
         * TEST: check input with wrong formats like "2m,2d"
         */

        returned = HelperUtils.parseDuration("2m,2d");
        assertEquals(new Long(-1), returned);
        returned = HelperUtils.parseDuration("2md");
        assertEquals(new Long(-1), returned);
        returned = HelperUtils.parseDuration("2d,0");
        assertEquals(new Long(-1), returned);
        returned = HelperUtils.parseDuration("0,2d");
        assertEquals(new Long(-1), returned);
        returned = HelperUtils.parseDuration("0,2h");
        assertEquals(new Long(-1), returned);
        returned = HelperUtils.parseDuration("2h1d");
        assertEquals(new Long(-1), returned);
        returned = HelperUtils.parseDuration("2h,1m");
        assertEquals(new Long(-1), returned);
        returned = HelperUtils.parseDuration("0,0");
        assertEquals(new Long(-1), returned);

        /*
         * TEST: check for ignored spaces
         */
        returned = HelperUtils.parseDuration("2 h,3 d");
        dt = new DateTime();
        TestUtils.testTimeEqualityNearMinutes(dt.plusDays(3).plusHours(2).getMillis(), returned);

        /*
         * TEST: check for uppercase-letters
         */
        returned = HelperUtils.parseDuration("2h,1D");
        dt = new DateTime();
        TestUtils.testTimeEqualityNearMinutes(dt.plusDays(1).plusHours(2).getMillis(), returned);
        returned = HelperUtils.parseDuration("2H,1d");
        dt = new DateTime();
        TestUtils.testTimeEqualityNearMinutes(dt.plusDays(1).plusHours(2).getMillis(), returned);
        returned = HelperUtils.parseDuration("2 H,1 D");
        dt = new DateTime();
        TestUtils.testTimeEqualityNearMinutes(dt.plusDays(1).plusHours(2).getMillis(), returned);

    }

    @Test
    public void testGetMailTarget()
    {
        String returned = HelperUtils.getMailTarget("mailaddress@gmail.com");
        assertNotNull(returned);
        /*
         * TEST: domains which should definitively have some mx-records
         */
        returned = HelperUtils.getMailTarget("mailaddress@web.de");
        assertNotNull(returned);
        returned = HelperUtils.getMailTarget("mailaddress@yahoo.com");
        assertNotNull(returned);
        returned = HelperUtils.getMailTarget("mailaddress@xceptance.net");
        assertNotNull(returned);

        /*
         * TEST: domains which should have no mx-record
         */
        returned = HelperUtils.getMailTarget("mailaddress@example.com");
        assertNull(returned);

        // TODO if you set up your own dns and enter a mx-record for localhost this test should fail
        returned = HelperUtils.getMailTarget("mailaddress@localhost");
        assertNull(returned);

        // TODO maybe the returned records could be verified in another way? (check that the returned value is the right
        // one)

    }

    @Test
    public void testGetRndString()
    {

        /*
         * TEST: generate a random string with a length that will highly probable contain all chars and check whether
         * they are contained in the returned string
         */
        String returned = HelperUtils.getRndString(1000);
        
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

}
