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
package com.xceptance.xcmailr.controllers;

import java.security.Principal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.xceptance.xcmailr.repositories.DomainRepository;
import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

import models.Domain;
import models.MBox;
import models.User;

/**
 * Controller for interactive mailbox dashboard and management views.
 * Supports HTMX partial responses for live search, pagination, and modal actions.
 *
 * @author Xceptance Software Technologies GmbH
 */
@Controller
public class MailboxWebController
{
    private static final Logger LOG = LoggerFactory.getLogger(MailboxWebController.class);
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^[a-zA-Z0-9!#$%&'*+/=?^_`{|}~.-]+$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                                                                             .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final MailboxRepository mailboxRepository;
    private final DomainRepository domainRepository;
    private final MailRepository mailRepository;
    private final UserRepository userRepository;

    public MailboxWebController(final MailboxRepository mailboxRepository,
                                final DomainRepository domainRepository,
                                final MailRepository mailRepository,
                                final UserRepository userRepository)
    {
        this.mailboxRepository = mailboxRepository;
        this.domainRepository = domainRepository;
        this.mailRepository = mailRepository;
        this.userRepository = userRepository;
    }

    /**
     * Mailbox dashboard view. Handles full page rendering and HTMX partial table updates.
     * Supports server-side column sorting with sort parameter whitelist verification.
     *
     * @param search optional search query
     * @param status status filter (ALL, ACTIVE, EXPIRED)
     * @param page page number (0-indexed)
     * @param size page size
     * @param sort sort column property (address, status, expires, forwards, suppressions; defaults to id)
     * @param dir sort direction (asc, desc; defaults to desc)
     * @param hxRequest header indicating HTMX dynamic request
     * @param principal authenticated user
     * @param model template model
     * @return view or fragment name
     */
    @GetMapping({"/", "/mailboxes"})
    public String listMailboxes(@RequestParam(value = "search", required = false) final String search,
                                @RequestParam(value = "status", defaultValue = "ALL") final String status,
                                @RequestParam(value = "page", defaultValue = "0") final int page,
                                @RequestParam(value = "size", defaultValue = "15") final int size,
                                @RequestParam(value = "sort", defaultValue = "id") final String sort,
                                @RequestParam(value = "dir", defaultValue = "desc") final String dir,
                                @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                                final Principal principal,
                                final Model model)
    {
        if (principal == null)
        {
            return "redirect:/login";
        }
        final User user = getUser(principal);

        // Normalize sort column and direction via strict whitelist mapping
        final String effectiveSort = resolveSortField(sort);
        final String effectiveDir = "id".equals(effectiveSort) ? "desc" : resolveSortDirection(dir);
        final Sort sortOrder = resolveSort(sort, dir);
        final Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sortOrder);

        final Page<MBox> mailboxPage;
        final boolean hasSearch = search != null && !search.trim().isEmpty();
        final String trimmedSearch = hasSearch ? search.trim() : "";

        if ("ACTIVE".equalsIgnoreCase(status))
        {
            mailboxPage = hasSearch
                ? mailboxRepository.findByUsrAndExpiredAndSearch(user, false, trimmedSearch, pageable)
                : mailboxRepository.findByUsrAndExpired(user, false, pageable);
        }
        else if ("EXPIRED".equalsIgnoreCase(status))
        {
            mailboxPage = hasSearch
                ? mailboxRepository.findByUsrAndExpiredAndSearch(user, true, trimmedSearch, pageable)
                : mailboxRepository.findByUsrAndExpired(user, true, pageable);
        }
        else
        {
            mailboxPage = hasSearch
                ? mailboxRepository.findByUsrAndSearch(user, trimmedSearch, pageable)
                : mailboxRepository.findByUsr(user, pageable);
        }

        // Count messages for each mailbox in page
        final Map<Long, Long> mailCounts = new HashMap<>();
        for (final MBox box : mailboxPage.getContent())
        {
            mailCounts.put(box.getId(), mailRepository.countByMailbox(box.getId()));
        }

        model.addAttribute("user", user);
        model.addAttribute("mailboxes", mailboxPage.getContent());
        model.addAttribute("page", mailboxPage);
        model.addAttribute("mailCounts", mailCounts);
        model.addAttribute("search", trimmedSearch);
        model.addAttribute("status", status.toUpperCase());
        model.addAttribute("sort", effectiveSort);
        model.addAttribute("dir", effectiveDir);
        model.addAttribute("dateFormatter", DATE_FORMATTER);
        model.addAttribute("isoFormatter", ISO_FORMATTER);

        if ("true".equalsIgnoreCase(hxRequest))
        {
            return "mailboxes/fragments/mailbox-table :: table";
        }

        // Add available domains for new mailbox modal
        final List<Domain> domains = domainRepository.findAllByOrderByDomainnameAsc();
        model.addAttribute("domains", domains);
        model.addAttribute("suggestedAddress", RandomStringUtils.randomAlphanumeric(7).toLowerCase());
        model.addAttribute("selectedDuration", 2);
        model.addAttribute("forwardEmails", true);

        return "mailboxes/index";
    }

