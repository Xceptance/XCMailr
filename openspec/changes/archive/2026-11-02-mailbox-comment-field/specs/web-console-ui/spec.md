# Spec Delta

## ADDED Requirements

### Requirement: Mailbox Comment Management and Dashboard Display
The system SHALL support an optional comment field of up to 255 characters on mailbox creation and editing in the web console, displaying non-blank comments as a subtitle beneath the address in the mailbox dashboard table and enabling case-insensitive search by comment substring.

#### Scenario: Mailbox Creation with Comment and Umlauts (Happy Path)
- **WHEN** an authenticated user enters an address prefix, selects a domain, enters an optional comment containing special characters or German umlauts (e.g. `Projekt Überweisung`), and submits the creation modal
- **THEN** the system saves the mailbox with the comment and renders the comment as a subtitle beneath the mailbox address link in the dashboard table

#### Scenario: Mailbox Creation without Comment (Happy Path)
- **WHEN** an authenticated user creates a mailbox leaving the comment field empty or blank
- **THEN** the system saves the mailbox with a null comment and renders the address row without a comment subtitle

#### Scenario: Edit Existing Mailbox to Add or Update Comment (Happy Path)
- **WHEN** an authenticated user opens the edit modal for an existing mailbox, updates or adds a comment, and submits the form
- **THEN** the system persists the updated comment and refreshes the mailbox row displaying the new comment subtitle

#### Scenario: Edit Existing Mailbox to Clear Comment (Happy Path)
- **WHEN** an authenticated user clears the comment field in the edit modal and submits the form
- **THEN** the system persists a null comment and refreshes the mailbox row with the comment subtitle removed

#### Scenario: Live Search by Comment Substring (Happy Path)
- **WHEN** an authenticated user enters a search term matching part of a mailbox's comment (such as `überweisung` or `checkout`) into the dashboard search bar
- **THEN** the system returns and displays all mailboxes whose comment contains the search substring, matched case-insensitively, alongside mailboxes matching by address or domain

#### Scenario: SQL Injection Prevention in Search Input (Special Case)
- **WHEN** an authenticated user enters potentially malicious SQL injection payloads into the search bar (such as `' OR '1'='1`, `'; DROP TABLE mailboxes; --`, or `UNION SELECT`)
- **THEN** the system binds the search query strictly as a parameterized literal value using JPA query parameters without dynamic string concatenation, safely treating the input as literal text without altering query structure or triggering SQL execution errors

#### Scenario: Comment Length Constraint Enforcement (Error Case)
- **WHEN** an authenticated user attempts to submit a comment exceeding 255 characters in the creation or edit modal
- **THEN** client-side and server-side validation reject the submission, displaying localized validation feedback without saving the invalid comment

### Requirement: Recipient Comment Visibility in Email Inboxes
The system SHALL display the recipient mailbox's comment in email inbox views, rendering a contextual comment indicator alongside recipient badges in the unified cross-mailbox inbox view (`/mails`) and displaying the comment in the single mailbox inbox header (`/mailboxes/{id}/mails`).

#### Scenario: Unified Inbox Displays Comment for Tagged Mailbox (Happy Path)
- **WHEN** an authenticated user views the unified inbox at `/mails` and received messages belong to a mailbox configured with a comment
- **THEN** each matching message row displays the mailbox comment badge or indicator alongside the recipient mailbox address badge in the Message & Recipient column

#### Scenario: Unified Inbox Omits Comment for Untagged Mailbox (Happy Path)
- **WHEN** an authenticated user views the unified inbox at `/mails` and received messages belong to a mailbox with no comment configured
- **THEN** the message row displays only the recipient address badge without an empty comment badge or placeholder

#### Scenario: Single Mailbox Inbox Displays Comment in Header (Happy Path)
- **WHEN** an authenticated user navigates to `/mailboxes/{id}/mails` for a mailbox configured with a comment
- **THEN** the view header renders the comment tag beneath or alongside the full mailbox address

