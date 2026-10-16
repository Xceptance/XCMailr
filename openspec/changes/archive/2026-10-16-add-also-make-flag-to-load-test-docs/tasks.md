# Tasks

## 1. Documentation Updates

- [x] 1.1 Update `docs/load-testing-guide.md` to append `-am` to the build and packaging command (`mvn clean test-compile package -pl xcmailr-load-test-suite -am`) and the troubleshooting rebuild command (`mvn clean package -pl xcmailr-load-test-suite -am`), verifying markdown formatting.
- [x] 1.2 Update `xcmailr-load-test-suite/README.md` to append `-am` to the Maven test commands (`mvn test -pl xcmailr-load-test-suite -am ...`) for `SendMail`, `CreateAndDeleteMailbox`, and `SendAndCheckMail`, verifying markdown formatting.

## 2. Verification

- [x] 2.1 Execute `mvn clean test-compile package -pl xcmailr-load-test-suite -am` from the repository root and verify that Maven resolves the reactor dependencies and reports `BUILD SUCCESS`.

