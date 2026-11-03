package com.xceptance.xcmailr.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;

/**
 * Tests verifying that native Spring Boot logging configuration initializes
 * the expected Logback {@link RollingFileAppender} with size-and-time based rolling
 * limits matching the legacy Ninja framework configuration (10MB max file size,
 * 10 historical archives, 100MB total size cap).
 */
@SpringBootTest
@ActiveProfiles("test")
class LoggingConfigurationTest
{
    @Test
    @DisplayName("Should configure RollingFileAppender with size and history constraints")
    void shouldConfigureRollingFileAppenderWithConstraints()
    {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        assertNotNull(context, "LoggerContext must not be null");

        final Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        assertNotNull(rootLogger, "Root logger must not be null");

        final Appender<ILoggingEvent> appender = rootLogger.getAppender("FILE");
        assertNotNull(appender, "A 'FILE' appender must be registered on the root logger");
        assertTrue(appender instanceof RollingFileAppender, "FILE appender must be a RollingFileAppender");

        final RollingFileAppender<ILoggingEvent> rollingAppender = (RollingFileAppender<ILoggingEvent>) appender;
        assertNotNull(rollingAppender.getFile(), "Target log file must be configured");
        assertTrue(rollingAppender.getFile().endsWith("log/xcmailr.log"),
            "Log file path should end with log/xcmailr.log, but was: " + rollingAppender.getFile());

        assertTrue(rollingAppender.getRollingPolicy() instanceof SizeAndTimeBasedRollingPolicy,
            "Rolling policy must be SizeAndTimeBasedRollingPolicy");

        @SuppressWarnings("unchecked")
        final SizeAndTimeBasedRollingPolicy<ILoggingEvent> policy =
            (SizeAndTimeBasedRollingPolicy<ILoggingEvent>) rollingAppender.getRollingPolicy();

        // Verify file pattern
        assertTrue(policy.getFileNamePattern().contains("%d{yyyy-MM-dd}.%i"),
            "File name pattern must include date and index: " + policy.getFileNamePattern());

        // Verify max file size (10MB)
        final FileSize maxFileSize = (FileSize) ReflectionTestUtils.getField(policy, "maxFileSize");
        assertNotNull(maxFileSize, "maxFileSize must not be null");
        assertEquals(10L * 1024L * 1024L, maxFileSize.getSize(),
            "Max file size must be 10MB (10485760 bytes)");

        // Verify max history (10 archives)
        assertEquals(10, policy.getMaxHistory(),
            "Max history must be 10 files");

        // Verify total size cap (100MB)
        final FileSize totalSizeCap = (FileSize) ReflectionTestUtils.getField(policy, "totalSizeCap");
        assertNotNull(totalSizeCap, "totalSizeCap must not be null");
        assertEquals(100L * 1024L * 1024L, totalSizeCap.getSize(),
            "Total size cap must be 100MB (104857600 bytes)");
    }
}
