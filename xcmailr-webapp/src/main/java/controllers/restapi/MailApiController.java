/*
 * Copyright (c) 2013-2023 Xceptance Software Technologies GmbH
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
package controllers.restapi;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.activation.DataSource;
import javax.mail.internet.MimeMessage;

import org.apache.commons.mail.util.MimeMessageParser;
import org.apache.commons.mail.util.MimeMessageUtils;
import org.hibernate.validator.constraints.NotBlank;
import org.slf4j.Logger;

import com.google.inject.Inject;

import controllers.restapi.util.AbstractApiController;
import controllers.restapi.util.ApiResults;
import controllers.restapi.util.DbId;
import controllers.restapi.util.Email;
import etc.StreamRenderable;
import filters.ApiTokenFilter;
import models.MBox;
import models.Mail;
import ninja.Context;
import ninja.Result;
import ninja.Results;
import ninja.params.Attribute;
import ninja.params.Param;
import ninja.params.PathParam;
import ninja.validation.ConstraintViolation;
import ninja.validation.Validation;

/**
 * REST API endpoint for the management of mails.
 */
public class MailApiController extends AbstractApiController
{
    @Inject
    Logger log;

    /**
     * Lists the details of all mails in a certain mailbox.
     *
     * @param mailboxAddress
     *            the address of the mailbox to list
     * @param userId
     *            the ID of the user (derived from the passed API token in {@link ApiTokenFilter})
     * @param context
     *            the current Ninja context
     * @return the details of the mails as an array of {@link MailData} objects in JSON format
     * @throws Exception
     */
    // @Get("/api/v1/mails?mailboxAddress={mailboxAddress}")
    public Result listMails(@Param("mailboxAddress") @Email final String mailboxAddress,
                            @Attribute("userId") @DbId final Long userId, final Context context)
        throws Exception
    {
        final boolean lastMatch = context.getParameterAs("lastMatch", boolean.class, false);

        final Pattern senderPattern = createPatternFromParameter("from", context);
        final Pattern subjectPattern = createPatternFromParameter("subject", context);
        final Pattern plainTextPattern = createPatternFromParameter("textContent", context);
        final Pattern htmlTextPattern = createPatternFromParameter("htmlContent", context);
        final Pattern headerPattern = createPatternFromParameter("mailHeader", context);

        // report validation errors if any
        final Validation validation = context.getValidation();
        if (validation.hasViolations())
        {
            return ApiResults.badRequest(validation.getViolations());
        }

        // check that the mailbox exists
        final MBox mailbox = MBox.getByAddress(mailboxAddress);
        if (mailbox == null)
        {
            return ApiResults.notFound();
        }

        // check that the mailbox belongs to our user
        if (mailbox.getUsr().getId() != userId)
        {
            return ApiResults.forbidden("mailboxAddress", "Mailbox belongs to another user.");
        }

        // return the mails
        return findAndFilterMails(mailbox, context, lastMatch, senderPattern, subjectPattern, plainTextPattern,
                                  htmlTextPattern, headerPattern);
    }

    /**
     * Returns the details of a certain mail.
     *
     * @param mailId
     *            the mail ID
     * @param userId
     *            the ID of the user (derived from the passed API token in {@link ApiTokenFilter})
     * @param context
     *            the current Ninja context
     * @return the details of the mail as a {@link MailData} object in JSON format
     */
    // @Get("/api/v1/mails/<mailId>")
    public Result getMail(@PathParam("mailId") @DbId final Optional<Long> mailId, @Attribute("userId") @DbId final Long userId,
                          final Context context)
    {
        return performAction(mailId, userId, context, mail -> {
            try
            {
                final MailData mailData = new MailData(mail);
                return ApiResults.ok().render(mailData);
            }
            catch (final Exception e)
            {
                log.error("Failed to parse MIME message", e);
                return ApiResults.internalServerError();
            }
        });
    }

    /**
     * Returns an attachment to a certain mail.
     *
     * @param mailId
     *            the ID of the mail
     * @param attachmentName
     *            the (file) name of the attachment
     * @param userId
     *            the ID of the user (derived from the passed API token in {@link ApiTokenFilter})
     * @param context
     *            the current Ninja context
     * @return the attachment with the correct content type set
     */
    // @Get("/api/v1/mails/<mailId>/attachments/<attachmentName>")
    public Result getMailAttachment(@PathParam("mailId") @DbId final Optional<Long> mailId,
                                    @PathParam("attachmentName") @NotBlank final String attachmentName,
                                    @Attribute("userId") @DbId final Long userId, final Context context)
    {
        return performAction(mailId, userId, context, mail -> {
            return serveMailAttachment(context, mail, attachmentName);
        });
    }

    /**
     * Deletes a certain mail.
     *
     * @param mailId
     *            the ID of the mail
     * @param userId
     *            the ID of the user (derived from the passed API token in {@link ApiTokenFilter})
     * @param context
     *            the current Ninja context
     * @return an empty result
     */
    // @Delete("/api/v1/mails/<mailId>")
    public Result deleteMail(@PathParam("mailId") @DbId final Optional<Long> mailId, @Attribute("userId") @DbId final Long userId,
                             final Context context)
    {
        return performAction(mailId, userId, context, mail -> {
            mail.delete();
            return ApiResults.noContent();
        });
    }

