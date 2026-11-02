# Tasks

## 1. Template and Localization Cleanup

- [x] 1.1 Remove the tech stack summary paragraph from `xcmailr-webapp/src/main/resources/templates/layout/base.html` and verify the footer renders only the yearless copyright notice
- [x] 1.2 Remove `layout.footer.summary` from `messages.properties` and `messages_de.properties` and verify that the message files contain no obsolete footer summary keys

## 2. Test Suite Updates and Regression Verification

- [x] 2.1 Update `BaseLayoutFooterContractTest` to assert that internal technology stack strings (`XCMailr Modernized`, `Java 25`, `Spring Boot`, `HTMX`) are absent across public, authenticated, and error views, and verify tests pass with `mvn test -Dtest=BaseLayoutFooterContractTest`
- [x] 2.2 Update `AuthWebControllerTest` login view assertions to check for corporate branding instead of the tech stack line, and verify tests pass with `mvn test -Dtest=AuthWebControllerTest`
- [x] 2.3 Run the full controller and layout test suite with `mvn test -Dtest="*WebControllerTest,*ContractTest"` and verify all tests pass

