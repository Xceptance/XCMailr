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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jakarta.activation.DataSource;
import jakarta.mail.internet.MimeMessage;

import com.xceptance.xcmailr.util.JakartaMimeMessageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.security.UserPrincipal;

import controllers.restapi.MailData;
import controllers.restapi.util.ApiError;
import controllers.restapi.util.ApiErrors;
import etc.HelperUtils;
import models.MBox;
import models.Mail;

/**
 * REST API controller for emails received by temporary mailboxes under {@code /api/v1/mails}.
 * Supports querying emails with regex filters, fetching mail details, streaming attachments,
 * and deleting stored emails.
 *
 * @author Xceptance Software Technologies GmbH
 */
@RestController
@RequestMapping(path = "/api/v1/mails")
public class MailApiController
{
    private static final Logger log = LoggerFactory.getLogger(MailApiController.class);

    private final MailRepository mailRepository;
    private final MailboxRepository mailboxRepository;

    /**
     * Constructs the controller with required JPA repositories.
     *
     * @param mailRepository mail persistence repository
     * @param mailboxRepository mailbox persistence repository
     */
    public MailApiController(final MailRepository mailRepository,
                             final MailboxRepository mailboxRepository)
    {
        this.mailRepository = mailRepository;
        this.mailboxRepository = mailboxRepository;
    }

