/*
 * Copyright (c) Xceptance Software Technologies GmbH
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
package com.xceptance.xcmailr.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Unit tests verifying exact key parity, parameter placeholder alignment, and
 * non-empty translations across English and German message bundles.
 *
 * @author Xceptance Software Technologies GmbH
 */
class MessageBundleParityTest
{
    private static final Pattern PARAM_PATTERN = Pattern.compile("\\{(\\d+)\\}");

    private Properties englishProperties;
    private Properties germanProperties;
    private ResourceBundleMessageSource messageSource;

    /**
     * Loads the raw properties bundles with UTF-8 encoding and initializes the message source.
     *
     * @throws IOException if a properties file cannot be loaded
     */
    @BeforeEach
    void setUp() throws IOException
    {
        this.englishProperties = loadProperties("conf/messages.properties");
        this.germanProperties = loadProperties("conf/messages_de.properties");

        final ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("conf/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        source.setUseCodeAsDefaultMessage(false);
        this.messageSource = source;
    }

    /**
     * Loads a properties file from the classpath using UTF-8 character encoding.
     *
     * @param resourcePath the classpath resource path
     * @return the loaded Properties object
     * @throws IOException if the resource is missing or unreadable
     */
    private Properties loadProperties(final String resourcePath) throws IOException
    {
        final Properties props = new Properties();
        try (final InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath))
        {
            assertNotNull(in, "Properties resource must exist on classpath: " + resourcePath);
            try (final InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
            {
                props.load(reader);
            }
        }
        return props;
    }

    /**
     * Verifies that all modern namespaced keys defined in the English bundle exist in the German bundle.
     */
    @Test
    @DisplayName("Parity: All modern English keys exist in German bundle")
    void allEnglishNamespacedKeysExistInGerman()
    {
        final Set<String> missingInGerman = new HashSet<>();
        for (final String key : englishProperties.stringPropertyNames())
        {
            if (isModernNamespacedKey(key) && !germanProperties.containsKey(key))
            {
                missingInGerman.add(key);
            }
        }

        assertTrue(missingInGerman.isEmpty(),
                "The following modern keys are missing in messages_de.properties: " + missingInGerman);
    }

    /**
     * Verifies that all modern namespaced keys defined in the German bundle exist in the English bundle.
     */
    @Test
    @DisplayName("Parity: All modern German keys exist in English bundle")
    void allGermanNamespacedKeysExistInEnglish()
    {
        final Set<String> missingInEnglish = new HashSet<>();
        for (final String key : germanProperties.stringPropertyNames())
        {
            if (isModernNamespacedKey(key) && !englishProperties.containsKey(key))
            {
                missingInEnglish.add(key);
            }
        }

        assertTrue(missingInEnglish.isEmpty(),
                "The following modern keys are missing in messages.properties: " + missingInEnglish);
    }

    /**
     * Verifies that no modern translation key has an empty or blank value in either bundle.
     */
    @Test
    @DisplayName("Integrity: No modern translation has empty or whitespace-only value")
    void noEmptyTranslations()
    {
        for (final String key : englishProperties.stringPropertyNames())
        {
            if (isModernNamespacedKey(key))
            {
                final String enValue = englishProperties.getProperty(key);
                assertFalse(enValue == null || enValue.trim().isEmpty(),
                        "English message for key '" + key + "' must not be empty");

                final String deValue = germanProperties.getProperty(key);
                if (deValue != null)
                {
                    assertFalse(deValue.trim().isEmpty(),
                            "German message for key '" + key + "' must not be empty");
                }
            }
        }
    }

    /**
     * Verifies that message parameter placeholder counts (e.g. {0}, {1}) match exactly between English and German.
     */
    @Test
    @DisplayName("Integrity: Parameter placeholder indices align between English and German")
    void parameterPlaceholdersMatch()
    {
        for (final String key : englishProperties.stringPropertyNames())
        {
            if (isModernNamespacedKey(key) && germanProperties.containsKey(key))
            {
                final String enText = englishProperties.getProperty(key);
                final String deText = germanProperties.getProperty(key);

                final Set<String> enPlaceholders = extractPlaceholders(enText);
                final Set<String> dePlaceholders = extractPlaceholders(deText);

                assertEquals(enPlaceholders, dePlaceholders,
                        "Parameter placeholder mismatch for key '" + key + "': English=" + enPlaceholders + ", German=" + dePlaceholders);
            }
        }
    }

    /**
     * Verifies happy path resolution of common and domain keys across English and German via Spring MessageSource.
     */
    @Test
    @DisplayName("Happy Path: Spring MessageSource resolves modern keys in English and German")
    void resolveSampleMessagesViaMessageSource()
    {
        // Common button
        assertEquals("Save", messageSource.getMessage("common.button.save", null, Locale.ENGLISH));
        assertEquals("Speichern", messageSource.getMessage("common.button.save", null, Locale.GERMAN));

        // Layout navigation
        assertEquals("Mailboxes", messageSource.getMessage("layout.nav.mailboxes", null, Locale.ENGLISH));
        assertEquals("Postfächer", messageSource.getMessage("layout.nav.mailboxes", null, Locale.GERMAN));

        // Auth
        assertEquals("Sign In", messageSource.getMessage("auth.login.title", null, Locale.ENGLISH));
        assertEquals("Anmelden", messageSource.getMessage("auth.login.title", null, Locale.GERMAN));

        // Profile
        assertEquals("Personal Information", messageSource.getMessage("profile.personalInfo.title", null, Locale.ENGLISH));
        assertEquals("Persönliche Daten", messageSource.getMessage("profile.personalInfo.title", null, Locale.GERMAN));

        // Parameterized mailbox delete confirmation
        final Object[] args = new Object[]{"test@example.com"};
        final String enConfirm = messageSource.getMessage("mailbox.action.deleteConfirm", args, Locale.ENGLISH);
        final String deConfirm = messageSource.getMessage("mailbox.action.deleteConfirm", args, Locale.GERMAN);

        assertTrue(enConfirm.contains("test@example.com"));
        assertTrue(deConfirm.contains("test@example.com"));
    }

    /**
     * Verifies that when resolving a message for an unsupported locale or when a translation is absent from
     * the German bundle, the message source gracefully falls back to the default English bundle
     * without throwing a {@link org.springframework.context.NoSuchMessageException} or runtime exception.
     */
    @Test
    @DisplayName("Fallback: MessageSource gracefully falls back to default English when key or locale is absent")
    void fallbackToDefaultEnglishWhenKeyOrLocaleAbsent()
    {
        // 1. Unsupported locales fall back to default English bundle
        final String layoutNav = messageSource.getMessage("layout.nav.mailboxes", null, Locale.ITALIAN);
        assertEquals("Mailboxes", layoutNav, "Expected fallback to default English message for Italian locale");

        final String authLogin = messageSource.getMessage("auth.login.title", null, Locale.FRENCH);
        assertEquals("Sign In", authLogin, "Expected fallback to default English message for French locale");

        // 2. Key present in default messages.properties but absent in messages_de.properties falls back to English
        final Object[] args = new Object[]{"XCMailr"};
        final String fallbackGerman = messageSource.getMessage("user_Activate_Title", args, Locale.GERMAN);
        assertEquals("Account activation at XCMailr", fallbackGerman,
                "Expected German locale resolution to fall back to English bundle when key is absent in German bundle");

        // 3. Completely absent key with default fallback string resolves without throwing exception
        final String defaultFallback = messageSource.getMessage("nonexistent.translation.key", null, "Default Fallback Text", Locale.GERMAN);
        assertEquals("Default Fallback Text", defaultFallback,
                "Expected graceful resolution of default message when translation code is absent");
    }

    /**
     * Helper to extract all placeholder indices (e.g. "{0}", "{1}") from a message string.
     */
    private Set<String> extractPlaceholders(final String text)
    {
        final Set<String> placeholders = new HashSet<>();
        if (text == null)
        {
            return placeholders;
        }
        final Matcher matcher = PARAM_PATTERN.matcher(text);
        while (matcher.find())
        {
            placeholders.add(matcher.group(1));
        }
        return placeholders;
    }

    /**
     * Determines whether a key follows the modern dot-notation namespace convention.
     */
    private boolean isModernNamespacedKey(final String key)
    {
        return key.startsWith("common.")
                || key.startsWith("layout.")
                || key.startsWith("auth.")
                || key.startsWith("profile.")
                || key.startsWith("mailbox.")
                || key.startsWith("mail.")
                || key.startsWith("admin.")
                || key.startsWith("error.");
    }
}
