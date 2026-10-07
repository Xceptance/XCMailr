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
package com.xceptance.xcmailr.controllers.restapi;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xceptance.xcmailr.config.XcmailrProperties;
import com.xceptance.xcmailr.repositories.DomainRepository;
import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.security.UserPrincipal;

import controllers.restapi.MailboxData;
import controllers.restapi.util.ApiError;
import controllers.restapi.util.ApiErrors;
import etc.HelperUtils;
import models.MBox;

/**
 * REST API controller managing user mailboxes under {@code /api/v1/mailboxes}.
 * Supports mailbox listing, creation/reactivation, details lookup, modification, and deletion.
 *
 * @author Xceptance Software Technologies GmbH
 */
@RestController
@RequestMapping(path = "/api/v1/mailboxes", produces = MediaType.APPLICATION_JSON_VALUE)
public class MailboxApiController
{
    private static final Logger log = LoggerFactory.getLogger(MailboxApiController.class);

    private final MailboxRepository mailboxRepository;
    private final MailRepository mailRepository;
    private final DomainRepository domainRepository;
    private final XcmailrProperties xcmailrProperties;

    /**
     * Constructs the controller with required repositories and configuration.
     *
     * @param mailboxRepository mailbox repository
     * @param mailRepository mail repository
     * @param domainRepository domain repository
     * @param xcmailrProperties application properties
     */
    public MailboxApiController(final MailboxRepository mailboxRepository,
                                final MailRepository mailRepository,
                                final DomainRepository domainRepository,
                                final XcmailrProperties xcmailrProperties)
    {
        this.mailboxRepository = mailboxRepository;
        this.mailRepository = mailRepository;
        this.domainRepository = domainRepository;
        this.xcmailrProperties = xcmailrProperties;
    }

