package com.xceptance.xcmailr.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests verifying configuration properties in {@link XcmailrProperties},
 * specifically the nested {@link XcmailrProperties.MboxProperties.SslProperties}
 * configuration defaults, custom setters, and immutability/binding constraints.
 */
class XcmailrPropertiesTest
{
    /**
     * Verifies that newly instantiated {@link XcmailrProperties} contains
     * the expected default inbound SMTP SSL/TLS settings.
     */
    @Test
    @DisplayName("Should initialize SslProperties with secure development defaults")
    void shouldInitializeSslPropertiesWithDefaults()
    {
        final XcmailrProperties properties = new XcmailrProperties();
        final XcmailrProperties.MboxProperties mbox = properties.getMbox();

        assertNotNull(mbox, "MboxProperties must not be null by default");

        final XcmailrProperties.MboxProperties.SslProperties ssl = mbox.getSsl();
        assertNotNull(ssl, "SslProperties must not be null by default");

        // Verify default property values for zero-config local development
        assertEquals("classpath:keystore.p12", ssl.getKeyStore(), "Default keystore location must point to bundled classpath resource");
        assertEquals("topsecret", ssl.getKeyStorePassword(), "Default keystore password must match development keystore");
        assertEquals("PKCS12", ssl.getKeyStoreType(), "Default keystore format must be standard PKCS12");
        assertNull(ssl.getKeyAlias(), "Default certificate alias must be null (using first available key)");
    }

    /**
     * Verifies that custom values can be assigned to {@link XcmailrProperties.MboxProperties.SslProperties}
     * via getters and setters as used by Spring Boot ConfigurationProperties binding.
     */
    @Test
    @DisplayName("Should correctly update and return custom SslProperties values")
    void shouldSetAndGetCustomSslProperties()
    {
        final XcmailrProperties.MboxProperties.SslProperties ssl = new XcmailrProperties.MboxProperties.SslProperties();

        final String customKeyStore = "file:/etc/ssl/certs/xcmailr.p12";
        final String customPassword = "production-secret-password";
        final String customType = "JKS";
        final String customAlias = "xcmailr-prod-cert";

        ssl.setKeyStore(customKeyStore);
        ssl.setKeyStorePassword(customPassword);
        ssl.setKeyStoreType(customType);
        ssl.setKeyAlias(customAlias);

        assertEquals(customKeyStore, ssl.getKeyStore(), "Custom keystore path must be preserved");
        assertEquals(customPassword, ssl.getKeyStorePassword(), "Custom keystore password must be preserved");
        assertEquals(customType, ssl.getKeyStoreType(), "Custom keystore type must be preserved");
        assertEquals(customAlias, ssl.getKeyAlias(), "Custom certificate alias must be preserved");
    }

    /**
     * Verifies that replacing the entire {@link XcmailrProperties.MboxProperties.SslProperties}
     * instance on {@link XcmailrProperties.MboxProperties} works as expected.
     */
    @Test
    @DisplayName("Should allow replacing SslProperties instance on MboxProperties")
    void shouldAllowReplacingSslPropertiesInstance()
    {
        final XcmailrProperties.MboxProperties mbox = new XcmailrProperties.MboxProperties();
        final XcmailrProperties.MboxProperties.SslProperties customSsl = new XcmailrProperties.MboxProperties.SslProperties();
        customSsl.setKeyStore("file:/opt/keystore.p12");

        mbox.setSsl(customSsl);

        assertEquals("file:/opt/keystore.p12", mbox.getSsl().getKeyStore(), "Updated SslProperties instance must be returned");
    }
}
