# Application Logging Specification

## Purpose

Provides operational logging facilities to emit diagnostic events to console and disk, rotating log archives to prevent unbounded storage consumption.

## Requirements

### Requirement: Rotating File Log Appender
The system SHALL write application log events to a local file target defaulting to `log/xcmailr.log` alongside console output.

#### Scenario: Default log file destination
- **WHEN** the application starts with default configuration
- **THEN** the logging subsystem writes log entries to `log/xcmailr.log` while continuing to stream log entries to STDOUT

#### Scenario: Configurable log file destination via environment
- **WHEN** the `LOG_FILE` environment variable or configuration property is overridden
- **THEN** the logging subsystem targets the specified file path for file logging

### Requirement: Size-Based Log Rotation and History Bounds
The system SHALL rotate the active log file when it reaches 10MB, retain up to 10 historical archive files, and enforce a strict total log storage cap of 100MB to prevent disk exhaustion.

#### Scenario: File size limit rotation
- **WHEN** the active log file reaches 10MB in size
- **THEN** the logging subsystem rolls the active file into an indexed archive matching `${LOG_FILE}.%d{yyyy-MM-dd}.%i`

#### Scenario: Archive retention cap
- **WHEN** the number of archived log files exceeds 10 or total archived size exceeds 100MB
- **THEN** the oldest archive files are deleted so that total log disk usage does not exceed 100MB

### Requirement: Structured Diagnostic Log Formatting
The system SHALL format log file entries with timestamp, log severity level, executing thread, logger class, and message text.

#### Scenario: Diagnostic log entry format
- **WHEN** a log event is emitted to the file appender
- **THEN** the output line contains the timestamp, log level, thread name, logger category with line number, and message

