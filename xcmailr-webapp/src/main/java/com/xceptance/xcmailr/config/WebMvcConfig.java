/*
 * Copyright (c) Xceptance Software Technologies GmbH
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
package com.xceptance.xcmailr.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.xceptance.xcmailr.repositories.UserRepository;

/**
 * Spring MVC configuration for static resource locations, WebJar asset resolution, and user locale resolution.
 *
 * @author Xceptance Software Technologies GmbH
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer
{
    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry)
    {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/assets/");

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * Registers the application's {@link LocaleResolver} bean to honor user profile language preferences.
     *
     * @param userRepositoryProvider provider for {@link UserRepository} to support username lookups without circular dependencies
     * @return the configured {@link UserPreferenceLocaleResolver}
     */
    @Bean
    public LocaleResolver localeResolver(final ObjectProvider<UserRepository> userRepositoryProvider)
    {
        return new UserPreferenceLocaleResolver(userRepositoryProvider.getIfAvailable());
    }
}
