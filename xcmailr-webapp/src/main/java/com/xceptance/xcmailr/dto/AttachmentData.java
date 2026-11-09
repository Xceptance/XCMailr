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
package com.xceptance.xcmailr.dto;

import java.io.IOException;

import jakarta.activation.DataSource;

/**
 * Data transfer object representing the metadata of an email attachment
 * returned by the REST API.
 */
public class AttachmentData
{
    /**
     * The filename of the attachment.
     */
    public final String name;

    /**
     * The MIME content type of the attachment (e.g. {@code application/pdf}, {@code image/png}).
     */
    public final String contentType;

    /**
     * The size of the attachment content in bytes.
     */
    public final int size;

    /**
     * Constructs an {@code AttachmentData} DTO extracting metadata from an activation {@link DataSource}.
     *
     * @param attachment the MIME message attachment data source, must not be {@code null}
     * @throws IOException if an error occurs reading the attachment input stream length
     */
    public AttachmentData(final DataSource attachment) throws IOException
    {
        name = attachment.getName();
        contentType = attachment.getContentType();
        size = attachment.getInputStream().available();
    }
}
