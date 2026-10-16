/*
 * Copyright (c) 2013-2026 Xceptance Software Technologies GmbH
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
package com.xceptance.xcmailr.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests verifying {@link EmailSanitizationService} for both OWASP HTML sanitization
 * and SMTP header CRLF injection prevention.
 *
 * @author Xceptance Software Technologies GmbH
 */
public class EmailSanitizationServiceTest
{
    private EmailSanitizationService service;

    @BeforeEach
    public void setUp()
    {
        service = new EmailSanitizationService();
    }

    @Test
    @DisplayName("Should preserve safe HTML tags, formatting, and tables")
    public void testSafeHtmlPreserved()
    {
        final String input = "<h1>Order Confirmation</h1>"
                           + "<p>Hello <b>John Doe</b>,</p>"
                           + "<table border=\"1\"><tr><td>Item 1</td><td>$10.00</td></tr></table>"
                           + "<p>Visit our <a href=\"https://xcmailr.test/order/123\">order page</a>.</p>";

        final String sanitized = service.sanitizeHtml(input);

        assertTrue(sanitized.contains("<h1>Order Confirmation</h1>"));
        assertTrue(sanitized.contains("<b>John Doe</b>"));
        assertTrue(sanitized.contains("<table>"));
        assertTrue(sanitized.contains("Item 1"));
        assertTrue(sanitized.contains("<a href=\"https://xcmailr.test/order/123\""));
    }

    @Test
    @DisplayName("Should strip script tags and inline javascript")
    public void testScriptTagsStripped()
    {
        final String input = "<p>Message before script</p>"
                           + "<script>alert('XSS-ATTACK');</script>"
                           + "<script type=\"text/javascript\">document.cookie='stolen';</script>"
                           + "<p>Message after script</p>";

        final String sanitized = service.sanitizeHtml(input);

        assertFalse(sanitized.contains("<script"));
        assertFalse(sanitized.contains("alert"));
        assertFalse(sanitized.contains("stolen"));
        assertTrue(sanitized.contains("Message before script"));
        assertTrue(sanitized.contains("Message after script"));
    }

    @Test
    @DisplayName("Should strip event handlers like onerror, onclick, and onload")
    public void testEventHandlersStripped()
    {
        final String input = "<img src=\"https://xcmailr.test/img.png\" onerror=\"alert('pwned')\" />"
                           + "<button onclick=\"fetch('/evil')\">Click</button>"
                           + "<body onload=\"runBadCode()\">Body text</body>";

        final String sanitized = service.sanitizeHtml(input);

        assertFalse(sanitized.contains("onerror"));
        assertFalse(sanitized.contains("alert"));
        assertFalse(sanitized.contains("onclick"));
        assertFalse(sanitized.contains("onload"));
        assertTrue(sanitized.contains("<img"));
    }

    @Test
    @DisplayName("Should strip javascript: pseudo-protocols in links")
    public void testJavascriptPseudoProtocolStripped()
    {
        final String input = "<a href=\"javascript:alert(1337)\">Click me</a>";

        final String sanitized = service.sanitizeHtml(input);

        assertFalse(sanitized.contains("javascript:"));
        assertFalse(sanitized.contains("alert(1337)"));
    }

    @Test
    @DisplayName("Should strip iframe, object, and embed elements")
    public void testDangerousEmbeddingTagsStripped()
    {
        final String input = "<iframe src=\"https://malicious.test/iframe\"></iframe>"
                           + "<object data=\"malicious.swf\"></object>"
                           + "<embed src=\"malicious.swf\" />";

        final String sanitized = service.sanitizeHtml(input);

        assertFalse(sanitized.contains("<iframe"));
        assertFalse(sanitized.contains("<object"));
        assertFalse(sanitized.contains("<embed"));
    }

    @Test
    @DisplayName("Should handle null and empty HTML gracefully")
    public void testNullAndEmptyHtml()
    {
        assertEquals("", service.sanitizeHtml(null));
        assertEquals("", service.sanitizeHtml(""));
        assertEquals("", service.sanitizeHtml("   \t\n  "));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Subject with\rCR",
        "Subject with\nLF",
        "Subject with\r\nCRLF",
        "Subject\nBcc: victim@test.org",
        "Header\0with null byte"
    })
    @DisplayName("Should detect CRLF and null characters in tainted header strings")
    public void testContainsCrlfPositive(final String taintedHeader)
    {
        assertTrue(service.containsCrlf(taintedHeader));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Clean Subject Line",
        "Order #12345 Confirmation - XCMailr",
        "support@xcmailr.test",
        "Hello World 123!@#$%^&*()-_=+"
    })
    @DisplayName("Should return false for clean headers without CRLF")
    public void testContainsCrlfNegative(final String cleanHeader)
    {
        assertFalse(service.containsCrlf(cleanHeader));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException on CRLF header injection")
    public void testValidateHeaderThrows()
    {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.validateHeader("Subject", "Normal Subject\r\nBcc: attacker@evil.test");
        });

        assertTrue(ex.getMessage().contains("Subject"));
        assertTrue(ex.getMessage().contains("CRLF"));
    }

    @Test
    @DisplayName("Should not throw on valid clean headers")
    public void testValidateHeaderSuccess()
    {
        service.validateHeader("Subject", "Valid Subject");
        service.validateHeader("To", "user@example.com");
    }

    @Test
    @DisplayName("Should sanitize headers by stripping CRLF and collapsing whitespace")
    public void testSanitizeHeader()
    {
        assertNull(service.sanitizeHeader(null));

        final String raw = "First Line\r\nSecond Line\nThird Line\r\0Fourth Line";
        final String sanitized = service.sanitizeHeader(raw);

        assertEquals("First Line Second Line Third Line Fourth Line", sanitized);
        assertFalse(service.containsCrlf(sanitized));
    }
}
