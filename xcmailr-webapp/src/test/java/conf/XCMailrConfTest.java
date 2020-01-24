package conf;

import java.lang.reflect.Field;

import org.apache.commons.configuration.CompositeConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ninja.utils.NinjaMode;
import ninja.utils.NinjaPropertiesImpl;

/**
 * Tests the implementation of {@linkplain XCMailrConf}.
 *
 * @author Hartmut Arlt (Xceptance Software Technologies GmbH)
 */
public class XCMailrConfTest
{
    private static final String MBOX_DOMAINS_PROP = "mbox.dlist";

    private NinjaPropertiesImpl ninjaProperties;

    @Before
    public void initProps() throws Throwable
    {
        ninjaProperties = new NinjaPropertiesImpl(NinjaMode.test);
        disableDelimiterParsing();
    }

    @Test(expected = RuntimeException.class)
    public void testMboxDomainUndefined()
    {
        ninjaProperties.setProperty(MBOX_DOMAINS_PROP, null);

        new XCMailrConf(ninjaProperties);
    }

    @Test
    public void testMboxDomainEmpty()
    {
        ninjaProperties.setProperty(MBOX_DOMAINS_PROP, "");
        final XCMailrConf conf = new XCMailrConf(ninjaProperties);
        Assert.assertNotNull(conf.DOMAIN_LIST);
        Assert.assertEquals(0, conf.DOMAIN_LIST.length);
    }

    @Test
    public void testSingleMBoxDomain()
    {
        ninjaProperties.setProperty(MBOX_DOMAINS_PROP, "  xcmailr.test  ");
        final XCMailrConf conf = new XCMailrConf(ninjaProperties);
        Assert.assertNotNull(conf.DOMAIN_LIST);
        Assert.assertEquals(1, conf.DOMAIN_LIST.length);
        Assert.assertEquals("xcmailr.test", conf.DOMAIN_LIST[0]);
    }

    @Test
    public void testMultipleMBoxDomains()
    {
        ninjaProperties.setProperty(MBOX_DOMAINS_PROP, "  xcmailr.test , ccmailr.test,  ");

        final XCMailrConf conf = new XCMailrConf(ninjaProperties);
        Assert.assertNotNull(conf.DOMAIN_LIST);
        Assert.assertEquals(2, conf.DOMAIN_LIST.length);
        Assert.assertEquals("xcmailr.test", conf.DOMAIN_LIST[0]);
        Assert.assertEquals("ccmailr.test", conf.DOMAIN_LIST[1]);
    }

    @Test
    public void testMultipleDomainsWithDuplicates()
    {
        ninjaProperties.setProperty(MBOX_DOMAINS_PROP, "  xcmailr.test , ccmailr.test, XCMailr.test, CCMAILR.teST, ccmailr.test ");

        final XCMailrConf conf = new XCMailrConf(ninjaProperties);
        Assert.assertNotNull(conf.DOMAIN_LIST);
        Assert.assertEquals(2, conf.DOMAIN_LIST.length);
        Assert.assertEquals("xcmailr.test", conf.DOMAIN_LIST[0]);
        Assert.assertEquals("ccmailr.test", conf.DOMAIN_LIST[1]);
    }
    
    private void disableDelimiterParsing() throws Throwable
    {
        final Field field = NinjaPropertiesImpl.class.getDeclaredField("compositeConfiguration");
        field.setAccessible(true);

        final CompositeConfiguration fieldVal = (CompositeConfiguration) field.get(ninjaProperties);
        fieldVal.setDelimiterParsingDisabled(true);
    }
}