    /**
     * Resolves the normalized sort column property name from an input parameter against a strict whitelist.
     * Returns "id" if the input parameter is null, blank, or not in the allowed set.
     *
     * @param sort the requested sort column name
     * @return the normalized whitelist property key ("address", "status", "expires", "forwards", "suppressions"),
     *         or "id" as the safe fallback
     */
    static String resolveSortField(final String sort)
    {
        if (sort == null)
        {
            return "id";
        }
        final String cleanSort = sort.trim().toLowerCase();
        return switch (cleanSort)
        {
            case "address", "status", "expires", "forwards", "suppressions" -> cleanSort;
            default -> "id";
        };
    }

    /**
     * Resolves the normalized sort direction ("asc" or "desc").
     * Case-insensitively checks for "asc"; any other value (including null, empty, or invalid)
     * safely defaults to "desc".
     *
     * @param dir the requested sort direction string
     * @return "asc" if requested, otherwise "desc"
     */
    static String resolveSortDirection(final String dir)
    {
        return "asc".equalsIgnoreCase(dir) ? "asc" : "desc";
    }

    /**
     * Constructs a Spring Data {@link Sort} instance corresponding to the requested column and direction.
     * Only whitelisted column keys are mapped to entity attributes:
     * <ul>
     *   <li>"address" maps to composite ordering by address then domain</li>
     *   <li>"status" maps to expired flag</li>
     *   <li>"expires" maps to ts_Active timestamp</li>
     *   <li>"forwards" maps to forwards counter</li>
     *   <li>"suppressions" maps to suppressions counter</li>
     * </ul>
     * Any unrecognized or invalid sort parameter safely falls back to ordering by entity ID descending.
     *
     * @param sort the requested sort column key
     * @param dir the requested sort direction string ("asc" or "desc")
     * @return a valid {@link Sort} instance ready for use in database queries
     */
    static Sort resolveSort(final String sort, final String dir)
    {
        final String effectiveField = resolveSortField(sort);
        if ("id".equals(effectiveField))
        {
            // Default or unrecognized column safely defaults to ID descending
            return Sort.by(Sort.Direction.DESC, "id");
        }

        final Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return switch (effectiveField)
        {
            // Composite sort by local address part, then domain name
            case "address" -> Sort.by(direction, "address").and(Sort.by(direction, "domain"));
            // Boolean expired status (false = active, true = expired)
            case "status" -> Sort.by(direction, "expired");
            // Expiration epoch timestamp (use JpaSort.unsafe to avoid Spring Data underscore delimiter parsing on ts_Active)
            case "expires" -> JpaSort.unsafe(direction, "ts_Active");
            // Forwarded email counter
            case "forwards" -> Sort.by(direction, "forwards");
            // Suppressed email counter
            case "suppressions" -> Sort.by(direction, "suppressions");
            // Safe fallback to ID DESC for defensive coding
            default -> Sort.by(Sort.Direction.DESC, "id");
        };
    }

