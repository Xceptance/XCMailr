/*
 * Copyright (c) 2013-2026 Xceptance Software Technologies GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xceptance.xcmailr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the modernized Spring Boot XCMailr application.
 * Boots the embedded container, configures Virtual Threads, Flyway database migrations,
 * JPA persistence, and inbound SMTP listener services.
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootApplication(scanBasePackages = {
    "com.xceptance.xcmailr",
    "controllers",
    "services"
})
@EntityScan(basePackages = {
    "models",
    "com.xceptance.xcmailr.models"
})
@EnableJpaRepositories(basePackages = {
    "repositories",
    "com.xceptance.xcmailr.repositories"
})
@EnableScheduling
@EnableAsync
@ConfigurationPropertiesScan(basePackages = "com.xceptance.xcmailr.config")
public class XcmailrApplication
{
    /**
     * Application bootstrapping method.
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(final String[] args)
    {
        SpringApplication.run(XcmailrApplication.class, args);
    }
}
