# Design

## Context

See proposal.md for motivation.

In the legacy Ninja implementation, logging was configured via `conf/logback.xml` with a `RollingFileAppender` targeting `log/xcmailr.log`. The rolling policy used `FixedWindowRollingPolicy` with `<maxIndex>10</maxIndex>` and `SizeBasedTriggeringPolicy` with `<MaxFileSize>10MB</MaxFileSize>`, ensuring that disk space was bounded to approximately 100MB ($10 \times 10\text{ MB}$).

During the modernization cleanup, `conf/logback.xml` was removed, leaving Spring Boot to output exclusively to the console (`STDOUT`). Spring Boot's default logging subsystem provides built-in support for Logback rolling file appenders via standard application properties without requiring custom XML files.

## Goals / Non-Goals

**Goals:**
- Configure rotating file logging via native Spring Boot properties in `application.yml`.
- Replicate legacy rotation thresholds: 10MB file limit, 10 archive history, and 100MB total size cap.
- Direct output to `log/xcmailr.log` by default, overridable via `${LOG_FILE}`.
- Format file log records with timestamp, severity, thread, logger name with line number, and message.
- Verify through automated tests that the `RollingFileAppender` is configured with the expected rolling policy and constraints.

**Non-Goals:**
- Introducing XML configuration files (`logback.xml` or `logback-spring.xml`).
- Altering console logging behavior or eliminating console log emission.
- Modifying business service log levels beyond existing configurations.

## Decisions

### Decision 1: Use Native Spring Boot Application Properties

**Choice:** Configure `logging.file.name` and `logging.logback.rollingpolicy.*` directly in `xcmailr-webapp/src/main/resources/application.yml`.

**Rationale:**
- Keeps configuration declarative and centralized in YAML alongside other Spring Boot properties.
- Eliminates XML maintenance overhead.
- Spring Boot automatically initializes a `RollingFileAppender` with `SizeAndTimeBasedRollingPolicy` when `logging.file.name` is specified.
- Environment variable overrides (e.g. `LOG_FILE`) work out-of-the-box.

**Alternatives Considered:**
- `logback-spring.xml`: Provides granular XML appender setup, but adds boilerplate and separates logging from central YAML configuration.

### Decision 2: Rolling Policy Property Specification

**Choice:**
```yaml
logging:
  file:
    name: ${LOG_FILE:log/xcmailr.log}
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36}:%line - %msg%n"
  logback:
    rollingpolicy:
      file-name-pattern: ${LOG_FILE:log/xcmailr.log}.%d{yyyy-MM-dd}.%i
      max-file-size: 10MB
      max-history: 10
      total-size-cap: 100MB
      clean-history-on-start: false
```

**Rationale:**
- `file-name-pattern`: Includes both the date and the index (`%i`), allowing multiple rotated files per day up to the 10-file history limit.
- `max-file-size: 10MB`: Matches legacy's 10MB threshold.
- `max-history: 10`: Matches legacy's `maxIndex: 10`.
- `total-size-cap: 100MB`: Hard ceiling ensuring total disk consumption does not exceed 100MB.

### Decision 3: Automated Verification Strategy

**Choice:** Add an automated JUnit 5 test (`LoggingConfigurationTest`) that loads the Spring context or inspects the Logback `LoggerContext`.

**Rationale:**
- Retrieves the root logger appenders to verify that `RollingFileAppender` is present.
- Inspects the rolling policy to assert that `maxFileSize` is 10MB, `maxHistory` is 10, and `totalSizeCap` is 100MB.
- Guarantees future refactorings do not unintentionally drop the file appender or rolling constraints.

## Risks / Trade-offs

- **[Working Directory Dependency]** Default path `log/xcmailr.log` creates a directory relative to the process working directory.  
  → *Mitigation:* Property is wrapped in `${LOG_FILE:log/xcmailr.log}`, allowing containers or systemd units to specify absolute paths (e.g. `/var/log/xcmailr/xcmailr.log`) or disable file logging if stdout streaming is preferred.
- **[Archive Naming Pattern]** Legacy used `log/xcmailr.log.1` (FixedWindow), whereas modern Logback SizeAndTimeBased policy uses `log/xcmailr.log.YYYY-MM-DD.1`.  
  → *Mitigation:* Date-and-index naming is standard in modern logging frameworks and provides clearer chronological ordering without altering total file count or size constraints.

