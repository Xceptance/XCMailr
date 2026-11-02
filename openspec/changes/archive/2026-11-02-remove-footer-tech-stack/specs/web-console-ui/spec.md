# Spec Delta

## MODIFIED Requirements

### Requirement: Corporate Footer and Brand Notice
The system SHALL display a standardized corporate footer on all application views containing a yearless copyright notice, preventing recurring annual copyright maintenance churn and omitting internal technology stack metadata.

#### Scenario: Standardized Yearless Copyright Display
- **WHEN** any web application view is rendered using the base layout
- **THEN** the footer renders a yearless copyright notice referencing Xceptance Software Technologies GmbH without hardcoded year ranges
- **AND** internal technology stack details (including runtime, framework, and library versions) are omitted from the footer display

