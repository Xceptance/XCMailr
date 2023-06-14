/*
 * Copyright (c) 2013-2023 Xceptance Software Technologies GmbH
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
package services;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import etc.HelperUtils;

public class MessageListenerRawContentTest
{
    @Test
    public void testReadRawContent() throws Exception
    {
        final InputStream is = getClass().getResourceAsStream("/testmails/multiPart.eml");
        Assert.assertNotNull("Failed to load 'multiPart.eml'", is);

        final byte[] rawContent = HelperUtils.readLimitedAmount(is, 25_000_000);
        Assert.assertNotNull("RAW content is null", rawContent);

        final String raw = new String(rawContent, StandardCharsets.UTF_8);

        Assert.assertTrue("CRLF not found", raw.indexOf("\r\n") > -1);
        Assert.assertTrue("'Content-Type' header not found",
                          Pattern.compile("\r\nContent-Type:\\s*multipart/\\S+;").matcher(raw).find());
        Assert.assertTrue("'Subject' header not found", Pattern.compile("\r\nSubject:\\s*\\S+").matcher(raw).find());
    }
}
