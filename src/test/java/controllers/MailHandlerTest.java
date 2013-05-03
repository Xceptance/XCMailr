package controllers;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import etc.HelperUtils;
import ninja.NinjaTest;

public class MailHandlerTest extends NinjaTest
{
    @Mock
    JobController jc;
    
    MailHandler mh;
    
    @Before
    public void setUp(){
        mh = new MailHandler(jc);
    }
    
    @Test
    public void testGetMailTarget()
    {
        String returned = mh.getMailTarget("mailaddress@gmail.com");
        
        assertNotNull(returned);
        /*
         * TEST: domains which should definitively have some mx-records
         */
        returned = mh.getMailTarget("mailaddress@web.de");
        assertNotNull(returned);
        returned = mh.getMailTarget("mailaddress@yahoo.com");
        assertNotNull(returned);
        returned = mh.getMailTarget("mailaddress@xceptance.net");
        assertNotNull(returned);

        /*
         * TEST: domains which should have no mx-record
         */
        returned = mh.getMailTarget("mailaddress@example.com");
        assertNull(returned);

        // TODO if you set up your own dns and enter a mx-record for localhost this test should fail
        returned = mh.getMailTarget("mailaddress@localhost");
        assertNull(returned);

        // TODO maybe the returned records could be verified in another way? (check that the returned value is the right
        // one)

    }

}
