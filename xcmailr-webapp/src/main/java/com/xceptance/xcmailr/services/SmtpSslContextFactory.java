package com.xceptance.xcmailr.services;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import com.xceptance.xcmailr.config.XcmailrProperties;

/**
 * Factory responsible for initializing and configuring {@link SSLContext} instances
 * for the inbound SMTP server based on {@link XcmailrProperties.MboxProperties.SslProperties}.
 * <p>
 * Resolves keystore resources using Spring's {@link ResourceLoader}, supporting classpath
 * resources (e.g. {@code classpath:keystore.p12}) as well as external file system paths
 * (e.g. {@code file:/etc/ssl/certs/keystore.p12}).
 * </p>
 */
@Component
public class SmtpSslContextFactory
{
    private static final Logger log = LoggerFactory.getLogger(SmtpSslContextFactory.class);

    private final ResourceLoader resourceLoader;
    private final XcmailrProperties xcmailrProperties;

    /**
     * Constructs a new {@code SmtpSslContextFactory} with the required Spring dependencies.
     *
     * @param resourceLoader the Spring resource loader used to resolve keystore locations
     * @param xcmailrProperties the application properties containing inbound SMTP configuration
     */
    public SmtpSslContextFactory(final ResourceLoader resourceLoader, final XcmailrProperties xcmailrProperties)
    {
        this.resourceLoader = resourceLoader;
        this.xcmailrProperties = xcmailrProperties;
    }

    /**
     * Creates an initialized {@link SSLContext} using the application's configured
     * inbound mailbox properties.
     *
     * @return an initialized {@link SSLContext}, or {@code null} if TLS is disabled
     */
    public SSLContext createSslContext()
    {
        return createSslContext(xcmailrProperties.getMbox());
    }

    /**
     * Creates an initialized {@link SSLContext} using the provided {@link XcmailrProperties.MboxProperties}.
     *
     * @param mboxProperties the mailbox configuration settings to use
     * @return an initialized {@link SSLContext}, or {@code null} if TLS is disabled
     */
    public SSLContext createSslContext(final XcmailrProperties.MboxProperties mboxProperties)
    {
        // If TLS is explicitly disabled for inbound SMTP, no SSLContext is required
        if (mboxProperties == null || !mboxProperties.isEnableTls())
        {
            log.info("Inbound SMTP TLS is disabled; skipping SSLContext initialization");
            return null;
        }

        final XcmailrProperties.MboxProperties.SslProperties ssl = mboxProperties.getSsl();
        if (ssl == null)
        {
            throw new IllegalStateException("Inbound SMTP TLS is enabled, but SSL properties are not configured");
        }

        final String keyStoreLocation = ssl.getKeyStore();
        if (keyStoreLocation == null || keyStoreLocation.isBlank())
        {
            throw new IllegalStateException("Inbound SMTP TLS keystore location must neither be null nor blank");
        }

        log.info("Loading inbound SMTP TLS keystore from: {}", keyStoreLocation);

        try
        {
            // Resolve keystore resource via Spring's ResourceLoader (handles classpath: and file:)
            final Resource resource = resourceLoader.getResource(keyStoreLocation);
            if (!resource.exists())
            {
                throw new FileNotFoundException("SMTP TLS keystore resource not found: " + keyStoreLocation);
            }

            // Determine keystore type, defaulting to PKCS12 if unspecified
            final String storeType = (ssl.getKeyStoreType() != null && !ssl.getKeyStoreType().isBlank())
                ? ssl.getKeyStoreType()
                : "PKCS12";
            final KeyStore keyStore = KeyStore.getInstance(storeType);

            final char[] password = (ssl.getKeyStorePassword() != null)
                ? ssl.getKeyStorePassword().toCharArray()
                : new char[0];

            // Load the keystore stream into the KeyStore instance
            try (final InputStream inputStream = resource.getInputStream())
            {
                keyStore.load(inputStream, password);
            }

            // Initialize KeyManagerFactory with standard algorithm (e.g. SunX509 or PKIX)
            final String kmfAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(kmfAlgorithm);
            kmf.init(keyStore, password);

            // Construct and initialize the TLS SSLContext
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);

            log.info("Inbound SMTP SSLContext initialized successfully from keystore: {}", keyStoreLocation);
            return sslContext;
        }
        catch (final Exception e)
        {
            log.error("Failed to initialize inbound SMTP SSLContext from keystore: {}", keyStoreLocation, e);
            throw new IllegalStateException("Failed to initialize inbound SMTP SSLContext: " + e.getMessage(), e);
        }
    }
}
