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
package com.xceptance.xcmailr.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.xceptance.xcmailr.repositories.UserRepository;

/**
 * Spring Security configuration for XCMailr.
 * <p>
 * Configures dual security filter chains:
 * <ul>
 *   <li>Stateless REST API chain ({@code /api/**}) secured via Bearer tokens</li>
 *   <li>Stateful Web chain ({@code /**}) secured via session-based form login and CSRF protection</li>
 * </ul>
 * Also configures {@link BCryptPasswordEncoder} for backwards-compatible password hashing with legacy accounts.
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig
{
    /**
     * Password encoder compatible with legacy BCrypt hashes stored in the database.
     *
     * @return standard {@link BCryptPasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder()
    {
        return new BCryptPasswordEncoder();
    }

    /**
     * Resolves user accounts by email address for authentication.
     *
     * @param userRepository repository to fetch users
     * @return user details service
     */
    @Bean
    public UserDetailsService userDetailsService(final UserRepository userRepository)
    {
        return username -> userRepository.findByMailIgnoreCase(username)
                                         .map(UserPrincipal::new)
                                         .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    /**
     * Security filter chain for REST API endpoints ({@code /api/**}).
     * Uses stateless sessions, disables CSRF, and authenticates via Bearer API tokens.
     *
     * @param http HTTP security builder
     * @param userRepository repository used by the token filter
     * @return security filter chain
     * @throws Exception on configuration error
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(final HttpSecurity http,
                                                     final UserRepository userRepository) throws Exception
    {
        http.securityMatcher("/api/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .addFilterBefore(new ApiTokenAuthenticationFilter(userRepository), UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            );

        return http.build();
    }

    /**
     * Security filter chain for web pages, assets, and standard form login ({@code /**}).
     *
     * @param http HTTP security builder
     * @return security filter chain
     * @throws Exception on configuration error
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(final HttpSecurity http) throws Exception
    {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/login",
                    "/register",
                    "/confirm/**",
                    "/forgot-password",
                    "/pwreset/**",
                    "/error",
                    "/actuator/health",
                    "/assets/**",
                    "/webjars/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico"
                ).permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("mail")
                .passwordParameter("password")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .csrf(Customizer.withDefaults());

        return http.build();
    }
}