    /**
     * Renders modal dialog fragment for creating a new mailbox.
     *
     * @param principal authenticated user
     * @param model template model
     * @return modal fragment
     */
    @GetMapping("/mailboxes/new")
    public String newMailboxModal(final Principal principal, final Model model)
    {
        final List<Domain> domains = domainRepository.findAllByOrderByDomainnameAsc();
        model.addAttribute("domains", domains);
        model.addAttribute("suggestedAddress", RandomStringUtils.randomAlphanumeric(7).toLowerCase());
        model.addAttribute("selectedDuration", 2);
        model.addAttribute("forwardEmails", true);
        return "mailboxes/fragments/modal-new :: modal";
    }

    /**
     * Creates a new mailbox for the authenticated user.
     *
     * @param address desired local address part (generated if blank)
     * @param domain domain name
     * @param durationHours validity duration in hours (0 for unlimited, defaults to 2 hours)
     * @param forwardEmails whether to forward emails to user's registered address
     * @param hxRequest header indicating HTMX request
     * @param principal authenticated user
     * @param model template model
     * @return redirect or HTMX response
     */
    @PostMapping("/mailboxes")
    public Object createMailbox(@RequestParam(value = "address", required = false) final String address,
                                @RequestParam("domain") final String domain,
                                @RequestParam(value = "durationHours", defaultValue = "2") final int durationHours,
                                @RequestParam(value = "forwardEmails", defaultValue = "false") final boolean forwardEmails,
                                @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                                final Principal principal,
                                final Model model)
    {
        final User user = getUser(principal);
        String localAddress = address != null ? address.trim().toLowerCase() : "";
        final String cleanDomain = domain != null ? domain.trim().toLowerCase() : "";

        if (localAddress.isEmpty())
        {
            localAddress = RandomStringUtils.randomAlphanumeric(7).toLowerCase();
        }

        if (!ADDRESS_PATTERN.matcher(localAddress).matches())
        {
            if ("true".equalsIgnoreCase(hxRequest))
            {
                model.addAttribute("error", "Invalid mailbox address characters.");
                model.addAttribute("domains", domainRepository.findAllByOrderByDomainnameAsc());
                model.addAttribute("address", localAddress);
                model.addAttribute("selectedDomain", cleanDomain);
                model.addAttribute("selectedDuration", durationHours);
                model.addAttribute("forwardEmails", forwardEmails);
                return "mailboxes/fragments/modal-new :: modal";
            }
            return "redirect:/mailboxes?error=invalidAddress";
        }

        if (!domainRepository.existsByDomainnameIgnoreCase(cleanDomain))
        {
            if ("true".equalsIgnoreCase(hxRequest))
            {
                model.addAttribute("error", "Specified domain is not configured or whitelisted.");
                model.addAttribute("domains", domainRepository.findAllByOrderByDomainnameAsc());
                model.addAttribute("address", localAddress);
                model.addAttribute("selectedDomain", cleanDomain);
                model.addAttribute("selectedDuration", durationHours);
                model.addAttribute("forwardEmails", forwardEmails);
                return "mailboxes/fragments/modal-new :: modal";
            }
            return "redirect:/mailboxes?error=invalidDomain";
        }

        if (mailboxRepository.existsByAddressIgnoreCaseAndDomainIgnoreCase(localAddress, cleanDomain))
        {
            if ("true".equalsIgnoreCase(hxRequest))
            {
                model.addAttribute("error", "A mailbox with this address already exists.");
                model.addAttribute("domains", domainRepository.findAllByOrderByDomainnameAsc());
                model.addAttribute("address", localAddress);
                model.addAttribute("selectedDomain", cleanDomain);
                model.addAttribute("selectedDuration", durationHours);
                model.addAttribute("forwardEmails", forwardEmails);
                return "mailboxes/fragments/modal-new :: modal";
            }
            return "redirect:/mailboxes?error=mailboxExists";
        }

        final long now = System.currentTimeMillis();
        final long expirationTs = durationHours <= 0 ? 0L : now + TimeUnit.HOURS.toMillis(durationHours);

        final MBox mailbox = new MBox(localAddress, cleanDomain, expirationTs, false, user);
        mailbox.setForwardEmails(forwardEmails);
        mailboxRepository.save(mailbox);

        LOG.info("Created mailbox {}@{} for user {}", localAddress, cleanDomain, user.getMail());

        if ("true".equalsIgnoreCase(hxRequest))
        {
            final HttpHeaders headers = new HttpHeaders();
            headers.add("HX-Trigger", "mailboxChanged, closeModal");
            return ResponseEntity.ok().headers(headers).body("");
        }

        return "redirect:/mailboxes?created";
    }

