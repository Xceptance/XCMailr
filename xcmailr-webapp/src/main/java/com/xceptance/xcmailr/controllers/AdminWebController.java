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
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.data.domain.Sort;
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

import com.xceptance.xcmailr.dto.ChartDataDto;
import com.xceptance.xcmailr.dto.DomainStatisticsDto;
import com.xceptance.xcmailr.repositories.DomainRepository;
import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailStatisticsRepository;
import com.xceptance.xcmailr.repositories.MailTransactionRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;
import com.xceptance.xcmailr.services.EmailStatisticsService;

import com.xceptance.xcmailr.models.Domain;
import com.xceptance.xcmailr.models.MBox;
import com.xceptance.xcmailr.models.MailTransaction;
import com.xceptance.xcmailr.models.Status;
import com.xceptance.xcmailr.models.User;
import com.xceptance.xcmailr.util.PaginationHelper;

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
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final UserRepository userRepository;
    private final DomainRepository domainRepository;
    private final MailTransactionRepository transactionRepository;
    private final MailboxRepository mailboxRepository;
    private final MailRepository mailRepository;
    private final EmailStatisticsService statisticsService;
    private final MailStatisticsRepository mailStatisticsRepository;

    public AdminWebController(final UserRepository userRepository,
                              final DomainRepository domainRepository,
                              final MailTransactionRepository transactionRepository,
                              final MailboxRepository mailboxRepository,
                              final MailRepository mailRepository,
                              final EmailStatisticsService statisticsService,
                              final MailStatisticsRepository mailStatisticsRepository)
    {
        this.userRepository = userRepository;
        this.domainRepository = domainRepository;
        this.transactionRepository = transactionRepository;
        this.mailboxRepository = mailboxRepository;
        this.mailRepository = mailRepository;
        this.statisticsService = statisticsService;
        this.mailStatisticsRepository = mailStatisticsRepository;
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
     * Resolves and normalizes the requested sort column key against an allowed whitelist.
     * Returns "email" if the input parameter is null, blank, or not in the allowed set.
     *
     * @param sort the requested sort column name
     * @return the normalized whitelist property key ("email", "name", "status", "role"),
     *         or "email" as the safe fallback
     */
    static String resolveSortField(final String sort)
    {
        if (sort == null)
        {
            return "email";
        }
        final String cleanSort = sort.trim().toLowerCase();
        return switch (cleanSort)
        {
            case "email", "mail", "name", "status", "role" -> "mail".equals(cleanSort) ? "email" : cleanSort;
            default -> "email";
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
        if (dir == null)
        {
            return "desc";
        }
        return "asc".equalsIgnoreCase(dir.trim()) ? "asc" : "desc";
    }

    /**
     * Constructs a Spring Data {@link Sort} instance corresponding to the requested column and direction.
     * Only whitelisted column keys are mapped to entity attributes:
     * <ul>
     *   <li>"email" maps to case-insensitive natural ordering by {@code mail}</li>
     *   <li>"name" maps to directory-style composite ordering by {@code surname} then {@code forename}
     *       both case-insensitive, with {@code mail ASC} as secondary tiebreaker</li>
     *   <li>"status" maps to {@code active} boolean flag (active accounts first on ASC to match mailbox status
     *       ordering, inactive accounts first on DESC) with {@code mail ASC} as secondary tiebreaker</li>
     *   <li>"role" maps to {@code admin} boolean flag (administrators first on ASC to match alphabetical and
     *       privilege hierarchy, standard users first on DESC) with {@code mail ASC} as secondary tiebreaker</li>
     * </ul>
     * Any unrecognized or invalid sort parameter safely falls back to ordering by {@code mail} ascending.
     *
     * @param sort the requested sort column key
     * @param dir the requested sort direction string ("asc" or "desc")
     * @return a valid {@link Sort} instance ready for use in database queries
     */
    static Sort resolveSort(final String sort, final String dir)
    {
        final String effectiveField = resolveSortField(sort);
        final Sort.Direction direction = "asc".equals(resolveSortDirection(dir)) ? Sort.Direction.ASC : Sort.Direction.DESC;

        // Invert direction for boolean columns so that true (active, admin) precedes false (inactive, user) on ASC,
        // matching the mailbox table behavior and alphabetical/privilege expectations.
        final Sort.Direction booleanDirection = direction.isAscending() ? Sort.Direction.DESC : Sort.Direction.ASC;

        final Sort.Order mailTiebreaker = Sort.Order.by("mail").with(Sort.Direction.ASC).ignoreCase();

        return switch (effectiveField)
        {
            // Case-insensitive natural ordering by registered email address
            case "email" -> Sort.by(Sort.Order.by("mail").with(direction).ignoreCase());

            // Composite directory-style sort: surname first, forename second, both case-insensitive
            case "name" -> Sort.by(
                Sort.Order.by("surname").with(direction).ignoreCase(),
                Sort.Order.by("forename").with(direction).ignoreCase(),
                mailTiebreaker);

            // Boolean active status: true (active) first on ASC (matching mailbox status), false (inactive) first on DESC
            case "status" -> Sort.by(
                Sort.Order.by("active").with(booleanDirection),
                mailTiebreaker);

            // Boolean admin role: true (admin) first on ASC ("Admin" before "User"), false (user) first on DESC
            case "role" -> Sort.by(
                Sort.Order.by("admin").with(booleanDirection),
                mailTiebreaker);

            // Safe fallback to email ascending
            default -> Sort.by(mailTiebreaker);
        };
    }

    /**
     * Displays paginated user accounts with optional search query and column sorting.
     *
     * @param search optional text search query
     * @param page zero-based page index
     * @param size page size
     * @param sort column sort key (email, name, status, role)
     * @param dir sort direction (asc, desc)
     * @param hxRequest header indicating HTMX request
     * @param principal authenticated administrator
     * @param model Spring MVC model
     * @return view name or Thymeleaf fragment
     */
    @GetMapping("/users")
    public String listUsers(@RequestParam(value = "search", required = false) final String search,
                            @RequestParam(value = "page", defaultValue = "0") final int page,
                            @RequestParam(value = "size", defaultValue = "15") final int size,
                            @RequestParam(value = "sort", defaultValue = "email") final String sort,
                            @RequestParam(value = "dir", defaultValue = "asc") final String dir,
                            @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                            final Principal principal,
                            final Model model)
    {
        final User currentAdmin = getAdmin(principal);
        final String effectiveSort = resolveSortField(sort);
        final String effectiveDir = resolveSortDirection(dir);
        final Sort sortOrder = resolveSort(sort, dir);
        final int effectiveSize = PaginationHelper.clampPageSize(size, 15);
        final Pageable pageable = PageRequest.of(Math.max(0, page), effectiveSize, sortOrder);

        final boolean hasSearch = search != null && !search.trim().isEmpty();
        final String trimmedSearch = hasSearch ? search.trim() : "";

        final Page<User> usersPage = hasSearch
            ? userRepository.searchUsers(trimmedSearch, pageable)
            : userRepository.findAll(pageable);

        model.addAttribute("currentAdmin", currentAdmin);
        model.addAttribute("users", usersPage.getContent());
        model.addAttribute("page", usersPage);
        model.addAttribute("search", trimmedSearch);
        model.addAttribute("size", effectiveSize);
        model.addAttribute("sort", effectiveSort);
        model.addAttribute("dir", effectiveDir);

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
        final int effectiveSize = PaginationHelper.clampPageSize(size, 25);
        final Pageable pageable = PageRequest.of(Math.max(0, page), effectiveSize);
        final Page<MailTransaction> txPage = transactionRepository.findAllByOrderByTsDesc(pageable);
        final List<Status> statusSummary = transactionRepository.getStatusCounts();

        model.addAttribute("transactions", txPage.getContent());
        model.addAttribute("page", txPage);
        model.addAttribute("statusSummary", statusSummary);
        model.addAttribute("size", effectiveSize);
        model.addAttribute("dateFormatter", DATE_FORMATTER);
        model.addAttribute("isoFormatter", ISO_FORMATTER);

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
     * Displays transaction overview and statistics dashboard including 24-hour and 7-day
     * activity charts and aggregated sender domain breakdowns.
     *
     * @param model the Spring MVC model to populate with statistics attributes
     * @return view name for the admin statistics dashboard template
     */
    @GetMapping("/statistics")
    public String showStatistics(final Model model)
    {
        final List<Status> statusSummary = transactionRepository.getStatusCounts();
        final long totalTransactions = transactionRepository.count();
        final long totalUsers = userRepository.count();
        final long adminCount = userRepository.countByAdmin(true);
        final long regularCount = userRepository.countByAdmin(false);
        final long totalMailboxes = mailboxRepository.count();
        final long activeMailboxCount = mailboxRepository.countByExpired(false);
        final long expiredMailboxCount = mailboxRepository.countByExpired(true);
        final long totalMails = mailRepository.count();

        final ChartDataDto chart24h = statisticsService.getLast24HoursChartData();
        final ChartDataDto chart7d = statisticsService.getLast7DaysChartData();
        final long totalVolume24h = calculateTotalVolume(chart24h);
        final long totalVolume7d = calculateTotalVolume(chart7d);

        final Date today = Date.valueOf(LocalDate.now());
        final Date weekAgo = Date.valueOf(LocalDate.now().minusDays(6));
        final List<DomainStatisticsDto> todaySenderDomains = mailStatisticsRepository.findDomainStatisticsSince(today);
        final List<DomainStatisticsDto> weekSenderDomains = mailStatisticsRepository.findDailyDomainStatisticsSince(weekAgo);

        model.addAttribute("statusSummary", statusSummary);
        model.addAttribute("totalTransactions", totalTransactions);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("adminCount", adminCount);
        model.addAttribute("regularCount", regularCount);
        model.addAttribute("totalMailboxes", totalMailboxes);
        model.addAttribute("activeMailboxCount", activeMailboxCount);
        model.addAttribute("expiredMailboxCount", expiredMailboxCount);
        model.addAttribute("totalMails", totalMails);
        model.addAttribute("chart24h", chart24h);
        model.addAttribute("chart7d", chart7d);
        model.addAttribute("totalVolume24h", totalVolume24h);
        model.addAttribute("totalVolume7d", totalVolume7d);
        model.addAttribute("todaySenderDomains", todaySenderDomains);
        model.addAttribute("weekSenderDomains", weekSenderDomains);

        return "admin/statistics";
    }

    /**
     * Calculates the aggregate sum of all forwarded, stored, and dropped counts in a chart data transfer object.
     *
     * @param chartData the chart data transfer object to summarize
     * @return the total volume of emails represented across all buckets
     */
    private static long calculateTotalVolume(final ChartDataDto chartData)
    {
        if (chartData == null)
        {
            return 0L;
        }
        final long forwardedSum = chartData.forwarded().stream().mapToLong(Long::longValue).sum();
        final long storedSum = chartData.stored().stream().mapToLong(Long::longValue).sum();
        final long droppedSum = chartData.dropped().stream().mapToLong(Long::longValue).sum();
        return forwardedSum + storedSum + droppedSum;
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
