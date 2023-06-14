/*
 * Copyright (c) 2013-2023 Xceptance Software Technologies GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package controllers;

import java.util.Map;
import java.util.Map.Entry;

import org.joda.time.DateTime;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class TestUtils
{

    /**
     * checks the equality of the number of map-entrys and for each entry the equality of the keys and values
     * 
     * @param expected
     *            - the expected hashmap
     * @param actual
     *            - the actual hashmap
     */
    public static void testMapEntryEquality(Map<String, String> expected, Map<String, String> actual)
    {
        // fail the test if the number of items in both maps are not equal
        if (!(expected.size() == actual.size()))
        {
            fail();
        }

        for (Entry<String, String> e : expected.entrySet())
        {
            if (!actual.containsKey(e.getKey()))
            { // the expected key is not contained in "actual"
                fail();
            }
            else
            { // the key is contained, check the value
                assertEquals(e.getValue(), actual.get(e.getKey()));
            }
        }
    }

    /**
     * checks the equality of two longs in millisecs exactly to the minute
     * 
     * @param expected
     *            - expected millisecs
     * @param actual
     *            - returned millisecs
     */
    public static void testTimeEqualityNearMinutes(long expected, long actual)
    {
        DateTime dtexp = new DateTime(expected);
        DateTime dtact = new DateTime(actual);
        assertTrue(dtexp.getYear() == dtact.getYear());
        assertTrue(dtexp.getMonthOfYear() == dtact.getMonthOfYear());
        assertTrue(dtexp.getDayOfMonth() == dtact.getDayOfMonth());
        assertTrue(dtexp.getHourOfDay() == dtact.getHourOfDay());
        assertTrue(dtexp.getMinuteOfHour() == dtact.getMinuteOfHour());

    }

}
