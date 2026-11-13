# Tasks

## 1. Core Header Normalization

- [x] 1.1 Implement MIME header normalization in `MailDeliveryService.java` when preparing `forwardMsg` (ensure `MIME-Version: 1.0`, set `Content-Type: text/plain; charset=UTF-8` on untyped messages, append `; charset=UTF-8` on single-part text messages lacking charset, preserve explicit charsets and multipart messages). Add complete Javadoc, descriptive code comments, and declare `final` on all parameters and local variables.
- [x] 1.2 Verify compilation with `mvn test-compile` to ensure clean syntax and strong typing.

## 2. Testing Pyramid Suite

- [x] 2.1 Implement happy path unit tests in `MailDeliveryServiceTest.java` verifying that forwarding untyped single-part UTF-8 emails (no Content-Type) and single-part text/html emails without charset declarations outputs explicit `charset=UTF-8` and `MIME-Version: 1.0` headers.
- [x] 2.2 Implement error and edge case unit tests in `MailDeliveryServiceTest.java` verifying that forwarding behaves cleanly on minimal headers or empty bodies, and that inactive/missing mailboxes suppress forwarding without error.
- [x] 2.3 Implement special case unit tests in `MailDeliveryServiceTest.java` verifying that explicit legacy charsets (`charset=ISO-8859-1`), explicit UTF-8 charsets, and multipart messages (`multipart/alternative`) are preserved completely untouched.

## 3. Quality & Regression Verification

- [x] 3.1 Execute full test suite (`mvn test`) across the entire repository to verify that all unit, web, socket, and REST tests pass cleanly with zero regressions.

