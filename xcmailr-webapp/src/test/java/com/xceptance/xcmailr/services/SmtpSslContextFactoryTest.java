package com.xceptance.xcmailr.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import com.xceptance.xcmailr.config.XcmailrProperties;

/**
 * Unit tests verifying {@link SmtpSslContextFactory} behavior under normal,
 * edge-case, and error conditions according to the Test Pyramid specification.
 */
class SmtpSslContextFactoryTest
{
    private ResourceLoader resourceLoader;
    private XcmailrProperties properties;
    private SmtpSslContextFactory factory;

    /**
     * Initializes a fresh instance of properties and the factory prior to each test.
     */
    @BeforeEach
    void setUp()
    {
        this.resourceLoader = new DefaultResourceLoader();
        this.properties = new XcmailrProperties();
        this.factory = new SmtpSslContextFactory(this.resourceLoader, this.properties);
    }

    /**
     * Happy Path: Verifies that a valid PKCS12 keystore bundled in the classpath
     * initializes an active, non-null {@link SSLContext} with the TLS protocol.
     */
    @Test
    @DisplayName("Happy Path: Successfully create SSLContext from bundled PKCS12 keystore")
    void shouldCreateSslContextWhenValidKeystoreConfigured()
    {
        // By default, properties point to classpath:keystore.p12 with password 'topsecret'
        final SSLContext sslContext = factory.createSslContext();

        assertNotNull(sslContext, "Constructed SSLContext must not be null for valid keystore");
        assertEquals("TLS", sslContext.getProtocol(), "Initialized SSLContext protocol must be TLS");
    }

    /**
     * Special Case: Verifies that when inbound SMTP TLS is disabled via configuration,
     * the factory immediately returns {@code null} without attempting to load any keystore.
     */
    @Test
    @DisplayName("Special Case: Return null when inbound SMTP TLS is disabled")
    void shouldReturnNullWhenTlsIsDisabled()
    {
        properties.getMbox().setEnableTls(false);

        final SSLContext sslContext = factory.createSslContext();

        assertNull(sslContext, "Factory must return null when inbound TLS is disabled");
    }

    /**
     * Special Case: Verifies that passing a null MboxProperties reference returns {@code null}.
     */
    @Test
    @DisplayName("Special Case: Return null when MboxProperties is null")
    void shouldReturnNullWhenMboxPropertiesIsNull()
    {
        final SSLContext sslContext = factory.createSslContext(null);

        assertNull(sslContext, "Factory must return null when mboxProperties is null");
    }

    /**
     * Error Case: Verifies that configuring a non-existent keystore resource path
     * throws an {@link IllegalStateException} containing a descriptive error message.
     */
    @Test
    @DisplayName("Error Case: Throw IllegalStateException when keystore resource does not exist")
    void shouldThrowExceptionWhenKeystoreResourceNotFound()
    {
        properties.getMbox().getSsl().setKeyStore("classpath:non-existent-keystore.p12");

        final IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> factory.createSslContext(),
            "Expected IllegalStateException when keystore does not exist"
        );

        assertTrue(
            exception.getMessage().contains("SMTP TLS keystore resource not found"),
            "Exception message should mention missing resource"
        );
    }

    /**
     * Error Case: Verifies that providing an incorrect password for unlocking the keystore
     * throws an {@link IllegalStateException} wrapping the underlying keystore authentication failure.
     */
    @Test
    @DisplayName("Error Case: Throw IllegalStateException when keystore password is wrong")
    void shouldThrowExceptionWhenPasswordIsIncorrect()
    {
        properties.getMbox().getSsl().setKeyStorePassword("incorrect-password-1234");

        final IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> factory.createSslContext(),
            "Expected IllegalStateException when keystore password is wrong"
        );

        assertNotNull(exception.getCause(), "Root cause exception must be preserved");
    }

    /**
     * Error Case: Verifies that omitting the keystore location (blank or null)
     * throws an {@link IllegalStateException}.
     */
    @Test
    @DisplayName("Error Case: Throw IllegalStateException when keystore location is blank")
    void shouldThrowExceptionWhenKeystoreLocationIsBlank()
    {
        properties.getMbox().getSsl().setKeyStore("   ");

        final IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> factory.createSslContext(),
            "Expected IllegalStateException when keystore location is blank"
        );

        assertTrue(
            exception.getMessage().contains("must neither be null nor blank"),
            "Exception message should state location cannot be blank"
        );
    }

    /**
     * Error Case: Verifies that having null SslProperties while TLS is enabled throws an {@link IllegalStateException}.
     */
    @Test
    @DisplayName("Error Case: Throw IllegalStateException when SslProperties is null while TLS enabled")
    void shouldThrowExceptionWhenSslPropertiesIsNull()
    {
        properties.getMbox().setSsl(null);

        final IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> factory.createSslContext(),
            "Expected IllegalStateException when SslProperties is null"
        );

        assertTrue(
            exception.getMessage().contains("SSL properties are not configured"),
            "Exception message should indicate SSL properties are missing"
        );
    }
}
