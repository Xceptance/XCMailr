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
package com.xceptance.xcmailr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import jakarta.activation.DataSource;
import jakarta.mail.Address;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.xceptance.xcmailr.XcmailrApplication;

/**
 * Comprehensive unit tests for {@link JakartaMimeMessageParser} following the testing pyramid.
 * Validates session properties, happy paths (raw UTF-8, RFC 2047, non-Latin, emojis),
 * error handling (malformed bytes, invalid RFC 2047, missing headers), and special cases.
 *
 * @author Xceptance Software Technologies GmbH
 */
public class JakartaMimeMessageParserTest
{
    @BeforeAll
    public static void setUpClass()
    {
        XcmailrApplication.configureMailSystemProperties();
    }

    @Test
    @DisplayName("Verify default session properties configure UTF-8 and permissive decoding")
    public void testDefaultSessionProperties()
    {
        final Properties props = JakartaMimeMessageParser.getDefaultSessionProperties();
        assertNotNull(props);
        assertEquals("true", props.getProperty("mail.mime.allowutf8"));
        assertEquals("UTF-8", props.getProperty("mail.mime.charset"));
        assertEquals("true", props.getProperty("mail.mime.decodefilename"));
        assertEquals("false", props.getProperty("mail.mime.decodetext.strict"));
    }

    @Nested
    @DisplayName("Happy Path Tests")
    class HappyPathTests
    {
        @Test
        @DisplayName("Decode raw UTF-8 subject with German umlauts and French accents")
        public void testRawUtf8SubjectUmlautsAndAccents() throws Exception
        {
            final String raw = """
                From: sender@external.com\r
                To: recipient@xcmailr.test\r
                Subject: Docker Smoke Test ü ä ö ß é à ç\r
                \r
                Testing UTF-8 reception.
                """;
            final MimeMessage msg = JakartaMimeMessageParser.createMimeMessage(raw.getBytes(StandardCharsets.UTF_8));
            assertEquals("Docker Smoke Test ü ä ö ß é à ç", msg.getSubject());
        }

        @Test
        @DisplayName("Decode raw UTF-8 subject with non-Latin scripts (Cyrillic, CJK, Arabic)")
        public void testRawUtf8SubjectNonLatinScripts() throws Exception
        {
            final String raw = """
                From: sender@external.com\r
                To: recipient@xcmailr.test\r
                Subject: Привет мир / こんにちは世界 / مرحبا\r
                \r
                International content test.
                """;
            final MimeMessage msg = JakartaMimeMessageParser.createMimeMessage(raw.getBytes(StandardCharsets.UTF_8));
            assertEquals("Привет мир / こんにちは世界 / مرحبا", msg.getSubject());
        }

        @Test
        @DisplayName("Decode raw UTF-8 subject with 4-byte multi-byte emojis")
        public void testRawUtf8SubjectWithEmojis() throws Exception
        {
            final String raw = """
                From: sender@external.com\r
                To: recipient@xcmailr.test\r
                Subject: Deployment Success 🚀🎉📧✨\r
                \r
                Emoji message body.
                """;
            final MimeMessage msg = JakartaMimeMessageParser.createMimeMessage(raw.getBytes(StandardCharsets.UTF_8));
            assertEquals("Deployment Success 🚀🎉📧✨", msg.getSubject());
        }

        @Test
        @DisplayName("Decode RFC 2047 Base64 and Quoted-Printable encoded subjects")
        public void testRfc2047EncodedSubjects() throws Exception
        {
            // Base64 encoding for "Subject ü"
            final String rawB64 = """
                From: sender@external.com\r
                To: recipient@xcmailr.test\r
                Subject: =?UTF-8?B?U3ViamVjdCDDvA==?=\r
                \r
                Base64 body.
                """;
            final MimeMessage msgB64 = JakartaMimeMessageParser.createMimeMessage(rawB64.getBytes(StandardCharsets.UTF_8));
            assertEquals("Subject ü", msgB64.getSubject());

            // Quoted-Printable encoding for "Subject ü"
            final String rawQ = """
                From: sender@external.com\r
                To: recipient@xcmailr.test\r
                Subject: =?UTF-8?Q?Subject_=C3=BC?=\r
                \r
                QP body.
                """;
            final MimeMessage msgQ = JakartaMimeMessageParser.createMimeMessage(rawQ.getBytes(StandardCharsets.UTF_8));
            assertEquals("Subject ü", msgQ.getSubject());
        }

