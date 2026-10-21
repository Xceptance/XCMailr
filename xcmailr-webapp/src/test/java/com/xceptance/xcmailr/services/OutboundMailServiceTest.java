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
package com.xceptance.xcmailr.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.mail.Session;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.xceptance.xcmailr.config.XcmailrProperties;

/**
 * Unit tests verifying that {@link OutboundMailService} correctly configures
 * outbound mail sessions with UTF-8 MIME properties and SMTP parameters.
 *
 * @author Xceptance Software Technologies GmbH
 */
public class OutboundMailServiceTest
{
    @Test
    @DisplayName("Verify createSession populates UTF-8 MIME properties")
    public void testCreateSessionPopulatesUtf8MimeProperties()
    {
        final XcmailrProperties props = new XcmailrProperties();
        final OutboundMailService service = new OutboundMailService(props);

        final Session session = service.createSession();
        assertNotNull(session);
        assertEquals("true", session.getProperty("mail.mime.allowutf8"));
        assertEquals("UTF-8", session.getProperty("mail.mime.charset"));
        assertEquals("true", session.getProperty("mail.mime.decodefilename"));
        assertEquals("false", session.getProperty("mail.mime.decodetext.strict"));
        assertEquals("smtp", session.getProperty("mail.transport.protocol"));
    }
}

