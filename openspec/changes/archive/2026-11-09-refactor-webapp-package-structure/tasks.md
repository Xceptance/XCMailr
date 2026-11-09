# Tasks: Modernize xcmailr-webapp Package Structure

## 1. Utilities Migration

- [x] 1.1 Move `HelperUtils.java` and `SizeLimitExceededException.java` from `etc` to `com.xceptance.xcmailr.util`, update package headers, and remove top-level `etc` main directory
- [x] 1.2 Move `HelperUtilsTest.java` from `test/java/etc` to `test/java/com/xceptance/xcmailr/util`, update package header, and verify test execution via `mvn test -Dtest=HelperUtilsTest`

## 2. REST DTO and Error Model Migration

- [x] 2.1 Move `MailData.java`, `MailboxData.java`, and `AttachmentData.java` to `com.xceptance.xcmailr.controllers.restapi` and update package headers
- [x] 2.2 Move `ApiError.java` and `ApiErrors.java` to `com.xceptance.xcmailr.controllers.restapi` and update package headers
- [x] 2.3 Update REST controllers (`MailApiController`, `MailboxApiController`) and tests (`MailboxApiIT`) to use the co-located DTOs, and verify via `mvn test -Dtest=MailboxApiIT,MailApiIT`

## 3. JPA Entities Migration

- [x] 3.1 Move all 9 entity classes (`AbstractEntity`, `Domain`, `Mail`, `MailStatistics`, `MailStatisticsKey`, `MailTransaction`, `MBox`, `Status`, `User`) from `models` to `com.xceptance.xcmailr.models`, update package headers, and remove top-level `models` directory
- [x] 3.2 Update all `import models.*` statements across controllers, services, repositories, security providers, and integration tests to `import com.xceptance.xcmailr.models.*`
- [x] 3.3 Verify JPA entity mapping and repository persistence by running `mvn test -Dtest=EntityMappingTest,RepositoryTests,MailRepositoryTest`

## 4. Spring Boot Scanning Cleanup and Suite Verification

- [x] 4.1 Simplify `XcmailrApplication.java` to remove manual string scan arguments (`scanBasePackages`, `@EntityScan`, `@EnableJpaRepositories`), verify standard Spring Boot hierarchical scanning, and remove any remaining empty legacy directories
- [x] 4.2 Execute full reactor clean build and test suite (`mvn clean test`) to verify all 681 tests pass hermetically