        @Test
        @DisplayName("Decode sender display name with international characters")
        public void testSenderDisplayName() throws Exception
        {
            final String raw = """
                From: "Jürgen Schmidt" <juergen@example.com>\r
                To: recipient@xcmailr.test\r
                Subject: Sender test\r
                \r
                Body.
                """;
            final MimeMessage msg = JakartaMimeMessageParser.createMimeMessage(raw.getBytes(StandardCharsets.UTF_8));
            final Address[] from = msg.getFrom();
            assertNotNull(from);
            assertEquals(1, from.length);
            assertTrue(from[0] instanceof InternetAddress);
            final InternetAddress addr = (InternetAddress) from[0];
            assertEquals("Jürgen Schmidt", addr.getPersonal());
            assertEquals("juergen@example.com", addr.getAddress());
        }

        @Test
        @DisplayName("Extract attachment with raw UTF-8 filename")
        public void testAttachmentWithRawUtf8Filename() throws Exception
        {
            final String raw = """
                From: sender@external.com\r
                To: recipient@xcmailr.test\r
                Subject: Attachment Test\r
                MIME-Version: 1.0\r
                Content-Type: multipart/mixed; boundary="BOUNDARY123"\r
                \r
                --BOUNDARY123\r
                Content-Type: text/plain; charset=UTF-8\r
                \r
                Please find report attached.\r
                --BOUNDARY123\r
                Content-Type: application/pdf\r
                Content-Disposition: attachment; filename="prüfbericht_2026.pdf"\r
                Content-Transfer-Encoding: base64\r
                \r
                cGRmZGF0YQ==\r
                --BOUNDARY123--\r
                """;
            final MimeMessage msg = JakartaMimeMessageParser.createMimeMessage(raw.getBytes(StandardCharsets.UTF_8));
            final JakartaMimeMessageParser parser = new JakartaMimeMessageParser(msg).parse();
            final List<DataSource> attachments = parser.getAttachmentList();
            assertEquals(1, attachments.size());
            assertEquals("prüfbericht_2026.pdf", attachments.get(0).getName());
            assertEquals("Please find report attached.", parser.getPlainContent().trim());
        }

        @Test
        @DisplayName("Extract attachment with RFC 2231 parameterized encoded filename")
        public void testAttachmentWithRfc2231Filename() throws Exception
        {
            final String raw = """
                From: sender@external.com\r
                To: recipient@xcmailr.test\r
                Subject: RFC2231 Attachment Test\r
                MIME-Version: 1.0\r
                Content-Type: multipart/mixed; boundary="BOUNDARY456"\r
                \r
                --BOUNDARY456\r
                Content-Type: text/plain; charset=UTF-8\r
                \r
                Body.\r
                --BOUNDARY456\r
                Content-Type: application/octet-stream\r
                Content-Disposition: attachment; filename*0*=UTF-8''pr%C3%BCf; filename*1*=bericht.pdf\r
                Content-Transfer-Encoding: base64\r
                \r
                cGRmZGF0YQ==\r
                --BOUNDARY456--\r
                """;
            final MimeMessage msg = JakartaMimeMessageParser.createMimeMessage(raw.getBytes(StandardCharsets.UTF_8));
            final JakartaMimeMessageParser parser = new JakartaMimeMessageParser(msg).parse();
            final List<DataSource> attachments = parser.getAttachmentList();
            assertEquals(1, attachments.size());
            assertEquals("prüfbericht.pdf", attachments.get(0).getName());
        }
    }

