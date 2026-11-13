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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.xceptance.xcmailr.models.MailTransaction;

/**
 * Asynchronous in-memory buffer service for SMTP transaction audit records.
 * <p>
 * Decouples the hot inbound SMTP delivery path from synchronous database persistence
 * latency and database connection pool contention. Transactions are buffered lock-free in a
 * {@link ConcurrentLinkedQueue} and subsequently drained by scheduled batch jobs or shutdown hooks.
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
@Service
public class MailTransactionQueueService
{
    private static final Logger LOG = LoggerFactory.getLogger(MailTransactionQueueService.class);

    /**
     * Non-blocking unbounded queue holding transactions awaiting batch database ingestion.
     */
    private final ConcurrentLinkedQueue<MailTransaction> queue = new ConcurrentLinkedQueue<>();

    /**
     * Enqueues a transaction into the in-memory buffer in a non-blocking, thread-safe manner.
     * Null transactions are safely ignored to guard against caller errors.
     *
     * @param transaction the transaction to buffer; ignored if null
     */
    public void enqueue(final MailTransaction transaction)
    {
        if (transaction == null)
        {
            LOG.warn("Attempted to enqueue a null transaction; ignoring.");
            return;
        }

        // Lock-free insertion into queue
        queue.offer(transaction);
    }

    /**
     * Drains all pending transactions currently in the queue into a snapshot list in FIFO order.
     * Each drained transaction is removed from the internal queue, guaranteeing exactly-once delivery.
     *
     * @return unmodifiable list containing all drained transactions, or an empty list if queue was empty
     */
    public List<MailTransaction> drainAll()
    {
        final List<MailTransaction> drained = new ArrayList<>();
        MailTransaction tx;
        while ((tx = queue.poll()) != null)
        {
            drained.add(tx);
        }

        return Collections.unmodifiableList(drained);
    }

    /**
     * Returns the approximate number of transactions currently buffered in the queue.
     *
     * @return current queue size
     */
    public int size()
    {
        return queue.size();
    }

    /**
     * Checks whether the transaction buffer is currently empty.
     *
     * @return true if no transactions are pending, false otherwise
     */
    public boolean isEmpty()
    {
        return queue.isEmpty();
    }

    /**
     * Clears all transactions from the queue. Intended primarily for testing teardown.
     */
    public void clear()
    {
        queue.clear();
    }
}