    /**
     * Performs the given action with a mail entity. Before the action is applied, the context is checked for validation
     * violations, then the mail in question is looked up in the database, and finally some other basic access checks
     * are made.
     *
     * @param mailId
     *            the ID of the mail with which to perform the action
     * @param userId
     *            the ID of the user (derived from the passed API token in {@link ApiTokenFilter})
     * @param context
     *            the current Ninja context
     * @param action
     *            the action that manipulates the mail entity and returns a corresponding result
     * @return the result produced by the action, or an error result
     */
    private Result performAction(final Optional<Long> mailId, final Long userId, final Context context,
                                 final Function<Mail, Result> action)
    {
        // check the context for violations
        if (context.getValidation().hasViolations())
        {
            return ApiResults.badRequest(context.getValidation().getViolations());
        }

        if(mailId.isEmpty())
        {
            return ApiResults.badRequest(List.of());
        }
        // get mail
        final Mail mail = Mail.find(mailId.get());
        if (mail == null)
        {
            return ApiResults.notFound();
        }

        // check if the current user is the owner of the mail
        if (mail.getMailbox().getUsr().getId() != userId)
        {
            return ApiResults.forbidden("mailId", "Mail belongs to another user.");
        }

        // perform the action with the mail
        return action.apply(mail);
    }

    /**
     * Returns all mails in a mailbox that match all of the given patterns.
     *
     * @param mailbox
     *            the mailbox
     * @param context
     *            the current Ninja context
     * @param lastMatch
     *            whether only the most recent matching mail is to be returned
     * @param senderPattern
     *            the pattern the sender address must match
     * @param subjectPattern
     *            the pattern the subject must match
     * @param plainTextPattern
     *            the pattern the text content must match
     * @param htmlTextPattern
     *            the pattern the HTML content must match
     * @param headerPattern
     *            the pattern the mail headers must match
     * @return the details of the mails as an array of {@link MailData} objects in JSON format
     * @throws Exception
     */
    private Result findAndFilterMails(final MBox mailbox, final Context context, final boolean lastMatch,
                                      final Pattern senderPattern, final Pattern subjectPattern,
                                      final Pattern plainTextPattern, final Pattern htmlTextPattern,
                                      final Pattern headerPattern)
        throws Exception
    {
        // get the mails sorted by receive time
        final List<Mail> mails = Mail.findAndSort(mailbox.getId());

        // filter mails
        List<MailData> filteredMails = new LinkedList<>();
        for (final Mail mail : mails)
        {
            final MailData mailData = new MailData(mail);

            if ((senderPattern == null || senderPattern.matcher(mailData.sender).find()) //
                && (subjectPattern == null || subjectPattern.matcher(mailData.subject).find()) //
                && (plainTextPattern == null
                    || (mailData.textContent != null && plainTextPattern.matcher(mailData.textContent).find())) //
                && (htmlTextPattern == null
                    || (mailData.htmlContent != null && htmlTextPattern.matcher(mailData.htmlContent).find())) //
                && (headerPattern == null || headerPattern.matcher(mailData.headers).find()))
            {
                filteredMails.add(mailData);
            }
        }

        // reduce list to one if so requested
        final int size = filteredMails.size();
        if (size > 1 && lastMatch)
        {
            filteredMails = filteredMails.subList(size - 1, size);
        }

        // finally return the list as json
        return ApiResults.ok().render(filteredMails);
    }

    /**
     * Looks the parameter with the given name up in the context and compiles its value to a {@link Pattern} object. If
     * the value could not be compiled successfully, a corresponding violation will be added to the context.
     *
     * @param parameterName
     *            the name of the parameter to check
     * @param context
     *            the current context
     * @return the compiled pattern, or <code>null</code> if the value could not be compiled to a pattern
     */
    private Pattern createPatternFromParameter(final String parameterName, final Context context)
    {
        final String regex = context.getParameter(parameterName);
        Pattern pattern = null;

        try
        {
            if (regex != null)
            {
                pattern = Pattern.compile(regex, Pattern.MULTILINE | Pattern.DOTALL);
            }
        }
        catch (final PatternSyntaxException e)
        {
            context.getValidation()
                   .addViolation(new ConstraintViolation(null, parameterName, "Invalid regular expression: " + regex));
        }

        return pattern;
    }

    /**
     * Serves the attachment of a mail to the client.
     *
     * @param context
     *            the current Ninja context
     * @param attachmentName
     *            the attachment name
     * @return
     */
    private Result serveMailAttachment(final Context context, final Mail mail, final String attachmentName)
    {
        try
        {
            final MimeMessage mimeMessage = MimeMessageUtils.createMimeMessage(null, mail.getMessage());
            final MimeMessageParser mimeMessageParser = new MimeMessageParser(mimeMessage);
            mimeMessageParser.parse();

            for (final DataSource attachment : mimeMessageParser.getAttachmentList())
            {
                if (attachment.getName().equals(attachmentName))
                {
                    return Results.ok().render(new StreamRenderable(attachment.getInputStream(),
                                                                    attachment.getContentType()));
                }
            }

            return ApiResults.notFound();
        }
        catch (final Exception e)
        {
            log.error("Failed to serve mail attachment", e);
            return ApiResults.internalServerError();
        }
    }
}
