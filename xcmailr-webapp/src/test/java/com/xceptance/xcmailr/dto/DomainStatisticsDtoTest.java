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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Date;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * Unit tests verifying behavior, field normalization, and getters of {@link DomainStatisticsDto}.
 */
public class DomainStatisticsDtoTest
{
    @Test
    public void testFullConstructorWithDate()
    {
        final Date date = Date.valueOf(LocalDate.of(2026, 11, 11));
        final DomainStatisticsDto dto = new DomainStatisticsDto(date, "example.com", 5L, 10L, 20L);

        assertEquals(date, dto.date());
        assertEquals(date, dto.getDate());
        assertEquals("example.com", dto.domain());
        assertEquals("example.com", dto.getDomain());
        assertEquals(5L, dto.dropCount());
        assertEquals(5L, dto.getDropCount());
        assertEquals(10L, dto.forwardCount());
        assertEquals(10L, dto.getForwardCount());
        assertEquals(20L, dto.storeCount());
        assertEquals(20L, dto.getStoreCount());
        assertEquals(35L, dto.totalCount());
        assertEquals(35L, dto.getTotalCount());
    }

    @Test
    public void testOverloadedConstructorWithoutDate()
    {
        final DomainStatisticsDto dto = new DomainStatisticsDto("test.org", 2L, 3L, 4L);

        assertNull(dto.date());
        assertNull(dto.getDate());
        assertEquals("test.org", dto.domain());
        assertEquals(2L, dto.dropCount());
        assertEquals(3L, dto.forwardCount());
        assertEquals(4L, dto.storeCount());
        assertEquals(9L, dto.totalCount());
    }

    @Test
    public void testNullDomainNormalization()
    {
        final DomainStatisticsDto dto = new DomainStatisticsDto(null, null, 1L, 1L, 1L);
        assertEquals("", dto.domain());
        assertEquals("", dto.getDomain());
        assertNull(dto.date());
    }
}