    /**
     * Lists emails for a given mailbox, optionally filtered by regex patterns across headers,
     * subject, sender, and body content.
     *
     * @param principal authenticated user principal
     * @param mailboxAddress required email address of the mailbox
     * @param lastMatch if true, returns only the most recent matching email
     * @param fromPattern regex pattern to match sender address
     * @param subjectPattern regex pattern to match email subject
     * @param textContentPattern regex pattern to match plain text body
     * @param htmlContentPattern regex pattern to match HTML body
     * @param headerPattern regex pattern to match raw MIME headers
     * @return list of matching {@link MailData} DTOs
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> listMails(@AuthenticationPrincipal final UserPrincipal principal,
                                       @RequestParam("mailboxAddress") final String mailboxAddress,
                                       @RequestParam(name = "lastMatch", defaultValue = "false") final boolean lastMatch,
                                       @RequestParam(name = "from", required = false) final String fromPattern,
                                       @RequestParam(name = "subject", required = false) final String subjectPattern,
                                       @RequestParam(name = "textContent", required = false) final String textContentPattern,
                                       @RequestParam(name = "htmlContent", required = false) final String htmlContentPattern,
                                       @RequestParam(name = "mailHeader", required = false) final String headerPattern)
    {
        // Validate mailbox email format
        if (!isValidEmail(mailboxAddress))
        {
            return ResponseEntity.badRequest()
                                 .body(new ApiErrors(List.of(new ApiError("mailboxAddress", "Invalid email address."))));
        }

        // Validate and compile regex filters
        final Pattern compiledFrom;
        final Pattern compiledSubject;
        final Pattern compiledText;
        final Pattern compiledHtml;
        final Pattern compiledHeaders;

        try
        {
            compiledFrom = compileRegex("from", fromPattern);
            compiledSubject = compileRegex("subject", subjectPattern);
            compiledText = compileRegex("textContent", textContentPattern);
            compiledHtml = compileRegex("htmlContent", htmlContentPattern);
            compiledHeaders = compileRegex("mailHeader", headerPattern);
        }
        catch (final RegexValidationException ex)
        {
            return ResponseEntity.badRequest()
                                 .body(new ApiErrors(List.of(new ApiError(ex.getParameterName(), ex.getMessage()))));
        }

        // Lookup mailbox by address and domain
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

        // Retrieve mails ordered chronologically
        final List<Mail> mails = mailRepository.findByMailboxOrderByReceiveTimeAsc(mailbox.getId());
        List<MailData> filteredMails = new ArrayList<>();

        for (final Mail mail : mails)
        {
            try
            {
                final MailData mailData = new MailData(mail, mailbox);

                final boolean matchesSender = (compiledFrom == null || compiledFrom.matcher(mailData.sender).find());
                final boolean matchesSubject = (compiledSubject == null || compiledSubject.matcher(mailData.subject).find());
                final boolean matchesText = (compiledText == null || (mailData.textContent != null && compiledText.matcher(mailData.textContent).find()));
                final boolean matchesHtml = (compiledHtml == null || (mailData.htmlContent != null && compiledHtml.matcher(mailData.htmlContent).find()));
                final boolean matchesHeaders = (compiledHeaders == null || (mailData.headers != null && compiledHeaders.matcher(mailData.headers).find()));

                if (matchesSender && matchesSubject && matchesText && matchesHtml && matchesHeaders)
                {
                    filteredMails.add(mailData);
                }
            }
            catch (final Exception e)
            {
                log.error("Failed to parse mail message with id: {}", mail.getId(), e);
            }
        }

        // Reduce to last match if requested
        if (lastMatch && filteredMails.size() > 1)
        {
            filteredMails = List.of(filteredMails.getLast());
        }

        return ResponseEntity.ok(filteredMails);
    }

    /**
     * Retrieves full details for a single email by ID.
     *
     * @param principal authenticated user principal
     * @param mailIdStr email ID path parameter
     * @return mail details DTO
     */
    @GetMapping(path = "/{mailId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getMail(@AuthenticationPrincipal final UserPrincipal principal,
                                     @PathVariable("mailId") final String mailIdStr)
    {
        final Long mailId = parseId(mailIdStr);
        if (mailId == null)
        {
            return ResponseEntity.badRequest()
                                 .body(new ApiErrors(List.of(new ApiError("mailId", "Invalid mail id: " + mailIdStr))));
        }

        final Optional<Mail> mailOpt = mailRepository.findById(mailId);
        if (mailOpt.isEmpty())
        {
            return ResponseEntity.notFound().build();
        }

        final Mail mail = mailOpt.get();
        final Optional<MBox> mailboxOpt = mailboxRepository.findById(mail.getMailboxId());
        if (mailboxOpt.isEmpty() || mailboxOpt.get().getUsr().getId() != principal.getId())
        {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body(new ApiErrors(List.of(new ApiError("mailId", "Mail belongs to another user."))));
        }

        try
        {
            final MailData mailData = new MailData(mail, mailboxOpt.get());
            return ResponseEntity.ok(mailData);
        }
        catch (final Exception e)
        {
            log.error("Failed to parse MIME content for mail: {}", mailId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Streams an attachment of an email as a binary resource with appropriate Content-Type.
     *
     * @param principal authenticated user principal
     * @param mailIdStr email ID path parameter
     * @param attachmentName file name of the requested attachment
     * @return attachment byte stream resource or 404 if not found
     */
    @GetMapping("/{mailId}/attachments/{attachmentName}")
    public ResponseEntity<Resource> getMailAttachment(@AuthenticationPrincipal final UserPrincipal principal,
                                                      @PathVariable("mailId") final String mailIdStr,
                                                      @PathVariable("attachmentName") final String attachmentName)
    {
        final Long mailId = parseId(mailIdStr);
        if (mailId == null || attachmentName == null || attachmentName.isBlank())
        {
            return ResponseEntity.badRequest().build();
        }

        final Optional<Mail> mailOpt = mailRepository.findById(mailId);
        if (mailOpt.isEmpty())
        {
            return ResponseEntity.notFound().build();
        }

        final Mail mail = mailOpt.get();
        final Optional<MBox> mailboxOpt = mailboxRepository.findById(mail.getMailboxId());
        if (mailboxOpt.isEmpty() || mailboxOpt.get().getUsr().getId() != principal.getId())
        {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try
        {
            final MimeMessage mimeMessage = JakartaMimeMessageParser.createMimeMessage(mail.getMessage());
            final JakartaMimeMessageParser mimeMessageParser = new JakartaMimeMessageParser(mimeMessage).parse();

            for (final DataSource attachment : mimeMessageParser.getAttachmentList())
            {
                if (attachmentName.equalsIgnoreCase(attachment.getName()))
                {
                    try (final InputStream in = attachment.getInputStream())
                    {
                        final byte[] content = in.readAllBytes();
                        final MediaType mediaType;
                        try
                        {
                            mediaType = MediaType.parseMediaType(attachment.getContentType());
                        }
                        catch (final Exception ex)
                        {
                            return ResponseEntity.ok()
                                                 .contentLength(content.length)
                                                 .body(new ByteArrayResource(content));
                        }

                        return ResponseEntity.ok()
                                             .contentType(mediaType)
                                             .contentLength(content.length)
                                             .body(new ByteArrayResource(content));
                    }
                }
            }

            return ResponseEntity.notFound().build();
        }
        catch (final Exception e)
        {
            log.error("Failed to parse attachment '{}' for mail {}", attachmentName, mailId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Deletes a specific email by its ID.
     *
     * @param principal authenticated user principal
     * @param mailIdStr email ID path parameter
     * @return 204 No Content, 404 Not Found, or 403 Forbidden
     */
    @DeleteMapping("/{mailId}")
    public ResponseEntity<?> deleteMail(@AuthenticationPrincipal final UserPrincipal principal,
                                        @PathVariable("mailId") final String mailIdStr)
    {
        final Long mailId = parseId(mailIdStr);
        if (mailId == null)
        {
            return ResponseEntity.badRequest()
                                 .body(new ApiErrors(List.of(new ApiError("mailId", "Invalid mail id: " + mailIdStr))));
        }

        final Optional<Mail> mailOpt = mailRepository.findById(mailId);
        if (mailOpt.isEmpty())
        {
            return ResponseEntity.notFound().build();
        }

        final Mail mail = mailOpt.get();
        final Optional<MBox> mailboxOpt = mailboxRepository.findById(mail.getMailboxId());
        if (mailboxOpt.isEmpty() || mailboxOpt.get().getUsr().getId() != principal.getId())
        {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body(new ApiErrors(List.of(new ApiError("mailId", "Mail belongs to another user."))));
        }

        mailRepository.delete(mail);
        return ResponseEntity.noContent().build();
    }

    private Long parseId(final String idStr)
    {
        if (idStr == null || idStr.isBlank())
        {
            return null;
        }
        try
        {
            final long val = Long.parseLong(idStr);
            return val > 0 ? val : null;
        }
        catch (final NumberFormatException e)
        {
            return null;
        }
    }

    private Pattern compileRegex(final String paramName, final String regex)
    {
        if (regex == null || regex.isEmpty())
        {
            return null;
        }
        try
        {
            return Pattern.compile(regex, Pattern.MULTILINE | Pattern.DOTALL);
        }
        catch (final PatternSyntaxException e)
        {
            throw new RegexValidationException(paramName, "Invalid regular expression: " + regex);
        }
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

    /**
     * Internal exception used for regex pattern syntax error reporting.
     */
    private static class RegexValidationException extends RuntimeException
    {
        private final String parameterName;

        public RegexValidationException(final String parameterName, final String message)
        {
            super(message);
            this.parameterName = parameterName;
        }

        public String getParameterName()
        {
            return parameterName;
        }
    }
}
