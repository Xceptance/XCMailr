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

import java.util.regex.Pattern;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Service;

/**
 * Service providing HTML content sanitization and email header injection protection.
 * <p>
 * HTML sanitization prevents Cross-Site Scripting (XSS) when rendering untrusted email HTML bodies,
 * while CRLF injection protection guards against SMTP header splitting attacks (RFC 5322).
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
@Service
public class EmailSanitizationService
{
    /**
     * Pattern matching carriage return (\r), line feed (\n), and null byte (\0).
     */
    private static final Pattern CRLF_PATTERN = Pattern.compile("[\r\n\0]");

    /**
     * OWASP HTML Sanitizer policy tailored for rich email content.
     * Allows safe formatting, tables, images, links, styles, and common HTML5 elements,
     * while stripping dangerous executable tags (<script>, <iframe>, <object>, <embed>),
     * inline event handlers (onerror, onload, onclick), and unsafe protocols (javascript:).
     */
    private final PolicyFactory policy;

    /**
     * Constructs the sanitization service, assembling the multi-part OWASP HTML policy.
     */
    public EmailSanitizationService()
    {
        // Custom policy extensions for email layouts (e.g., center, hr, pre, code)
        final PolicyFactory customPolicy = new HtmlPolicyBuilder()
            .allowElements("center", "hr", "pre", "code", "span", "div", "blockquote")
            .allowAttributes("class", "id", "style").globally()
            .allowUrlProtocols("http", "https", "mailto", "cid")
            .toFactory();

        this.policy = Sanitizers.FORMATTING
                                .and(Sanitizers.BLOCKS)
                                .and(Sanitizers.STYLES)
                                .and(Sanitizers.LINKS)
                                .and(Sanitizers.TABLES)
                                .and(Sanitizers.IMAGES)
                                .and(customPolicy);
    }

    /**
     * Sanitizes untrusted email HTML content to eliminate malicious scripts, event handlers,
     * and unsafe URLs.
     *
     * @param untrustedHtml raw HTML from an email message
     * @return sanitized safe HTML string, or an empty string if input was null/blank
     */
    public String sanitizeHtml(final String untrustedHtml)
    {
        if (untrustedHtml == null || untrustedHtml.isBlank())
        {
            return "";
        }

        // Apply OWASP HTML sanitization pipeline
        return policy.sanitize(untrustedHtml);
    }

    /**
     * Checks whether an untrusted email header value contains CRLF injection characters
     * (carriage return, line feed, or null bytes).
     *
     * @param headerValue the header value to check
     * @return {@code true} if forbidden CRLF characters are present; {@code false} otherwise
     */
    public boolean containsCrlf(final String headerValue)
    {
        if (headerValue == null)
        {
            return false;
        }
        return CRLF_PATTERN.matcher(headerValue).find();
    }

    /**
     * Validates that an email header value does not contain CRLF characters.
     *
     * @param headerName name of the header being validated (e.g., "Subject", "From")
     * @param headerValue the header value to validate
     * @throws IllegalArgumentException if the header value contains CRLF or null bytes
     */
    public void validateHeader(final String headerName, final String headerValue)
    {
        if (containsCrlf(headerValue))
        {
            throw new IllegalArgumentException("Header '" + headerName + "' contains illegal CRLF characters.");
        }
    }

    /**
     * Sanitizes an email header value by replacing all occurrences of CRLF and null characters
     * with a single space and stripping redundant whitespace.
     *
     * @param headerValue raw header value
     * @return sanitized header value safe for SMTP headers
     */
    public String sanitizeHeader(final String headerValue)
    {
        if (headerValue == null)
        {
            return null;
        }

        // Replace carriage returns, line feeds, and null bytes with spaces
        final String cleaned = CRLF_PATTERN.matcher(headerValue).replaceAll(" ");
        // Collapse multiple contiguous whitespace into a single space and trim
        return cleaned.replaceAll("\\s+", " ").trim();
    }
}
