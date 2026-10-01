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

import java.io.InputStream;
import java.security.Principal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;
import com.xceptance.xcmailr.services.EmailSanitizationService;
import com.xceptance.xcmailr.util.JakartaMimeMessageParser;

import jakarta.activation.DataSource;
import jakarta.mail.internet.MimeMessage;
import models.MBox;
import models.Mail;
import models.User;

/**
 * Controller for viewing stored emails, inspecting sanitized message bodies,
 * and downloading message attachments.
 *
 * @author Xceptance Software Technologies GmbH
 */
@Controller
public class MailWebController
{
    private static final Logger LOG = LoggerFactory.getLogger(MailWebController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                                                             .withZone(ZoneId.systemDefault());

    private final MailRepository mailRepository;
    private final MailboxRepository mailboxRepository;
    private final UserRepository userRepository;
    private final EmailSanitizationService sanitizationService;

    public MailWebController(final MailRepository mailRepository,
                             final MailboxRepository mailboxRepository,
                             final UserRepository userRepository,
                             final EmailSanitizationService sanitizationService)
    {
        this.mailRepository = mailRepository;
        this.mailboxRepository = mailboxRepository;
        this.userRepository = userRepository;
        this.sanitizationService = sanitizationService;
    }

    /**
     * Mailbox inbox message list view.
     *
     * @param boxId mailbox ID
     * @param hxRequest header indicating HTMX request
     * @param principal authenticated user
     * @param model template model
     * @return view or fragment name
     */
    @GetMapping("/mailboxes/{boxId}/mails")
    public String listMails(@PathVariable("boxId") final long boxId,
                            @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                            final Principal principal,
                            final Model model)
    {
        final User user = getUser(principal);
        final MBox mailbox = getMailboxForUser(boxId, user);

        final List<Mail> mails = new ArrayList<>(mailRepository.findByMailboxOrderByReceiveTimeAsc(boxId));
        // Sort descending by receive timestamp (most recent first)
        mails.sort(Comparator.comparingLong(Mail::getReceiveTime).reversed());

        model.addAttribute("mailbox", mailbox);
        model.addAttribute("mails", mails);
        model.addAttribute("dateFormatter", DATE_FORMATTER);

        if ("true".equalsIgnoreCase(hxRequest))
        {
            return "mailboxes/fragments/mail-list :: mailList";
        }

        return "mailboxes/mails";
    }

    /**
     * Views details of a specific email, rendering sanitized HTML or plain text body.
     *
     * @param boxId mailbox ID
     * @param mailId mail ID
     * @param hxRequest header indicating HTMX request
     * @param principal authenticated user
     * @param model template model
     * @return view or fragment name
     */
    @GetMapping("/mailboxes/{boxId}/mails/{mailId}")
    public String viewMail(@PathVariable("boxId") final long boxId,
                           @PathVariable("mailId") final long mailId,
                           @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                           final Principal principal,
                           final Model model)
    {
        final User user = getUser(principal);
        final MBox mailbox = getMailboxForUser(boxId, user);
        final Mail mail = getMailForMailbox(mailId, mailbox);

        try
        {
            final MimeMessage mimeMessage = JakartaMimeMessageParser.createMimeMessage(mail.getMessage());
            final JakartaMimeMessageParser parser = new JakartaMimeMessageParser(mimeMessage).parse();

            final String rawHtml = parser.getHtmlContent();
            final String sanitizedHtml = (rawHtml != null && !rawHtml.isBlank())
                ? sanitizationService.sanitizeHtml(rawHtml)
                : null;
            final String plainText = parser.getPlainContent();
            final String headers = JakartaMimeMessageParser.getHeaderText(mimeMessage);

            final List<AttachmentInfo> attachments = new ArrayList<>();
            final List<DataSource> dsList = parser.getAttachmentList();
            for (int i = 0; i < dsList.size(); i++)
            {
                final DataSource ds = dsList.get(i);
                final String name = (ds.getName() != null && !ds.getName().isBlank()) ? ds.getName() : "attachment-" + (i + 1);
                attachments.add(new AttachmentInfo(i, name, ds.getContentType()));
            }

            model.addAttribute("mailbox", mailbox);
            model.addAttribute("mail", mail);
            model.addAttribute("sanitizedHtml", sanitizedHtml);
            model.addAttribute("plainText", plainText);
            model.addAttribute("headers", headers);
            model.addAttribute("attachments", attachments);
            model.addAttribute("dateFormatter", DATE_FORMATTER);

            if ("true".equalsIgnoreCase(hxRequest))
            {
                return "mailboxes/fragments/mail-detail :: detail";
            }

            return "mailboxes/mail-view";
        }
        catch (final Exception e)
        {
            LOG.error("Failed to parse email message {}", mailId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error parsing email message", e);
        }
    }

    /**
     * Downloads an attachment belonging to an email.
     *
     * @param boxId mailbox ID
     * @param mailId mail ID
     * @param index attachment index (0-based)
     * @param principal authenticated user
     * @return streamed attachment resource
     */
    @GetMapping("/mailboxes/{boxId}/mails/{mailId}/attachments/{index}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable("boxId") final long boxId,
                                                       @PathVariable("mailId") final long mailId,
                                                       @PathVariable("index") final int index,
                                                       final Principal principal)
    {
        final User user = getUser(principal);
        final MBox mailbox = getMailboxForUser(boxId, user);
        final Mail mail = getMailForMailbox(mailId, mailbox);

        try
        {
            final MimeMessage mimeMessage = JakartaMimeMessageParser.createMimeMessage(mail.getMessage());
            final JakartaMimeMessageParser parser = new JakartaMimeMessageParser(mimeMessage).parse();

            final List<DataSource> dsList = parser.getAttachmentList();
            if (index < 0 || index >= dsList.size())
            {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment index out of bounds: " + index);
            }

            final DataSource ds = dsList.get(index);
            final String fileName = (ds.getName() != null && !ds.getName().isBlank())
                ? ds.getName()
                : "attachment-" + (index + 1);

            final InputStream in = ds.getInputStream();
            final InputStreamResource resource = new InputStreamResource(in);

            return ResponseEntity.ok()
                                 .contentType(MediaType.parseMediaType(ds.getContentType()))
                                 .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                                 .body(resource);
        }
        catch (final ResponseStatusException rse)
        {
            throw rse;
        }
        catch (final Exception e)
        {
            LOG.error("Failed to download attachment {} for mail {}", index, mailId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error reading attachment", e);
        }
    }

    /**
     * Downloads the raw MIME (.eml) message payload.
     *
     * @param boxId mailbox ID
     * @param mailId mail ID
     * @param principal authenticated user
     * @return raw EML file stream
     */
    @GetMapping("/mailboxes/{boxId}/mails/{mailId}/raw")
    public ResponseEntity<Resource> downloadRawMessage(@PathVariable("boxId") final long boxId,
                                                       @PathVariable("mailId") final long mailId,
                                                       final Principal principal)
    {
        final User user = getUser(principal);
        final MBox mailbox = getMailboxForUser(boxId, user);
        final Mail mail = getMailForMailbox(mailId, mailbox);

        final ByteArrayResource resource = new ByteArrayResource(mail.getMessage());
        final String fileName = (mail.getUuid() != null ? mail.getUuid() : "mail-" + mailId) + ".eml";

        return ResponseEntity.ok()
                             .contentType(MediaType.parseMediaType("message/rfc822"))
                             .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                             .body(resource);
    }

    /**
     * Deletes a single email message.
     *
     * @param boxId mailbox ID
     * @param mailId mail ID
     * @param hxRequest header indicating HTMX request
     * @param principal authenticated user
     * @return redirect or HTMX response
     */
    @DeleteMapping("/mailboxes/{boxId}/mails/{mailId}")
    public Object deleteMail(@PathVariable("boxId") final long boxId,
                             @PathVariable("mailId") final long mailId,
                             @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                             final Principal principal)
    {
        final User user = getUser(principal);
        final MBox mailbox = getMailboxForUser(boxId, user);
        final Mail mail = getMailForMailbox(mailId, mailbox);

        mailRepository.delete(mail);
        LOG.info("Deleted email {} from mailbox {} for user {}", mailId, boxId, user.getMail());

        if ("true".equalsIgnoreCase(hxRequest))
        {
            final HttpHeaders headers = new HttpHeaders();
            headers.add("HX-Trigger", "mailListChanged");
            return ResponseEntity.ok().headers(headers).body("");
        }

        return "redirect:/mailboxes/" + boxId + "/mails?deleted";
    }

    /**
     * Purges all emails in a mailbox.
     *
     * @param boxId mailbox ID
     * @param hxRequest header indicating HTMX request
     * @param principal authenticated user
     * @return redirect or HTMX response
     */
    @DeleteMapping("/mailboxes/{boxId}/mails")
    public Object purgeMails(@PathVariable("boxId") final long boxId,
                             @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                             final Principal principal)
    {
        final User user = getUser(principal);
        getMailboxForUser(boxId, user);

        mailRepository.deleteByMailbox(boxId);
        LOG.info("Purged all emails from mailbox {} for user {}", boxId, user.getMail());

        if ("true".equalsIgnoreCase(hxRequest))
        {
            final HttpHeaders headers = new HttpHeaders();
            headers.add("HX-Trigger", "mailListChanged");
            return ResponseEntity.ok().headers(headers).body("");
        }

        return "redirect:/mailboxes/" + boxId + "/mails?purged";
    }

    private User getUser(final Principal principal)
    {
        if (principal == null)
        {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User must be authenticated");
        }
        return userRepository.findByMailIgnoreCase(principal.getName())
                             .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

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

    private Mail getMailForMailbox(final long mailId, final MBox mailbox)
    {
        final Mail mail = mailRepository.findById(mailId)
                                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email not found"));
        if (mail.getMailboxId() != mailbox.getId())
        {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Email does not belong to specified mailbox");
        }
        return mail;
    }

    /**
     * DTO containing display metadata for an email attachment.
     */
    public record AttachmentInfo(int index, String name, String contentType) {}
}
