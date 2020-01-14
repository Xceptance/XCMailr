/**  
 *  Copyright 2013 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
package controllers.restapi;

import java.util.LinkedList;
import java.util.List;
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
     * Lists the details of all mails in a certain mailbox of a certain user.
     * 
     * @param mailboxId
     *            the ID of the mailbox to list
     * @param userId
     *            the ID of the user (derived from the passed API token in {@link ApiTokenFilter})
     * @param context
     *            the current Ninja context
     * @return the details of the mails as an array of {@link MailData} objects in JSON format
     * @throws Exception
     */
    // @Get("/api/v1/mails?mailboxId={mailboxId}")
    public Result listMails(@Param("mailboxId") @DbId Long mailboxId, @Attribute("userId") @DbId Long userId,
                            Context context)
        throws Exception
    {
        boolean lastMatch = context.getParameter("lastMatch") != null;

        final Pattern senderPattern = createPatternFromParameter("from", context);
        final Pattern subjectPattern = createPatternFromParameter("subject", context);
        final Pattern plainTextPattern = createPatternFromParameter("textContent", context);
        final Pattern htmlTextPattern = createPatternFromParameter("htmlContent", context);
        final Pattern headerPattern = createPatternFromParameter("mailHeader", context);

        // report validation errors if any
        Validation validation = context.getValidation();
        if (validation.hasViolations())
        {
            return ApiResults.badRequest(validation.getViolations());
        }

        // check that the mailbox exists
        MBox mailbox = MBox.getById(mailboxId);
        if (mailbox == null)
        {
            return ApiResults.notFound();
        }

        // check that the mailbox belongs to our user
        if (mailbox.getUsr().getId() != userId)
        {
            return ApiResults.forbidden("mailboxId", "Mailbox belongs to another user.");
        }

        // return the mails
        return findAndFilterMails(mailbox, context, lastMatch, senderPattern, subjectPattern, plainTextPattern,
                                  htmlTextPattern, headerPattern);
    }

    /**
     * Returns the details of a certain mail.
     * 
     * @param id
     *            the mail ID
     * @param userId
     *            the ID of the user (derived from the passed API token in {@link ApiTokenFilter})
     * @param context
     *            the current Ninja context
     * @return the details of the mail as a {@link MailData} object in JSON format
     */
    // @Get("/api/v1/mails/<id>")
    public Result getMail(@PathParam("id") @DbId Long id, @Attribute("userId") @DbId Long userId, Context context)
    {
        return performAction(id, userId, context, mail -> {

            MailData mailData;
            try
            {
                mailData = new MailData(mail);
                return ApiResults.ok().render(mailData);
            }
            catch (Exception e)
            {
                log.error("Failed to parse MIME message", e);
                return ApiResults.internalServerError();
            }
        });
    }

    /**
     * Returns an attachment to a certain mail.
     * 
     * @param id
     *            the ID of the mail
     * @param attachmentName
     *            the (file) name of the attachment
     * @param userId
     *            the ID of the user (derived from the passed API token in {@link ApiTokenFilter})
     * @param context
     *            the current Ninja context
     * @return the attachment with the correct content type set
     */
    // @Get("/api/v1/mails/<id>/attachments/<attachmentName>")
    public Result getMailAttachment(@PathParam("id") @DbId Long id,
                                    @PathParam("attachmentName") @NotBlank String attachmentName,
                                    @Attribute("userId") @DbId Long userId, Context context)
    {
        return performAction(id, userId, context, mail -> {
            return serveMailAttachment(context, mail, attachmentName);
        });
    }

    /**
     * Deletes a certain mail.
     * 
     * @param id
     *            the ID of the mail
     * @param userId
     *            the ID of the user (derived from the passed API token in {@link ApiTokenFilter})
     * @param context
     *            the current Ninja context
     * @return an empty result
     */
    // @Delete("/api/v1/mails/<id>")
    public Result deleteMail(@PathParam("id") @DbId Long id, @Attribute("userId") @DbId Long userId, Context context)
    {
        return performAction(id, userId, context, mail -> {
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
     *            the ID of the mail
     * @param userId
     *            the ID of the user (derived from the passed API token in {@link ApiTokenFilter})
     * @param context
     *            the current Ninja context
     * @param action
     *            the action that manipulates the mail entity and returns a corresponding result
     * @return the result produced by the action, or an error result
     */
    private Result performAction(Long mailId, Long userId, Context context, Function<Mail, Result> action)
    {
        // check the context for violations
        if (context.getValidation().hasViolations())
        {
            return ApiResults.badRequest(context.getValidation().getViolations());
        }

        // get mail
        Mail mail = Mail.find(mailId);
        if (mail == null)
        {
            return ApiResults.notFound();
        }

        // check if the current user is the owner of the mail
        if (mail.getMailbox().getUsr().getId() != userId)
        {
            return ApiResults.forbidden("id", "Mail belongs to another user.");
        }

        // perform the action with the mail
        return action.apply(mail);
    }

    /**
     * Returns all mails in a mailbox that match any of the given patterns. If only the last matching mail is desired
     * 
     * @param mailbox
     *            the mailbox
     * @param context
     *            the current Ninja context
     * @param lastMatch
     *            whether only the most recent matching mail is to be returned
     * @param senderPattern
     *            a pattern to be applied for the sender address
     * @param subjectPattern
     * @param plainTextPattern
     * @param htmlTextPattern
     * @param headerPattern
     * @return
     * @throws Exception
     */
    private Result findAndFilterMails(MBox mailbox, Context context, boolean lastMatch, Pattern senderPattern,
                                      Pattern subjectPattern, Pattern plainTextPattern, Pattern htmlTextPattern,
                                      Pattern headerPattern)
        throws Exception
    {
        // get the mails sorted by receive time
        List<Mail> mails = Mail.findAndSort(mailbox.getId());

        // filter mails
        List<MailData> entries = new LinkedList<>();
        for (Mail mail : mails)
        {
            final MailData mailData = new MailData(mail);

            final MailData.Content mailContent = mailData.mailContent;

            if ((senderPattern == null || senderPattern.matcher(mailData.sender).find()) //
                && (subjectPattern == null || subjectPattern.matcher(mailData.subject).find()) //
                && (plainTextPattern == null
                    || (mailContent != null && plainTextPattern.matcher(mailContent.text).find())) //
                && (htmlTextPattern == null
                    || (mailContent != null && htmlTextPattern.matcher(mailContent.html).find())) //
                && (headerPattern == null || headerPattern.matcher(mailData.mailHeader).find()))
            {
                entries.add(mailData);
            }
        }

        // reduce list to one if so requested
        int size = entries.size();
        if (size > 1 && lastMatch)
        {
            entries = entries.subList(size - 1, size);
        }

        // finally return the list as json
        return ApiResults.ok().render(entries);
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
    private Pattern createPatternFromParameter(String parameterName, Context context)
    {
        String regex = context.getParameter(parameterName);
        Pattern pattern = null;

        try
        {
            if (regex != null)
            {
                pattern = Pattern.compile(regex, Pattern.MULTILINE | Pattern.DOTALL);
            }
        }
        catch (PatternSyntaxException e)
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
    private Result serveMailAttachment(Context context, Mail mail, String attachmentName)
    {
        try
        {
            MimeMessage mimeMessage = MimeMessageUtils.createMimeMessage(null, mail.getMessage());
            MimeMessageParser mimeMessageParser = new MimeMessageParser(mimeMessage);
            mimeMessageParser.parse();

            for (DataSource attachment : mimeMessageParser.getAttachmentList())
            {
                if (attachment.getName().equals(attachmentName))
                {
                    return Results.ok().render(new StreamRenderable(attachment.getInputStream(),
                                                                    attachment.getContentType()));
                }
            }

            return ApiResults.notFound();
        }
        catch (Exception e)
        {
            log.error("Failed to serve mail attachment", e);
            return ApiResults.internalServerError();
        }
    }
}
