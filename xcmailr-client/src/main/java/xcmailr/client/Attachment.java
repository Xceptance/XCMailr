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
package xcmailr.client;

/**
 * The data object that represents the details of a mail attachment.
 * 
 * @see Mail
 * @see MailApi
 */
public class Attachment
{
    /**
     * The name of the attachment.
     */
    public String name;

    /**
     * The content type of the attachment.
     */
    public String contentType;

    /**
     * The size (in bytes) of the attachment.
     */
    public int size;

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("%s:{ name: '%s', contentType: '%s', size: '%d' }", super.toString(), name, contentType, size);
    }
}