    /**
     * Renders modal dialog fragment for editing an existing mailbox.
     *
     * @param id mailbox ID
     * @param principal authenticated user
     * @param model template model
     * @return modal fragment
     */
    @GetMapping("/mailboxes/{id}/edit")
    public String editMailboxModal(@PathVariable("id") final long id,
                                   final Principal principal,
                                   final Model model)
    {
        final User user = getUser(principal);
        final MBox mailbox = getMailboxForUser(id, user);

        model.addAttribute("mailbox", mailbox);
        model.addAttribute("dateFormatter", DATE_FORMATTER);
        model.addAttribute("isoFormatter", ISO_FORMATTER);
        return "mailboxes/fragments/modal-edit :: modal";
    }

    /**
     * Updates an existing mailbox's settings.
     *
     * @param id mailbox ID
     * @param forwardEmails whether forwarding is active
     * @param active whether the mailbox is enabled
     * @param hxRequest header indicating HTMX request
     * @param principal authenticated user
     * @return redirect or HTMX response
     */
    @PostMapping("/mailboxes/{id}/edit")
    public Object updateMailbox(@PathVariable("id") final long id,
                                @RequestParam(value = "forwardEmails", defaultValue = "false") final boolean forwardEmails,
                                @RequestParam(value = "active", defaultValue = "false") final boolean active,
                                @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                                final Principal principal)
    {
        final User user = getUser(principal);
        final MBox mailbox = getMailboxForUser(id, user);

        mailbox.setForwardEmails(forwardEmails);
        mailbox.setExpired(!active);
        mailboxRepository.save(mailbox);

        LOG.info("Updated mailbox {} (id: {}) for user {}", mailbox.getFullAddress(), id, user.getMail());

        if ("true".equalsIgnoreCase(hxRequest))
        {
            final HttpHeaders headers = new HttpHeaders();
            headers.add("HX-Trigger", "mailboxChanged, closeModal");
            return ResponseEntity.ok().headers(headers).body("");
        }

        return "redirect:/mailboxes?updated";
    }

    /**
     * Extends the expiration timestamp of a mailbox.
     *
     * @param id mailbox ID
     * @param hours hours to add (defaults to 24)
     * @param hxRequest header indicating HTMX request
     * @param principal authenticated user
     * @return redirect or HTMX response
     */
    @PostMapping("/mailboxes/{id}/extend")
    public Object extendMailbox(@PathVariable("id") final long id,
                                @RequestParam(value = "hours", defaultValue = "24") final int hours,
                                @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                                final Principal principal)
    {
        final User user = getUser(principal);
        final MBox mailbox = getMailboxForUser(id, user);

        final long now = System.currentTimeMillis();
        final long base = Math.max(now, mailbox.getTs_Active());
        mailbox.setTs_Active(base + TimeUnit.HOURS.toMillis(hours));
        mailbox.setExpired(false);
        mailboxRepository.save(mailbox);

        LOG.info("Extended mailbox {} (id: {}) by {} hours for user {}", mailbox.getFullAddress(), id, hours, user.getMail());

        if ("true".equalsIgnoreCase(hxRequest))
        {
            final HttpHeaders headers = new HttpHeaders();
            headers.add("HX-Trigger", "mailboxChanged");
            return ResponseEntity.ok().headers(headers).body("");
        }

        return "redirect:/mailboxes?extended";
    }

