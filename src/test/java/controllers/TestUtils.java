package controllers;

import java.util.Map;
import java.util.Map.Entry;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;

public class TestUtils
{
    
    /**
     * checks the equality of the number of map-entrys and for each entry the equality of the keys and values
     * @param expected - the expected hashmap
     * @param actual - the actual hashmap
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
            { //the expected key is not contained in "actual"
                fail();
            }
            else
            { // the key is contained, check the value
                assertEquals(e.getValue(), actual.get(e.getKey()));
            }
        }
    }

}