    @Nested
    @DisplayName("Error and Edge Cases")
    class ErrorAndEdgeCases
    {
        @Test
        @DisplayName("Handle missing or empty subject header gracefully")
        public void testMissingOrEmptySubject() throws Exception
        {
            final String rawNoSubject = """
                From: sender@external.com\r
                To: recipient@xcmailr.test\r
                \r
                Body without subject.
                """;
            final MimeMessage msgNoSubject = JakartaMimeMessageParser.createMimeMessage(rawNoSubject.getBytes(StandardCharsets.UTF_8));
            assertNull(msgNoSubject.getSubject());

            final String rawEmptySubject = """
                From: sender@external.com\r
                To: recipient@xcmailr.test\r
                Subject:    \r
                \r
                Body with blank subject.
                """;
            final MimeMessage msgEmptySubject = JakartaMimeMessageParser.createMimeMessage(rawEmptySubject.getBytes(StandardCharsets.UTF_8));
            final String subject = msgEmptySubject.getSubject();
            assertTrue(subject == null || subject.isBlank());
        }

        @Test
        @DisplayName("Handle missing From header gracefully")
        public void testMissingFromHeader() throws Exception
        {
            final String raw = """
                To: recipient@xcmailr.test\r
                Subject: No From Header\r
                \r
                Body.
                """;
            final MimeMessage msg = JakartaMimeMessageParser.createMimeMessage(raw.getBytes(StandardCharsets.UTF_8));
            assertNull(msg.getFrom());
        }

        @Test
        @DisplayName("Handle truncated or malformed UTF-8 byte sequence without throwing fatal exception")
        public void testMalformedUtf8ByteSequence() throws Exception
        {
            // Byte sequence 0xC3 followed by an ASCII character instead of continuation byte 0x80-0xBF
            final byte[] raw = ("From: sender@external.com\r\n"
                              + "To: recipient@xcmailr.test\r\n"
                              + "Subject: Broken \u00C3 in header\r\n"
                              + "\r\n"
                              + "Body.\r\n").getBytes(StandardCharsets.ISO_8859_1);

            final MimeMessage msg = JakartaMimeMessageParser.createMimeMessage(raw);
            assertNotNull(msg.getSubject());
            assertFalse(msg.getSubject().isEmpty());
        }

        @Test
        @DisplayName("Handle malformed RFC 2047 tokens permissively without failing")
        public void testMalformedRfc2047Token() throws Exception
        {
            // Malformed token missing closing '?='
            final String raw = """
                From: sender@external.com\r
                To: recipient@xcmailr.test\r
                Subject: =?UTF-8?B?bad_base64_string\r
                \r
                Body.
                """;
            final MimeMessage msg = JakartaMimeMessageParser.createMimeMessage(raw.getBytes(StandardCharsets.UTF_8));
            assertNotNull(msg.getSubject());
        }

        @Test
        @DisplayName("Handle long folded UTF-8 header across multiple lines with continuation whitespace")
        public void testFoldedUtf8Header() throws Exception
        {
            final String raw = "From: sender@external.com\r\n"
                             + "To: recipient@xcmailr.test\r\n"
                             + "Subject: Multi-line header with umlauts üöä\r\n"
                             + "\tand continuation with emojis 🚀🎉\r\n"
                             + "\r\n"
                             + "Body.\r\n";
            final MimeMessage msg = JakartaMimeMessageParser.createMimeMessage(raw.getBytes(StandardCharsets.UTF_8));
            final String subject = msg.getSubject();
            assertNotNull(subject);
            assertTrue(subject.contains("üöä"), "Subject should contain umlauts: " + subject);
            assertTrue(subject.contains("🚀🎉"), "Subject should contain emojis: " + subject);
        }
    }
}

