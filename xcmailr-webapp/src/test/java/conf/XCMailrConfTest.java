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

    private static final String APP_LANG_PROP = "application.languages";

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
        Assert.assertArrayEquals(new String[] {"xcmailr.test"}, conf.DOMAIN_LIST);
    }

    @Test
    public void testMultipleMBoxDomains()
    {
        ninjaProperties.setProperty(MBOX_DOMAINS_PROP, "  xcmailr.test , ccmailr.test,  ");

        final XCMailrConf conf = new XCMailrConf(ninjaProperties);
        Assert.assertNotNull(conf.DOMAIN_LIST);
        Assert.assertArrayEquals(new String[] {"xcmailr.test", "ccmailr.test"},conf.DOMAIN_LIST);
    }

    @Test
    public void testMultipleDomainsWithDuplicates()
    {
        ninjaProperties.setProperty(MBOX_DOMAINS_PROP, "  xcmailr.test , ccmailr.test, XCMailr.test, CCMAILR.teST, ccmailr.test ");

        final XCMailrConf conf = new XCMailrConf(ninjaProperties);
        Assert.assertNotNull(conf.DOMAIN_LIST);
        Assert.assertArrayEquals(new String[] {"xcmailr.test", "ccmailr.test"},conf.DOMAIN_LIST);
    }

    @Test(expected = RuntimeException.class)
    public void testAppLangUndefined()
    {
        ninjaProperties.setProperty(APP_LANG_PROP, null);

        new XCMailrConf(ninjaProperties);
    }

    @Test(expected = RuntimeException.class)
    public void testAppLangEmpty()
    {
        ninjaProperties.setProperty(APP_LANG_PROP, "");

        new XCMailrConf(ninjaProperties);

    }

    @Test(expected = RuntimeException.class)
    public void testAppLangBlank()
    {
        ninjaProperties.setProperty(APP_LANG_PROP, "    ");

        new XCMailrConf(ninjaProperties);
    }

    @Test
    public void testSingleAppLang()
    {
        ninjaProperties.setProperty(APP_LANG_PROP, "en");

        final XCMailrConf conf = new XCMailrConf(ninjaProperties);
        Assert.assertNotNull(conf);
        Assert.assertArrayEquals(new String[]
            {
              "en"
            }, conf.APP_LANGS);

    }

    @Test
    public void testMultipleAppLangs()
    {
        ninjaProperties.setProperty(APP_LANG_PROP, "en, fr");

        final XCMailrConf conf = new XCMailrConf(ninjaProperties);
        Assert.assertNotNull(conf);
        Assert.assertArrayEquals(new String[]
            {
              "en", "fr"
            }, conf.APP_LANGS);
    }

    @Test
    public void testMupltipleAppLangsWithDuplicates()
    {
        ninjaProperties.setProperty(APP_LANG_PROP, "en, fr, en, fr");

        final XCMailrConf conf = new XCMailrConf(ninjaProperties);
        Assert.assertNotNull(conf);
        Assert.assertArrayEquals(new String[]
            {
              "en", "fr"
            }, conf.APP_LANGS);
    }

    @Test
    public void testMultipleAppLangsPreserveCasing()
    {
        ninjaProperties.setProperty(APP_LANG_PROP, "en, EN, eN, En");

        final XCMailrConf conf = new XCMailrConf(ninjaProperties);
        Assert.assertNotNull(conf);
        Assert.assertArrayEquals(new String[]
            {
              "en", "EN", "eN", "En"
            }, conf.APP_LANGS);
    }

    private void disableDelimiterParsing() throws Throwable
    {
        final Field field = NinjaPropertiesImpl.class.getDeclaredField("compositeConfiguration");
        field.setAccessible(true);

        final CompositeConfiguration fieldVal = (CompositeConfiguration) field.get(ninjaProperties);
        fieldVal.setDelimiterParsingDisabled(true);
    }
}
