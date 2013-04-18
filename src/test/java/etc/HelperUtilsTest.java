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

    Map<String, String> headers = Maps.newHashMap();

    Map<String, String> formParams = Maps.newHashMap();

    Map<String, String> returnedData = Maps.newHashMap();

    Map<String, String> userData = Maps.newHashMap();

    String result;

    @Test
    public void testParseDuration()
    {

        /*
         * TEST: check request for a unlimited mailbox
         */
        Long returned = HelperUtils.parseDuration("0");
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

        //TODO this should return a correct value
        returned = HelperUtils.parseDuration("2h,1D");
        assertEquals(new Long(-1), returned);
        returned = HelperUtils.parseDuration("2H,1d");
        assertEquals(new Long(-1), returned);
    }

}
