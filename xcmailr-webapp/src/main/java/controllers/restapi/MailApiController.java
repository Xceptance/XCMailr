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

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.activation.DataSource;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.mail.util.MimeMessageParser;
import org.apache.commons.mail.util.MimeMessageUtils;
import org.hibernate.validator.constraints.NotBlank;
import org.slf4j.Logger;

import com.google.inject.Inject;

import controllers.restapi.util.AbstractApiController;
import controllers.restapi.util.ApiResults;
import controllers.restapi.util.DbId;
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
 * Handles all actions for the (virtual) Mailboxes like add, delete and edit box
 */
public class MailApiController extends AbstractApiController
{
    @Inject
    Logger log;

    /**
     * @param mailboxId
     * @param userId
     *            the ID of the user
     * @param context
     *            the Ninja context
     * @return
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
     * @param id
     * @param userId
     * @param context
     * @return
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
     * @param id
     * @param attachmentName
     * @param userId
     * @param context
     * @return
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
     * @param id
     * @param userId
     * @param context
     * @return
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
     * @param mailId
     * @param userId
     * @param context
     * @param action
     * @return
     */
    private Result performAction(Long mailId, Long userId, Context context, Function<Mail, Result> action)
    {
        //
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
     * @param mailbox
     * @param context
     * @param lastMatch
     * @param senderPattern
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
        //
        List<Mail> mails = Mail.findAndSort(mailbox.getId());

        //
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

        //
        if (!entries.isEmpty() && lastMatch)
        {
            int size = entries.size();
            entries = entries.subList(size - 1, size);
        }

        return Results.json().render(entries);
    }

    /**
     * @param fieldName
     * @param context
     * @return
     */
    private Pattern createPatternFromParameter(String fieldName, Context context)
    {
        String regex = context.getParameter(fieldName);
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
                   .addViolation(new ConstraintViolation(null, fieldName, "Invalid regular expression: " + regex));
        }

        return pattern;
    }

    /**
     * @param context
     * @param downloadToken
     * @param attachmentName
     * @return
     * @throws Exception
     */
    private Result serveMailAttachment(Context context, Mail mail, String attachmentName)
    {
        try
        {
            MimeMessage mimeMessage = MimeMessageUtils.createMimeMessage(null, mail.getMessage());
            MimeMessageParser mimeMessageParser = new MimeMessageParser(mimeMessage);
            mimeMessageParser.parse();

            DataSource foundAttachment = null;
            for (DataSource attachment : mimeMessageParser.getAttachmentList())
            {
                if (attachment.getName().equals(attachmentName))
                {
                    foundAttachment = attachment;
                    break;
                }
            }

            if (foundAttachment == null)
            {
                return ApiResults.notFound();
            }

            final ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
            try (final InputStream is = foundAttachment.getInputStream())
            {
                IOUtils.copy(is, baos);
            }

            return Results.ok().contentType(foundAttachment.getContentType()).renderRaw(baos.toByteArray());
        }
        catch (Exception e)
        {
            log.error("Failed to serve mail attachment", e);
            return ApiResults.internalServerError();
        }
    }
}
