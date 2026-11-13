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
package com.xceptance.xcmailr.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Immutable data transfer object representing time-series chart data
 * with aligned time bucket labels, forwarded counts, stored counts, and dropped counts.
 *
 * @param labels list of formatted time bucket labels (e.g. hourly or daily intervals)
 * @param forwarded list of forwarded email counts aligned with {@code labels}
 * @param stored list of locally stored email counts aligned with {@code labels}
 * @param dropped list of dropped email counts aligned with {@code labels}
 * @author Xceptance Software Technologies GmbH
 */
public record ChartDataDto(
    List<String> labels,
    List<Long> forwarded,
    List<Long> stored,
    List<Long> dropped
) implements Serializable
{
    private static final long serialVersionUID = 2L;

    /**
     * Compact constructor ensuring non-null, immutable list copies.
     *
     * @param labels time bucket labels
     * @param forwarded forwarded email counts
     * @param stored stored email counts
     * @param dropped dropped email counts
     */
    public ChartDataDto
    {
        labels = labels != null ? List.copyOf(labels) : List.of();
        forwarded = forwarded != null ? List.copyOf(forwarded) : List.of();
        stored = stored != null ? List.copyOf(stored) : List.of();
        dropped = dropped != null ? List.copyOf(dropped) : List.of();
    }
}