    /**
     * Toggles active / inactive (expired) state of a mailbox.
     *
     * @param id mailbox ID
     * @param hxRequest header indicating HTMX request
     * @param principal authenticated user
     * @return redirect or HTMX response
     */
    @PostMapping("/mailboxes/{id}/toggle")
    public Object toggleMailbox(@PathVariable("id") final long id,
                                @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                                final Principal principal)
    {
        final User user = getUser(principal);
        final MBox mailbox = getMailboxForUser(id, user);

        final boolean willBeActive = mailbox.isExpired();
        mailbox.setExpired(!willBeActive);

        if (willBeActive && mailbox.getTs_Active() > 0 && mailbox.getTs_Active() < System.currentTimeMillis())
        {
            // If reactivating an already time-expired mailbox, grant 1 hour of life
            mailbox.setTs_Active(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1));
        }

        mailboxRepository.save(mailbox);

        LOG.info("Toggled mailbox {} (id: {}) active={} for user {}", mailbox.getFullAddress(), id, willBeActive, user.getMail());

        if ("true".equalsIgnoreCase(hxRequest))
        {
            final HttpHeaders headers = new HttpHeaders();
            headers.add("HX-Trigger", "mailboxChanged");
            return ResponseEntity.ok().headers(headers).body("");
        }

