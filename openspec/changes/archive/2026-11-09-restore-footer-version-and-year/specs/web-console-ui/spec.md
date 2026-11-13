# Spec Delta

## MODIFIED Requirements

### Requirement: Corporate Footer and Brand Notice
The system SHALL display a standardized corporate footer on all application views containing the corporate copyright notice with inception-to-current year range and application version, while strictly omitting internal technology stack and framework metadata.

#### Scenario: Standardized Yearless Copyright Display
- **WHEN** any web application view is rendered using the base layout
- **THEN** the footer renders a copyright notice with the inception-to-current year range referencing Xceptance Software Technologies GmbH (e.g. `&copy; 2013-2026 Xceptance Software Technologies GmbH`)
- **AND** the footer renders the application version label and version number (e.g. `Version: 4.0.0`)
- **AND** internal technology stack details (including runtime, framework, and library versions such as Java, Spring Boot, or HTMX) are omitted from the footer display
