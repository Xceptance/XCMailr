# Proposal

## Why

During the modernization from the legacy Ninja framework to Spring Boot, the legacy `conf/logback.xml` was removed, leaving the application logging exclusively to standard output (`STDOUT`) without an active file appender or log rotation policy. Without file rotation, operational logs are not archived to disk by default, and in deployments where standard output is redirected to a file, disk storage can grow unbounded until the disk is filled.

Restoring native Spring Boot log rotation re-establishes the legacy behavior: writing to `log/xcmailr.log` with a 10MB per-file threshold, a 10-file history, and a 100MB total size cap. This prevents disk exhaustion while remaining fully configurable via environment variables.

## What Changes

- Configure Spring Boot native file logging in `application.yml` defaulting to `${LOG_FILE:log/xcmailr.log}`.
- Configure Logback rolling policy properties in `application.yml`:
  - `file-name-pattern`: `${LOG_FILE:log/xcmailr.log}.%d{yyyy-MM-dd}.%i`
  - `max-file-size`: `10MB`
  - `max-history`: `10`
  - `total-size-cap`: `100MB`
  - `clean-history-on-start`: `false`
- Configure the file log pattern in `application.yml` to preserve comprehensive diagnostics (timestamp, log level, thread, logger class/line, and message).
- Ensure configuration is environment-friendly: file logging can be redirected or disabled (e.g. setting `LOG_FILE=""` in containerized deployments).
- Add automated verification tests to validate that the rolling file appender and rolling policies are correctly bound and active in the application context.

## Capabilities

### New Capabilities
- `application-logging`: Operational logging infrastructure ensuring diagnostic messages are formatted, output to console and rotating file storage, and bounded to prevent disk exhaustion.

### Modified Capabilities

## Impact

- Runtime: Creates the `log/` directory and writes `log/xcmailr.log` with rotated archives `log/xcmailr.log.YYYY-MM-DD.i` during application execution.
- Storage: Log disk consumption is strictly bounded to $\le 100\text{ MB}$.
- Configuration: Managed entirely via native Spring Boot properties in `application.yml` without introducing raw XML files.

