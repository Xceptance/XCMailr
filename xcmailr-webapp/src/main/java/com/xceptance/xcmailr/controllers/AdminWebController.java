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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.xceptance.xcmailr.repositories.DomainRepository;
import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailTransactionRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

import models.Domain;
import models.MBox;
import models.MailTransaction;
import models.Status;
import models.User;

/**
 * Controller providing administrative controls including user management,
 * domain whitelisting, transaction auditing, and email statistics.
 *
 * @author Xceptance Software Technologies GmbH
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminWebController
{
    private static final Logger LOG = LoggerFactory.getLogger(AdminWebController.class);
    private static final Pattern DOMAIN_PATTERN = Pattern.compile("^[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*\\.[a-z]{2,63}$",
                                                                  Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                                                             .withZone(ZoneId.systemDefault());

    private final UserRepository userRepository;
    private final DomainRepository domainRepository;
    private final MailTransactionRepository transactionRepository;
    private final MailboxRepository mailboxRepository;
    private final MailRepository mailRepository;

    public AdminWebController(final UserRepository userRepository,
                              final DomainRepository domainRepository,
                              final MailTransactionRepository transactionRepository,
                              final MailboxRepository mailboxRepository,
                              final MailRepository mailRepository)
    {
        this.userRepository = userRepository;
        this.domainRepository = domainRepository;
        this.transactionRepository = transactionRepository;
        this.mailboxRepository = mailboxRepository;
        this.mailRepository = mailRepository;
    }

    /**
     * Redirects admin root to user management dashboard.
     */
    @GetMapping
    public String adminIndex()
    {
        return "redirect:/admin/users";
    }

    // =========================================================================
    // User Management
    // =========================================================================

    /**
     * Displays paginated user accounts with optional search query.
     */
    @GetMapping("/users")
    public String listUsers(@RequestParam(value = "search", required = false) final String search,
                            @RequestParam(value = "page", defaultValue = "0") final int page,
                            @RequestParam(value = "size", defaultValue = "15") final int size,
                            @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                            final Principal principal,
                            final Model model)
    {
        final User currentAdmin = getAdmin(principal);
        final Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));

        final boolean hasSearch = search != null && !search.trim().isEmpty();
        final String trimmedSearch = hasSearch ? search.trim() : "";

        final Page<User> usersPage = hasSearch
            ? userRepository.searchUsers(trimmedSearch, pageable)
            : userRepository.findAllByOrderByMailAsc(pageable);

        model.addAttribute("currentAdmin", currentAdmin);
        model.addAttribute("users", usersPage.getContent());
        model.addAttribute("page", usersPage);
        model.addAttribute("search", trimmedSearch);

        if ("true".equalsIgnoreCase(hxRequest))
        {
            return "admin/fragments/user-table :: table";
        }

        return "admin/users";
    }

    /**
     * Toggles activation status of a user account.
     */
    @PostMapping("/users/{id}/toggle-active")
    public Object toggleUserActive(@PathVariable("id") final long id,
                                   @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                                   final Principal principal)
    {
        final User currentAdmin = getAdmin(principal);
        if (currentAdmin.getId() == id)
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Administrators cannot deactivate their own account");
        }

        final User targetUser = userRepository.findById(id)
                                              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        targetUser.setActive(!targetUser.isActive());
        userRepository.save(targetUser);

        LOG.info("Admin {} toggled active status for user {} to {}", currentAdmin.getMail(), targetUser.getMail(), targetUser.isActive());

        if ("true".equalsIgnoreCase(hxRequest))
        {
            final HttpHeaders headers = new HttpHeaders();
            headers.add("HX-Trigger", "userListChanged");
            return ResponseEntity.ok().headers(headers).body("");
        }

        return "redirect:/admin/users?updated";
    }

    /**
     * Promotes or demotes user administrative privileges.
     */
    @PostMapping("/users/{id}/toggle-admin")
    public Object toggleUserAdminRole(@PathVariable("id") final long id,
                                      @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                                      final Principal principal)
    {
        final User currentAdmin = getAdmin(principal);
        if (currentAdmin.getId() == id)
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Administrators cannot remove their own admin privileges");
        }

        final User targetUser = userRepository.findById(id)
                                              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        targetUser.setAdmin(!targetUser.isAdmin());
        userRepository.save(targetUser);

        LOG.info("Admin {} changed admin role for user {} to {}", currentAdmin.getMail(), targetUser.getMail(), targetUser.isAdmin());

        if ("true".equalsIgnoreCase(hxRequest))
        {
            final HttpHeaders headers = new HttpHeaders();
            headers.add("HX-Trigger", "userListChanged");
            return ResponseEntity.ok().headers(headers).body("");
        }

        return "redirect:/admin/users?updated";
    }

    /**
     * Deletes a user account and all owned mailboxes and messages.
     */
    @DeleteMapping("/users/{id}")
    @Transactional
    public Object deleteUser(@PathVariable("id") final long id,
                             @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                             final Principal principal)
    {
        final User currentAdmin = getAdmin(principal);
        if (currentAdmin.getId() == id)
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Administrators cannot delete their own account");
        }

        final User targetUser = userRepository.findById(id)
                                              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Delete user's mailboxes and emails
        final List<MBox> boxes = mailboxRepository.findByUsr(targetUser, Pageable.unpaged()).getContent();
        for (final MBox box : boxes)
        {
            mailRepository.deleteByMailbox(box.getId());
            mailboxRepository.delete(box);
        }

        userRepository.delete(targetUser);
        LOG.info("Admin {} deleted user {} (id: {})", currentAdmin.getMail(), targetUser.getMail(), id);

        if ("true".equalsIgnoreCase(hxRequest))
        {
            final HttpHeaders headers = new HttpHeaders();
            headers.add("HX-Trigger", "userListChanged");
            return ResponseEntity.ok().headers(headers).body("");
        }

        return "redirect:/admin/users?deleted";
    }

    // =========================================================================
    // Domain Whitelist Management
    // =========================================================================

    /**
     * Displays configured domain whitelist.
     */
    @GetMapping("/domains")
    public String listDomains(@RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                              final Model model)
    {
        final List<Domain> domains = domainRepository.findAllByOrderByDomainnameAsc();
        model.addAttribute("domains", domains);

        if ("true".equalsIgnoreCase(hxRequest))
        {
            return "admin/fragments/domain-table :: table";
        }

        return "admin/domains";
    }

    /**
     * Adds a new domain to the whitelist.
     */
    @PostMapping("/domains")
    public Object addDomain(@RequestParam("domainName") final String domainName,
                            @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                            final Principal principal)
    {
        final String cleanDomain = (domainName != null) ? domainName.trim().toLowerCase() : "";

        if (cleanDomain.isEmpty() || !DOMAIN_PATTERN.matcher(cleanDomain).matches())
        {
            if ("true".equalsIgnoreCase(hxRequest))
            {
                return ResponseEntity.badRequest().body("Invalid domain name format.");
            }
            return "redirect:/admin/domains?error=invalidDomain";
        }

        if (domainRepository.existsByDomainnameIgnoreCase(cleanDomain))
        {
            if ("true".equalsIgnoreCase(hxRequest))
            {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Domain already exists in whitelist.");
            }
            return "redirect:/admin/domains?error=duplicateDomain";
        }

        final Domain domain = new Domain(cleanDomain);
        domainRepository.save(domain);
        LOG.info("Admin {} added domain '{}' to whitelist", principal.getName(), cleanDomain);

        if ("true".equalsIgnoreCase(hxRequest))
        {
            final HttpHeaders headers = new HttpHeaders();
            headers.add("HX-Trigger", "domainListChanged");
            return ResponseEntity.ok().headers(headers).body("");
        }

        return "redirect:/admin/domains?created";
    }

    /**
     * Removes a domain from the whitelist.
     */
    @DeleteMapping("/domains/{id}")
    public Object deleteDomain(@PathVariable("id") final long id,
                               @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                               final Principal principal)
    {
        final Domain domain = domainRepository.findById(id)
                                              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Domain not found"));

        domainRepository.delete(domain);
        LOG.info("Admin {} removed domain '{}' from whitelist", principal.getName(), domain.getDomainname());

        if ("true".equalsIgnoreCase(hxRequest))
        {
            final HttpHeaders headers = new HttpHeaders();
            headers.add("HX-Trigger", "domainListChanged");
            return ResponseEntity.ok().headers(headers).body("");
        }

        return "redirect:/admin/domains?deleted";
    }

    // =========================================================================
    // Transaction Audit Logs & Statistics
    // =========================================================================

    /**
     * Displays paged inbound SMTP transaction audit log.
     */
    @GetMapping("/transactions")
    public String listTransactions(@RequestParam(value = "page", defaultValue = "0") final int page,
                                   @RequestParam(value = "size", defaultValue = "25") final int size,
                                   @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                                   final Model model)
    {
        final Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        final Page<MailTransaction> txPage = transactionRepository.findAllByOrderByTsDesc(pageable);
        final List<Status> statusSummary = transactionRepository.getStatusCounts();

        model.addAttribute("transactions", txPage.getContent());
        model.addAttribute("page", txPage);
        model.addAttribute("statusSummary", statusSummary);
        model.addAttribute("dateFormatter", DATE_FORMATTER);

        if ("true".equalsIgnoreCase(hxRequest))
        {
            return "admin/fragments/transaction-table :: table";
        }

        return "admin/transactions";
    }

    /**
     * Purges transaction audit logs older than a given number of days.
     */
    @PostMapping("/transactions/purge")
    public String purgeTransactions(@RequestParam(value = "days", defaultValue = "30") final int days,
                                    final Principal principal)
    {
        if (days <= 0)
        {
            transactionRepository.deleteAll();
            LOG.info("Admin {} purged all transaction audit records", principal.getName());
        }
        else
        {
            final long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
            transactionRepository.deleteByTsBefore(cutoff);
            LOG.info("Admin {} purged transactions older than {} days (cutoff: {})", principal.getName(), days, cutoff);
        }

        return "redirect:/admin/transactions?purged";
    }

    /**
     * Displays transaction overview and statistics dashboard.
     */
    @GetMapping("/statistics")
    public String showStatistics(final Model model)
    {
        final List<Status> statusSummary = transactionRepository.getStatusCounts();
        final long totalTransactions = transactionRepository.count();
        final long totalUsers = userRepository.count();
        final long totalMailboxes = mailboxRepository.count();
        final long totalMails = mailRepository.count();

        model.addAttribute("statusSummary", statusSummary);
        model.addAttribute("totalTransactions", totalTransactions);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalMailboxes", totalMailboxes);
        model.addAttribute("totalMails", totalMails);

        return "admin/statistics";
    }

    private User getAdmin(final Principal principal)
    {
        if (principal == null)
        {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User must be authenticated");
        }
        final User user = userRepository.findByMailIgnoreCase(principal.getName())
                                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (!user.isAdmin())
        {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrative privileges required");
        }
        return user;
    }
}