        return "redirect:/mailboxes?toggled";
    }

    /**
     * Resets forwards and suppressions counters for a mailbox.
     *
     * @param id mailbox ID
     * @param hxRequest header indicating HTMX request
     * @param principal authenticated user
     * @return redirect or HTMX response
     */
    @PostMapping("/mailboxes/{id}/reset-counters")
    public Object resetCounters(@PathVariable("id") final long id,
                                 @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                                 final Principal principal)
    {
        final User user = getUser(principal);
        final MBox mailbox = getMailboxForUser(id, user);

        mailbox.resetForwards();
        mailbox.resetSuppressions();
        mailboxRepository.save(mailbox);

        LOG.info("Reset counters on mailbox {} (id: {}) for user {}", mailbox.getFullAddress(), id, user.getMail());

        if ("true".equalsIgnoreCase(hxRequest))
        {
            final HttpHeaders headers = new HttpHeaders();
            headers.add("HX-Trigger", "mailboxChanged");
            return ResponseEntity.ok().headers(headers).body("");
        }

        return "redirect:/mailboxes?reset";
    }

    /**
     * Deletes a mailbox and its associated emails.
     *
     * @param id mailbox ID
     * @param hxRequest header indicating HTMX request
     * @param principal authenticated user
     * @return response entity or redirect
     */
    @DeleteMapping("/mailboxes/{id}")
    public Object deleteMailbox(@PathVariable("id") final long id,
                                @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                                final Principal principal)
    {
        final User user = getUser(principal);
        final MBox mailbox = getMailboxForUser(id, user);

        // Delete associated mails first
        mailRepository.deleteByMailbox(id);
        mailboxRepository.delete(mailbox);

        LOG.info("Deleted mailbox {} (id: {}) for user {}", mailbox.getFullAddress(), id, user.getMail());

        if ("true".equalsIgnoreCase(hxRequest))
        {
            final HttpHeaders headers = new HttpHeaders();
            headers.add("HX-Trigger", "mailboxChanged");
            return ResponseEntity.ok().headers(headers).body("");
        }

        return "redirect:/mailboxes?deleted";
    }

    /**
     * Bulk enables the selected mailboxes owned by the authenticated user.
     *
     * @param mailboxIds list of mailbox IDs to enable
     * @param hxRequest header indicating HTMX request
     * @param principal authenticated user
     * @return redirect or HTMX response
     */
    @PostMapping("/mailboxes/bulk/enable")
    public Object bulkEnableMailboxes(@RequestParam(value = "mailboxIds", required = false) final List<Long> mailboxIds,
                                      @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                                      final Principal principal)
    {
        final User user = getUser(principal);
        if (mailboxIds != null && !mailboxIds.isEmpty())
        {
            final List<MBox> boxes = mailboxRepository.findByUsrAndIdIn(user, mailboxIds);
            final long now = System.currentTimeMillis();
            for (final MBox box : boxes)
            {
                box.setExpired(false);
                if (box.getTs_Active() > 0 && box.getTs_Active() < now)
                {
                    box.setTs_Active(now + TimeUnit.HOURS.toMillis(1));
                }
            }
            mailboxRepository.saveAll(boxes);
            LOG.info("Bulk enabled {} mailboxes for user {}", boxes.size(), user.getMail());
        }

        if ("true".equalsIgnoreCase(hxRequest))
        {
            final HttpHeaders headers = new HttpHeaders();
            headers.add("HX-Trigger", "mailboxChanged");
            return ResponseEntity.ok().headers(headers).body("");
        }

        return "redirect:/mailboxes?bulkEnabled";
    }

    /**
     * Bulk disables the selected mailboxes owned by the authenticated user.
     *
     * @param mailboxIds list of mailbox IDs to disable
     * @param hxRequest header indicating HTMX request
     * @param principal authenticated user
     * @return redirect or HTMX response
     */
    @PostMapping("/mailboxes/bulk/disable")
    public Object bulkDisableMailboxes(@RequestParam(value = "mailboxIds", required = false) final List<Long> mailboxIds,
                                       @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                                       final Principal principal)
    {
        final User user = getUser(principal);
        if (mailboxIds != null && !mailboxIds.isEmpty())
        {
            final List<MBox> boxes = mailboxRepository.findByUsrAndIdIn(user, mailboxIds);
            for (final MBox box : boxes)
            {
                box.setExpired(true);
            }
            mailboxRepository.saveAll(boxes);
            LOG.info("Bulk disabled {} mailboxes for user {}", boxes.size(), user.getMail());
        }

        if ("true".equalsIgnoreCase(hxRequest))
        {
            final HttpHeaders headers = new HttpHeaders();
            headers.add("HX-Trigger", "mailboxChanged");
            return ResponseEntity.ok().headers(headers).body("");
        }

        return "redirect:/mailboxes?bulkDisabled";
    }

    /**
     * Bulk extends the validity period of the selected mailboxes owned by the authenticated user.
     *
     * @param mailboxIds list of mailbox IDs to extend
     * @param hours number of hours to add (defaults to 24)
     * @param hxRequest header indicating HTMX request
     * @param principal authenticated user
     * @return redirect or HTMX response
     */
    @PostMapping("/mailboxes/bulk/extend")
    public Object bulkExtendMailboxes(@RequestParam(value = "mailboxIds", required = false) final List<Long> mailboxIds,
                                      @RequestParam(value = "hours", defaultValue = "24") final int hours,
                                      @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                                      final Principal principal)
    {
        final User user = getUser(principal);
        if (mailboxIds != null && !mailboxIds.isEmpty())
        {
            final List<MBox> boxes = mailboxRepository.findByUsrAndIdIn(user, mailboxIds);
            final long now = System.currentTimeMillis();
            for (final MBox box : boxes)
            {
                if (box.getTs_Active() > 0)
                {
                    final long base = Math.max(now, box.getTs_Active());
                    box.setTs_Active(base + TimeUnit.HOURS.toMillis(hours));
                }
                box.setExpired(false);
            }
            mailboxRepository.saveAll(boxes);
            LOG.info("Bulk extended {} mailboxes by {} hours for user {}", boxes.size(), hours, user.getMail());
        }

        if ("true".equalsIgnoreCase(hxRequest))
        {
            final HttpHeaders headers = new HttpHeaders();
            headers.add("HX-Trigger", "mailboxChanged");
            return ResponseEntity.ok().headers(headers).body("");
        }

        return "redirect:/mailboxes?bulkExtended";
    }

    /**
     * Bulk resets forward and suppression counters for the selected mailboxes owned by the authenticated user.
     *
     * @param mailboxIds list of mailbox IDs to reset
     * @param hxRequest header indicating HTMX request
     * @param principal authenticated user
     * @return redirect or HTMX response
     */
    @PostMapping("/mailboxes/bulk/reset-counters")
    public Object bulkResetCounters(@RequestParam(value = "mailboxIds", required = false) final List<Long> mailboxIds,
                                    @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                                    final Principal principal)
    {
        final User user = getUser(principal);
        if (mailboxIds != null && !mailboxIds.isEmpty())
        {
            final int updatedCount = mailboxRepository.resetCountersByUsrAndIds(user, mailboxIds);
            LOG.info("Bulk reset counters on {} mailboxes for user {}", updatedCount, user.getMail());
        }

        if ("true".equalsIgnoreCase(hxRequest))
        {
            final HttpHeaders headers = new HttpHeaders();
            headers.add("HX-Trigger", "mailboxChanged");
            return ResponseEntity.ok().headers(headers).body("");
        }

        return "redirect:/mailboxes?bulkReset";
    }

    /**
     * Bulk deletes the selected mailboxes and their associated emails.
     *
     * @param mailboxIds list of mailbox IDs to delete
     * @param hxRequest header indicating HTMX request
     * @param principal authenticated user
     * @return redirect or HTMX response
     */
    @DeleteMapping("/mailboxes/bulk")
    @PostMapping("/mailboxes/bulk/delete")
    public Object bulkDeleteMailboxes(@RequestParam(value = "mailboxIds", required = false) final List<Long> mailboxIds,
                                      @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                                      final Principal principal)
    {
        final User user = getUser(principal);
        if (mailboxIds != null && !mailboxIds.isEmpty())
        {
            final List<MBox> boxes = mailboxRepository.findByUsrAndIdIn(user, mailboxIds);
            for (final MBox box : boxes)
            {
                mailRepository.deleteByMailbox(box.getId());
                mailboxRepository.delete(box);
            }
            LOG.info("Bulk deleted {} mailboxes for user {}", boxes.size(), user.getMail());
        }

        if ("true".equalsIgnoreCase(hxRequest))
        {
            final HttpHeaders headers = new HttpHeaders();
            headers.add("HX-Trigger", "mailboxChanged");
            return ResponseEntity.ok().headers(headers).body("");
        }

        return "redirect:/mailboxes?bulkDeleted";
    }

    /**
     * Exports all mailbox addresses owned by the authenticated user as a plain text attachment.
     *
     * @param principal authenticated user
     * @return plain text download response with one address per line
     */
    @GetMapping({"/mailboxes/export/all", "/mail/mymaillist.txt"})
    public ResponseEntity<String> exportAllMailboxes(final Principal principal)
    {
        final User user = getUser(principal);
        final List<MBox> boxes = mailboxRepository.findByUsr(user);
        return renderAddressTextList(boxes, "mailboxes.txt");
    }

    /**
     * Exports all active mailbox addresses owned by the authenticated user as a plain text attachment.
     *
     * @param principal authenticated user
     * @return plain text download response with one address per line
     */
    @GetMapping({"/mailboxes/export/active", "/mail/myactivemaillist.txt"})
    public ResponseEntity<String> exportActiveMailboxes(final Principal principal)
    {
        final User user = getUser(principal);
        final List<MBox> boxes = mailboxRepository.findByUsrAndExpired(user, false);
        return renderAddressTextList(boxes, "mailboxes-active.txt");
    }

    /**
     * Exports selected mailbox addresses owned by the authenticated user as a plain text attachment.
     * Supports both list parameter binding and legacy JSON object payload.
     *
     * @param mailboxIds list of selected mailbox IDs
     * @param jsonObj legacy JSON map or array of mailbox IDs
     * @param principal authenticated user
     * @return plain text download response with one address per line
     */
    @PostMapping({"/mailboxes/bulk/export", "/mail/myselectedmaillist.txt"})
    @GetMapping({"/mailboxes/bulk/export", "/mail/myselectedmaillist.txt"})
    public ResponseEntity<String> exportSelectedMailboxes(@RequestParam(value = "mailboxIds", required = false) final List<Long> mailboxIds,
                                                          @RequestParam(value = "jsonObj", required = false) final String jsonObj,
                                                          final Principal principal)
    {
        final User user = getUser(principal);
        final List<Long> targetIds = resolveMailboxIds(mailboxIds, jsonObj);
        if (targetIds.isEmpty())
        {
            return renderAddressTextList(Collections.emptyList(), "mailboxes-selected.txt");
        }
        final List<MBox> boxes = mailboxRepository.findByUsrAndIdIn(user, targetIds);
        return renderAddressTextList(boxes, "mailboxes-selected.txt");
    }

    /**
     * Resolves mailbox IDs from either standard parameter list or legacy JSON payload.
     *
     * @param mailboxIds direct list of IDs
     * @param jsonObj legacy JSON string
     * @return resolved list of IDs
     */
    private List<Long> resolveMailboxIds(final List<Long> mailboxIds, final String jsonObj)
    {
        if (mailboxIds != null && !mailboxIds.isEmpty())
        {
            return mailboxIds;
        }
        if (jsonObj != null && !jsonObj.isBlank())
        {
            try
            {
                final JsonNode node = OBJECT_MAPPER.readTree(jsonObj);
                final List<Long> ids = new ArrayList<>();
                if (node.isArray())
                {
                    for (final JsonNode item : node)
                    {
                        ids.add(item.asLong());
                    }
                    return ids;
                }
                else if (node.isObject())
                {
                    final Iterator<String> fieldNames = node.fieldNames();
                    while (fieldNames.hasNext())
                    {
                        final String key = fieldNames.next();
                        if (node.get(key).asBoolean(false))
                        {
                            ids.add(Long.parseLong(key));
                        }
                    }
                    return ids;
                }
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to parse jsonObj for bulk export: {}", e.getMessage());
            }
        }
        return Collections.emptyList();
    }

    /**
     * Formats a list of mailboxes into a plain text address response with Content-Disposition attachment header.
     *
     * @param mailboxes list of mailboxes
     * @param filename  download filename
     * @return plain text HTTP response configured as a file attachment
     */
    private ResponseEntity<String> renderAddressTextList(final List<MBox> mailboxes, final String filename)
    {
        final StringBuilder sb = new StringBuilder();
        for (final MBox mailbox : mailboxes)
        {
            sb.append(mailbox.getFullAddress()).append("\n");
        }
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return new ResponseEntity<>(sb.toString(), headers, HttpStatus.OK);
    }

    /**
     * Helper to load the authenticated user entity.
     */
    private User getUser(final Principal principal)
    {
        if (principal == null)
        {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User must be authenticated");
        }
        return userRepository.findByMailIgnoreCase(principal.getName())
                             .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    /**
     * Helper to load and verify ownership of a mailbox.
     */
    private MBox getMailboxForUser(final long mailboxId, final User user)
    {
        final MBox mailbox = mailboxRepository.findById(mailboxId)
                                              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mailbox not found"));
        if (mailbox.getUsr() == null || mailbox.getUsr().getId() != user.getId())
        {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to mailbox");
        }
        return mailbox;
    }
}
