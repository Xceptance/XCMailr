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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.xceptance.xcmailr.models.MailTransaction;

/**
 * Unit tests for {@link MailTransactionQueueService}.
 * Verifies FIFO enqueue/drain behaviors, defensive null handling,
 * and high-concurrency multi-threaded access adhering to the testing pyramid.
 *
 * @author Xceptance Software Technologies GmbH
 */
public class MailTransactionQueueServiceTest
{
    private MailTransactionQueueService queueService;

    @BeforeEach
    public void setUp()
    {
        queueService = new MailTransactionQueueService();
    }

    @Test
    @DisplayName("Happy Path: Enqueue and FIFO drain of transactions")
    public void testEnqueueAndDrainFifo()
    {
        assertTrue(queueService.isEmpty(), "Queue should initially be empty");
        assertEquals(0, queueService.size(), "Initial size should be 0");

        final MailTransaction tx1 = new MailTransaction(250, "alice@sender.test", "box1@xcmailr.test", null);
        final MailTransaction tx2 = new MailTransaction(300, "bob@sender.test", "box2@xcmailr.test", "forward@target.test");
        final MailTransaction tx3 = new MailTransaction(100, "charlie@sender.test", "unknown@xcmailr.test", null);

        queueService.enqueue(tx1);
        queueService.enqueue(tx2);
        queueService.enqueue(tx3);

        assertFalse(queueService.isEmpty(), "Queue should not be empty after enqueues");
        assertEquals(3, queueService.size(), "Queue size should be 3");

        final List<MailTransaction> drained = queueService.drainAll();
        assertNotNull(drained, "Drained list should not be null");
        assertEquals(3, drained.size(), "Drained list should contain 3 items");

        // Verify strict FIFO order
        assertEquals(tx1, drained.get(0), "First drained item must be tx1");
        assertEquals(tx2, drained.get(1), "Second drained item must be tx2");
        assertEquals(tx3, drained.get(2), "Third drained item must be tx3");

        // Verify queue is completely empty after drain
        assertTrue(queueService.isEmpty(), "Queue must be empty after drain");
        assertEquals(0, queueService.size(), "Queue size must be 0 after drain");
    }

    @Test
    @DisplayName("Error Case: Null transaction handling without exception")
    public void testNullEnqueueHandledGracefully()
    {
        queueService.enqueue(null);

        assertTrue(queueService.isEmpty(), "Queue should remain empty after enqueuing null");
        assertEquals(0, queueService.size(), "Queue size should remain 0");

        final List<MailTransaction> drained = queueService.drainAll();
        assertNotNull(drained, "Drained list should not be null");
        assertTrue(drained.isEmpty(), "Drained list should be empty");
    }

    @Test
    @DisplayName("Special Case: Drain and clear on empty queue")
    public void testDrainAndClearOnEmptyQueue()
    {
        final List<MailTransaction> drained = queueService.drainAll();
        assertNotNull(drained);
        assertTrue(drained.isEmpty());

        final MailTransaction tx = new MailTransaction(250, "test@test.de", "box@xcmailr.test", null);
        queueService.enqueue(tx);
        assertEquals(1, queueService.size());

        queueService.clear();
        assertTrue(queueService.isEmpty());
        assertEquals(0, queueService.size());
    }

    @Test
    @DisplayName("Special Case: Multi-threaded concurrent enqueue and drain operations")
    public void testConcurrentEnqueueAndDrain() throws InterruptedException
    {
        final int threadCount = 10;
        final int itemsPerThread = 100;
        final int expectedTotal = threadCount * itemsPerThread;

        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++)
        {
            final int threadId = i;
            executor.submit(() -> {
                try
                {
                    startLatch.await();
                    for (int j = 0; j < itemsPerThread; j++)
                    {
                        final MailTransaction tx = new MailTransaction(250,
                                                                       "user" + threadId + "_" + j + "@domain.test",
                                                                       "box@xcmailr.test",
                                                                       null);
                        queueService.enqueue(tx);
                    }
                }
                catch (final InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                finally
                {
                    doneLatch.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startLatch.countDown();
        final boolean completed = doneLatch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "All threads should complete within timeout");
        executor.shutdown();

        assertEquals(expectedTotal, queueService.size(), "Queue should contain all concurrently enqueued items");

        final List<MailTransaction> drained = queueService.drainAll();
        assertEquals(expectedTotal, drained.size(), "All items must be drained without loss");
        assertTrue(queueService.isEmpty(), "Queue must be empty after full drain");
    }
}
