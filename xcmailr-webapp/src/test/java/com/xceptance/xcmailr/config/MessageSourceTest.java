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

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Unit tests verifying localization message bundle resolution and parameter interpolation (Tier 1).
 * <p>
 * Tests verify that the message bundles at {@code conf/messages} resolve the expected localized
 * pagination summary strings with en-dash ranges for both English and German locales.
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
class MessageSourceTest
{
    private ResourceBundleMessageSource messageSource;

    /**
     * Initializes the message source configured matching {@code application.yml} settings.
     */
    @BeforeEach
    void setUp()
    {
        final ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("conf/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        source.setUseCodeAsDefaultMessage(false);
        this.messageSource = source;
    }

    /**
     * Verifies happy path: English range message resolution with parameter interpolation.
     */
    @Test
    @DisplayName("Happy Path: English range message formatting with parameters")
    void resolveEnglishShowingRange()
    {
        final Object[] args = new Object[]{1, 15, 17};
        final String message = messageSource.getMessage("mailbox.table.showingRange", args, Locale.ENGLISH);

        assertEquals("Showing 1–15 of 17 mailboxes", message, "Expected English range message with typographic en-dash");
    }

    /**
     * Verifies happy path: English singular item message resolution.
     */
    @Test
    @DisplayName("Happy Path: English singular mailbox message formatting")
    void resolveEnglishShowingSingle()
    {
        final String message = messageSource.getMessage("mailbox.table.showingSingle", null, Locale.ENGLISH);

        assertEquals("Showing 1 of 1 mailbox", message, "Expected English singular mailbox message");
    }

    /**
     * Verifies happy path: German range message resolution with parameter interpolation.
     */
    @Test
    @DisplayName("Happy Path: German range message formatting with parameters")
    void resolveGermanShowingRange()
    {
        final Object[] args = new Object[]{1, 15, 17};
        final String message = messageSource.getMessage("mailbox.table.showingRange", args, Locale.GERMAN);

        assertEquals("Zeige 1–15 von 17 Postfächern", message, "Expected German range message with typographic en-dash");
    }

    /**
     * Verifies happy path: German singular item message resolution.
     */
    @Test
    @DisplayName("Happy Path: German singular mailbox message formatting")
    void resolveGermanShowingSingle()
    {
        final String message = messageSource.getMessage("mailbox.table.showingSingle", null, Locale.GERMAN);

        assertEquals("Zeige 1 von 1 Postfach", message, "Expected German singular mailbox message");
    }

    /**
     * Verifies special case: Second page range interpolation with larger offsets (e.g. Issue #92 example: 15 of 333).
     */
    @Test
    @DisplayName("Special Case: Multi-page subsequent page range formatting")
    void resolveSubsequentPageRange()
    {
        final Object[] englishArgs = new Object[]{16, 30, 333};
        final String englishMessage = messageSource.getMessage("mailbox.table.showingRange", englishArgs, Locale.ENGLISH);
        assertEquals("Showing 16–30 of 333 mailboxes", englishMessage);

        final Object[] germanArgs = new Object[]{16, 30, 333};
        final String germanMessage = messageSource.getMessage("mailbox.table.showingRange", germanArgs, Locale.GERMAN);
        assertEquals("Zeige 16–30 von 333 Postfächern", germanMessage);
    }

    /**
     * Verifies fallback behavior: When a locale is not explicitly provided with a dedicated bundle
     * (e.g. Italian or French), the message source gracefully falls back to the default English
     * message bundle without throwing {@link org.springframework.context.NoSuchMessageException}.
     */
    @Test
    @DisplayName("Fallback Case: Unsupported locale falls back to default English message")
    void resolveFallbackToDefaultEnglishOnUnsupportedLocale()
    {
        final Object[] args = new Object[]{1, 10, 25};
        final String message = messageSource.getMessage("mailbox.table.showingRange", args, Locale.ITALIAN);

        assertEquals("Showing 1–10 of 25 mailboxes", message, "Expected fallback to default English message for Italian locale");
    }
}
