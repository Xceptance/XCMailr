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
package com.xceptance.xcmailr.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import models.User;

/**
 * Spring Data JPA repository for user accounts.
 *
 * @author Xceptance Software Technologies GmbH
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>
{
    /**
     * Finds a user by their registered email address, ignoring case.
     *
     * @param mail email address to search for
     * @return an {@link Optional} containing the user if found
     */
    Optional<User> findByMailIgnoreCase(final String mail);

    /**
     * Checks if a user exists with the given email address, ignoring case.
     *
     * @param mail email address to verify
     * @return true if an account exists with this email
     */
    boolean existsByMailIgnoreCase(final String mail);

    /**
     * Finds a user by their account activation / password reset confirmation token.
     *
     * @param confirmation confirmation token string
     * @return an {@link Optional} containing the user if found
     */
    Optional<User> findByConfirmation(final String confirmation);

    /**
     * Finds a user by their API authentication token.
     *
     * @param apiToken alphanumeric API token
     * @return an {@link Optional} containing the user if found
     */
    Optional<User> findByApiToken(final String apiToken);

    /**
     * Finds inactive user accounts whose confirmation token has expired before the specified timestamp.
     *
     * @param timestamp epoch millisecond expiration limit
     * @return list of expired inactive users eligible for cleanup
     */
    @Query("SELECT u FROM User u WHERE u.active = false AND u.confirmation IS NOT NULL AND u.ts_confirm < :timestamp")
    List<User> findExpiredInactiveUsers(@Param("timestamp") final Long timestamp);

    /**
     * Finds active users whose API token was created before the specified timestamp.
     *
     * @param timestamp epoch millisecond expiration cutoff
     * @return list of users whose API tokens have expired
     */
    @Query("SELECT u FROM User u WHERE u.apiToken IS NOT NULL AND u.apiTokenCreationTimestamp > 0 AND u.apiTokenCreationTimestamp < :timestamp")
    List<User> findUsersWithExpiredApiTokens(@Param("timestamp") final Long timestamp);

    /**
     * Retrieves a page of all users ordered by email ascending.
     *
     * @param pageable pagination parameters
     * @return page of users
     */
    Page<User> findAllByOrderByMailAsc(final Pageable pageable);

    /**
     * Searches users by email, forename, or surname substring matching.
     *
     * @param search search term
     * @param pageable pagination parameters
     * @return matching page of users
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.mail) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.forename) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.surname) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY u.mail ASC")
    Page<User> searchUsers(@Param("search") final String search, final Pageable pageable);
}
