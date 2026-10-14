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
package com.xceptance.xcmailr.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.xceptance.xcmailr.repositories.DomainRepository;
import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailTransactionRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

import models.Domain;
import models.MBox;
import models.User;

/**
 * Integration test verifying that concurrent email delivery invocations to the same recipient mailbox
 * atomically update forward and suppression counters without throwing
 * {@link org.springframework.orm.ObjectOptimisticLockingFailureException}.
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest
@ActiveProfiles("test")
public class ConcurrentMailDeliveryIT
{
    @Autowired
    private MailDeliveryService mailDeliveryService;

    @Autowired
    private MailboxRepository mailboxRepository;

    @Autowired
    private MailRepository mailRepository;

    @Autowired
    private MailTransactionRepository mailTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DomainRepository domainRepository;

    private User testUser;
    private ExecutorService executorService;

    @BeforeEach
    public void setUp()
    {
        mailRepository.deleteAll();
        mailTransactionRepository.deleteAll();
        mailboxRepository.deleteAll();
        userRepository.deleteAll();
        domainRepository.deleteAll();

        final Domain domain = new Domain("xcmailr.test");
        domainRepository.save(domain);

        testUser = new User("Concurrent", "Tester", "forward_target@realdomain.test", "secret", "en");
        testUser.setActive(true);
        userRepository.save(testUser);

        executorService = Executors.newFixedThreadPool(20);
    }

    @AfterEach
    public void tearDown()
    {
        if (executorService != null)
        {
            executorService.shutdownNow();
        }
    }

    @Test
    @DisplayName("Concurrent deliveries to active mailbox increment forwards atomically without optimistic locking errors")
    public void testConcurrentDeliveriesIncrementForwardsWithoutOptimisticLockingFailure() throws Exception
    {
        final MBox activeBox = new MBox("concurrent-fwd", "xcmailr.test", 0L, false, testUser);
        activeBox.setActive(true);
        activeBox.setForwardEmails(true);
        mailboxRepository.save(activeBox);

        final int concurrency = 20;
        final CountDownLatch readyLatch = new CountDownLatch(concurrency);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < concurrency; i++)
        {
            final int index = i;
            futures.add(executorService.submit(() -> {
                readyLatch.countDown();
                try
                {
                    startLatch.await();
                    final String rawMessage = "From: sender" + index + "@example.com\r\n"
                        + "To: concurrent-fwd@xcmailr.test\r\n"
                        + "Subject: Concurrent Message " + index + "\r\n\r\n"
                        + "Body payload " + index;
                    final ByteArrayInputStream stream = new ByteArrayInputStream(rawMessage.getBytes(StandardCharsets.UTF_8));
                    mailDeliveryService.deliver("sender" + index + "@example.com", "concurrent-fwd@xcmailr.test", stream);
                }
                catch (final Exception e)
                {
                    throw new RuntimeException("Concurrent delivery failed for index " + index, e);
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (final Future<?> future : futures)
        {
            future.get(10, TimeUnit.SECONDS);
        }

        final MBox reloaded = mailboxRepository.findById(activeBox.getId()).orElseThrow();
        assertEquals(concurrency, reloaded.getForwards(),
                     "Forward counter must equal total concurrent deliveries without lost updates");
        assertEquals(concurrency, mailRepository.findByMailboxOrderByReceiveTimeAsc(activeBox.getId()).size(),
                     "All concurrent emails must be persisted in database");
    }

    @Test
    @DisplayName("Concurrent deliveries to inactive mailbox increment suppressions atomically without optimistic locking errors")
    public void testConcurrentDeliveriesIncrementSuppressionsWithoutOptimisticLockingFailure() throws Exception
    {
        final MBox inactiveBox = new MBox("concurrent-sup", "xcmailr.test", 0L, true, testUser);
        inactiveBox.setActive(false);
        mailboxRepository.save(inactiveBox);

        final int concurrency = 20;
        final CountDownLatch readyLatch = new CountDownLatch(concurrency);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < concurrency; i++)
        {
            final int index = i;
            futures.add(executorService.submit(() -> {
                readyLatch.countDown();
                try
                {
                    startLatch.await();
                    final String rawMessage = "From: sender" + index + "@example.com\r\n"
                        + "To: concurrent-sup@xcmailr.test\r\n"
                        + "Subject: Concurrent Suppressed " + index + "\r\n\r\n"
                        + "Body payload " + index;
                    final ByteArrayInputStream stream = new ByteArrayInputStream(rawMessage.getBytes(StandardCharsets.UTF_8));
                    mailDeliveryService.deliver("sender" + index + "@example.com", "concurrent-sup@xcmailr.test", stream);
                }
                catch (final Exception e)
                {
                    throw new RuntimeException("Concurrent delivery failed for index " + index, e);
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (final Future<?> future : futures)
        {
            future.get(10, TimeUnit.SECONDS);
        }

        final MBox reloaded = mailboxRepository.findById(inactiveBox.getId()).orElseThrow();
        assertEquals(concurrency, reloaded.getSuppressions(),
                     "Suppression counter must equal total concurrent deliveries without lost updates");
        assertEquals(0, mailRepository.findByMailboxOrderByReceiveTimeAsc(inactiveBox.getId()).size(),
                     "No emails must be persisted for inactive mailbox");
    }

    @Test
    @DisplayName("Concurrent deliveries to mailbox with inactive owner increment suppressions atomically")
    public void testConcurrentDeliveriesInactiveUserIncrementsSuppressions() throws Exception
    {
        final User inactiveUser = new User("Disabled", "Owner", "inactive_user@realdomain.test", "secret", "en");
        inactiveUser.setActive(false);
        userRepository.save(inactiveUser);

        final MBox box = new MBox("inactive-user-box", "xcmailr.test", 0L, false, inactiveUser);
        box.setActive(true);
        mailboxRepository.save(box);

        final int concurrency = 20;
        final CountDownLatch readyLatch = new CountDownLatch(concurrency);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < concurrency; i++)
        {
            final int index = i;
            futures.add(executorService.submit(() -> {
                readyLatch.countDown();
                try
                {
                    startLatch.await();
                    final String rawMessage = "From: sender" + index + "@example.com\r\n"
                        + "To: inactive-user-box@xcmailr.test\r\n"
                        + "Subject: Concurrent Inactive User " + index + "\r\n\r\n"
                        + "Body payload " + index;
                    final ByteArrayInputStream stream = new ByteArrayInputStream(rawMessage.getBytes(StandardCharsets.UTF_8));
                    mailDeliveryService.deliver("sender" + index + "@example.com", "inactive-user-box@xcmailr.test", stream);
                }
                catch (final Exception e)
                {
                    throw new RuntimeException("Concurrent delivery failed for index " + index, e);
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (final Future<?> future : futures)
        {
            future.get(10, TimeUnit.SECONDS);
        }

        final MBox reloaded = mailboxRepository.findById(box.getId()).orElseThrow();
        assertEquals(concurrency, reloaded.getSuppressions(),
                     "Suppression counter must equal total concurrent deliveries for inactive owner");
        assertEquals(0, mailRepository.findByMailboxOrderByReceiveTimeAsc(box.getId()).size(),
                     "No emails must be persisted when owner is inactive");
    }
}