    /**
     * Lists all mailboxes belonging to the authenticated user.
     *
     * @param principal authenticated user principal
     * @return list of mailbox data DTOs
     */
    @GetMapping
    public ResponseEntity<List<MailboxData>> listMailboxes(@AuthenticationPrincipal final UserPrincipal principal)
    {
        final List<MBox> mailboxes = mailboxRepository.findByUsr(principal.getUser());
        final List<MailboxData> result = mailboxes.stream()
                                                  .map(MailboxData::new)
                                                  .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * Creates a new mailbox or reactivates an existing mailbox for the authenticated user.
     *
     * @param principal authenticated user principal
     * @param mailboxData requested mailbox properties
     * @return created or reactivated mailbox data, or error status
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createMailbox(@AuthenticationPrincipal final UserPrincipal principal,
                                          @RequestBody final MailboxData mailboxData)
    {
        if (mailboxData == null || !isValidEmail(mailboxData.address))
        {
            return ResponseEntity.badRequest()
                                 .body(new ApiErrors(List.of(new ApiError("address", "Invalid email address."))));
        }

        final String[] parts = HelperUtils.splitMailAddress(mailboxData.address);
        final String localPart = parts[0];
        final String domainPart = parts[1];

        if (!isDomainAllowed(domainPart))
        {
            log.warn("Domain is not allowed for mailbox: {}", mailboxData.address);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body(new ApiErrors(List.of(new ApiError("address", "Domain is not allowed"))));
        }

        final Optional<MBox> existing = mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase(localPart, domainPart);
        if (existing.isPresent())
        {
            final MBox mailbox = existing.get();
            if (mailbox.getUsr().getId() != principal.getId())
            {
                log.debug("Mailbox address already taken by other user: {}", mailboxData.address);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                     .body(new ApiErrors(List.of(new ApiError("mailboxAddress", "Email address is already taken."))));
            }

            // Reactivate existing mailbox owned by this user
            mailbox.setTs_Active(mailboxData.deactivationTime);
            mailbox.setExpired(mailboxData.deactivationTime <= System.currentTimeMillis());
            mailbox.setForwardEmails(mailboxData.forwardEnabled);
            mailboxRepository.save(mailbox);

            return ResponseEntity.ok(new MailboxData(mailbox));
        }
        else
        {
            // Create fresh mailbox
            final MBox mailbox = new MBox(localPart, domainPart, mailboxData.deactivationTime,
                                          mailboxData.deactivationTime <= System.currentTimeMillis(),
                                          principal.getUser());
            mailbox.setForwardEmails(mailboxData.forwardEnabled);
            mailboxRepository.save(mailbox);

            return ResponseEntity.status(HttpStatus.CREATED).body(new MailboxData(mailbox));
        }
    }

    /**
     * Retrieves details for a specific mailbox by its full email address.
     *
     * @param principal authenticated user principal
     * @param mailboxAddress full email address of the mailbox
     * @return mailbox details, 404 if not found, or 403 if owned by another user
     */
    @GetMapping("/{mailboxAddress}")
    public ResponseEntity<?> getMailbox(@AuthenticationPrincipal final UserPrincipal principal,
                                        @PathVariable("mailboxAddress") final String mailboxAddress)
    {
        if (!isValidEmail(mailboxAddress))
        {
            return ResponseEntity.badRequest()
                                 .body(new ApiErrors(List.of(new ApiError("mailboxAddress", "Invalid email address."))));
        }

        final String[] parts = HelperUtils.splitMailAddress(mailboxAddress);
        final Optional<MBox> mailboxOpt = mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase(parts[0], parts[1]);

        if (mailboxOpt.isEmpty())
        {
            return ResponseEntity.notFound().build();
        }

        final MBox mailbox = mailboxOpt.get();
        if (mailbox.getUsr().getId() != principal.getId())
        {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body(new ApiErrors(List.of(new ApiError("mailboxAddress", "Mailbox belongs to another user."))));
        }

        return ResponseEntity.ok(new MailboxData(mailbox));
    }

    /**
     * Updates an existing mailbox with new configuration details.
     *
     * @param principal authenticated user principal
     * @param mailboxAddress current mailbox address
     * @param mailboxData new configuration
     * @return updated mailbox data DTO
     */
    @PutMapping(path = "/{mailboxAddress}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateMailbox(@AuthenticationPrincipal final UserPrincipal principal,
                                           @PathVariable("mailboxAddress") final String mailboxAddress,
                                           @RequestBody final MailboxData mailboxData)
    {
        if (!isValidEmail(mailboxAddress))
        {
            return ResponseEntity.badRequest()
                                 .body(new ApiErrors(List.of(new ApiError("mailboxAddress", "Invalid email address."))));
        }

        final String[] parts = HelperUtils.splitMailAddress(mailboxAddress);
        final Optional<MBox> mailboxOpt = mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase(parts[0], parts[1]);

        if (mailboxOpt.isEmpty())
        {
            return ResponseEntity.notFound().build();
        }

        final MBox mailbox = mailboxOpt.get();
        if (mailbox.getUsr().getId() != principal.getId())
        {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body(new ApiErrors(List.of(new ApiError("mailboxAddress", "Mailbox belongs to another user."))));
        }

        if (mailboxData == null || !isValidEmail(mailboxData.address))
        {
            return ResponseEntity.badRequest()
                                 .body(new ApiErrors(List.of(new ApiError("address", "Invalid email address."))));
        }

        final String[] newParts = HelperUtils.splitMailAddress(mailboxData.address);
        final String newLocal = newParts[0];
        final String newDomain = newParts[1];

        if (!isDomainAllowed(newDomain))
        {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body(new ApiErrors(List.of(new ApiError("mailboxAddress", "Domain is not allowed."))));
        }

        final Optional<MBox> taken = mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase(newLocal, newDomain);
        if (taken.isPresent() && taken.get().getId() != mailbox.getId())
        {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body(new ApiErrors(List.of(new ApiError("mailboxAddress", "Email address is already taken."))));
        }

        mailbox.setAddress(newLocal);
        mailbox.setDomain(newDomain);
        mailbox.setTs_Active(mailboxData.deactivationTime);
        mailbox.setExpired(mailboxData.deactivationTime <= System.currentTimeMillis());
        mailbox.setForwardEmails(mailboxData.forwardEnabled);
        mailboxRepository.save(mailbox);

        return ResponseEntity.ok(new MailboxData(mailbox));
    }

    /**
     * Deletes a mailbox and all its associated stored messages.
     *
     * @param principal authenticated user principal
     * @param mailboxAddress mailbox address to delete
     * @return 204 No Content, 404 Not Found, or 403 Forbidden
     */
    @DeleteMapping("/{mailboxAddress}")
    public ResponseEntity<?> deleteMailbox(@AuthenticationPrincipal final UserPrincipal principal,
                                           @PathVariable("mailboxAddress") final String mailboxAddress)
    {
        if (!isValidEmail(mailboxAddress))
        {
            return ResponseEntity.badRequest()
                                 .body(new ApiErrors(List.of(new ApiError("mailboxAddress", "Invalid email address."))));
        }

        final String[] parts = HelperUtils.splitMailAddress(mailboxAddress);
        final Optional<MBox> mailboxOpt = mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase(parts[0], parts[1]);

        if (mailboxOpt.isEmpty())
        {
            return ResponseEntity.notFound().build();
        }

        final MBox mailbox = mailboxOpt.get();
        if (mailbox.getUsr().getId() != principal.getId())
        {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body(new ApiErrors(List.of(new ApiError("mailboxAddress", "Mailbox belongs to another user."))));
        }

        mailRepository.deleteByMailbox(mailbox.getId());
        mailboxRepository.delete(mailbox);

        return ResponseEntity.noContent().build();
    }

    private boolean isDomainAllowed(final String domain)
    {
        if (domain == null || domain.isBlank())
        {
            return false;
        }

        final List<String> configuredDomains = xcmailrProperties.getMbox().getDomainList();
        if (configuredDomains != null)
        {
            for (final String configured : configuredDomains)
            {
                if (configured.equalsIgnoreCase(domain))
                {
                    return true;
                }
            }
        }

        return domainRepository.existsByDomainnameIgnoreCase(domain);
    }

    private boolean isValidEmail(final String address)
    {
        if (address == null || address.isBlank())
        {
            return false;
        }
        final String[] parts = HelperUtils.splitMailAddress(address);
        return parts != null && parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank() && parts[1].contains(".");
    }
}
