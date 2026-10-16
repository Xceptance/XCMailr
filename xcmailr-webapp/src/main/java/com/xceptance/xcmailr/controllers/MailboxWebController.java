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
package com.xceptance.xcmailr.controllers;

import java.security.Principal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
     *
     * @param search optional search query
     * @param status status filter (ALL, ACTIVE, EXPIRED)
     * @param page page number (0-indexed)
     * @param size page size
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
                                @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                                final Principal principal,
                                final Model model)
    {
        if (principal == null)
        {
            return "redirect:/login";
        }
        final User user = getUser(principal);
        final Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "id"));

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
        model.addAttribute("dateFormatter", DATE_FORMATTER);

        if ("true".equalsIgnoreCase(hxRequest))
        {
            return "mailboxes/fragments/mailbox-table :: table";
        }

        // Add available domains for new mailbox modal
        final List<Domain> domains = domainRepository.findAllByOrderByDomainnameAsc();
        model.addAttribute("domains", domains);
        model.addAttribute("suggestedAddress", RandomStringUtils.randomAlphanumeric(7).toLowerCase());

        return "mailboxes/index";
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
        return "mailboxes/fragments/modal-new :: modal";
    }

    /**
     * Creates a new mailbox for the authenticated user.
     *
     * @param address desired local address part (generated if blank)
     * @param domain domain name
     * @param durationHours validity duration in hours (0 for unlimited)
     * @param forwardEmails whether to forward emails to user's registered address
     * @param hxRequest header indicating HTMX request
     * @param principal authenticated user
     * @param model template model
     * @return redirect or HTMX response
     */
    @PostMapping("/mailboxes")
    public Object createMailbox(@RequestParam(value = "address", required = false) final String address,
                                @RequestParam("domain") final String domain,
                                @RequestParam(value = "durationHours", defaultValue = "1") final int durationHours,
                                @RequestParam(value = "forwardEmails", defaultValue = "true") final boolean forwardEmails,
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
            headers.add("HX-Trigger", "mailboxChanged");
            headers.add("HX-Trigger-After-Swap", "closeModal");
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
            headers.add("HX-Trigger", "mailboxChanged");
            headers.add("HX-Trigger-After-Swap", "closeModal");
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
