# Spec Delta

## Purpose

Provides a responsive, server-driven web management console using Thymeleaf and HTMX for self-service mailbox operations, email inspection, and system administration.

## ADDED Requirements

### Requirement: User Authentication and Self-Service
The system SHALL provide web views for user registration, email verification, session login, password reset, and profile management.

#### Scenario: User Login and Session Management
- **WHEN** a user enters valid email and password credentials on the login page
- **THEN** the system creates an authenticated HTTP session and redirects to the mailbox dashboard

#### Scenario: User Registration and Email Activation
- **WHEN** a new user completes the registration form
- **THEN** an activation token is generated, confirmation email is dispatched, and account remains inactive until confirmed

### Requirement: Dynamic Mailbox Dashboard
The system SHALL render an interactive mailbox dashboard supporting search, inline editing, modal dialogs, and bulk actions via HTMX without full page reloads.

#### Scenario: Live Search and Filter
- **WHEN** the user types into the mailbox filter input
- **THEN** HTMX triggers an asynchronous partial update of the mailbox table matching the filter criteria

#### Scenario: Modal Dialog Actions
- **WHEN** the user clicks to edit, delete, or extend expiration on a mailbox
- **THEN** HTMX loads the modal fragment dynamically and updates the table row upon successful submission

### Requirement: Admin Management Console
The system SHALL provide administrative interfaces for managing user accounts, reviewing transaction logs, analyzing traffic metrics, and maintaining the domain whitelist.

#### Scenario: Domain Whitelist Configuration
- **WHEN** an administrator adds or removes an allowed domain via the admin console
- **THEN** the system updates the registered domain table and enforces the change on the inbound SMTP listener immediately

#### Scenario: Paged Transaction Auditing
- **WHEN** an administrator inspects the mail transaction log
- **THEN** the system displays paginated records with filtering by timestamp, status, and address

