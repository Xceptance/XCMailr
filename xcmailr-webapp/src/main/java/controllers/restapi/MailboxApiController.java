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

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.google.inject.Inject;

import conf.XCMailrConf;
import controllers.restapi.unused.DbId;
import controllers.restapi.util.AbstractApiController;
import controllers.restapi.util.ApiResults;
import etc.HelperUtils;
import filters.ApiTokenFilter;
import models.MBox;
import models.User;
import ninja.Context;
import ninja.Result;
import ninja.params.Attribute;
import ninja.params.PathParam;
import ninja.validation.JSR303Validation;

/**
 * Handles all actions for the (virtual) Mailboxes like add, delete and edit box
 */
public class MailboxApiController extends AbstractApiController
{
    @Inject
    XCMailrConf xcmConfiguration;

    @Inject
    Logger log;

    /**
     * @param userId
     *            the ID of the user that is derived from the passed API token (provided by {@link ApiTokenFilter})
     * @param context
     *            the current context
     * @return the
     */
    // @Get("/api/v1/mailboxes")
    public Result listMailboxes(@Attribute("userId") @DbId Long userId, Context context)
    {
        List<MBox> mailboxes = MBox.allUser(userId);
        List<MailboxData> mailboxDatas = mailboxes.stream().map(mb -> new MailboxData(mb)).collect(Collectors.toList());

        return ApiResults.ok().render(mailboxDatas);
    }

    // @Post("/api/v1/mailboxes")
    public Result createMailbox(@JSR303Validation MailboxData mailboxData, @Attribute("user") User user,
                                Context context)
    {
        if (context.getValidation().hasViolations())
        {
            return ApiResults.badRequest(context.getValidation().getViolations());
        }

        return doCreateMailbox(mailboxData, user);
    }

    /**
     * @param userId
     * @param mailboxId
     * @param context
     * @return
     */
    // @Get("/api/v1/mailboxes/{id}")
    public Result getMailbox(@PathParam("id") @DbId Long mailboxId, @Attribute("userId") @DbId Long userId,
                             Context context)
    {
        return performAction(userId, mailboxId, context, mailbox -> {
            MailboxData mailboxData = new MailboxData(mailbox);
            return ApiResults.ok().render(mailboxData);
        });
    }

    /**
     * @param userId
     * @param mailboxId
     * @param mailboxData
     * @param context
     * @return
     */
    // @Put("/api/v1/mailboxes/<id>")
    public Result updateMailbox(@PathParam("id") @DbId Long mailboxId, @JSR303Validation MailboxData mailboxData,
                                @Attribute("userId") @DbId Long userId, Context context)
    {
        return performAction(userId, mailboxId, context, mailbox -> doUpdateMailbox(mailbox, mailboxData));
    }

    /**
     * @param userId
     * @param mailboxId
     * @param context
     * @return
     */
    // @Delete("/api/v1/mailboxes/{id}")
    public Result deleteMailbox(@PathParam("id") @DbId Long mailboxId, @Attribute("userId") @DbId Long userId,
                                Context context)
    {
        return performAction(userId, mailboxId, context, mailbox -> {
            mailbox.delete();
            return ApiResults.noContent();
        });
    }

    /**
     * @param userId
     * @param mailboxId
     * @param context
     * @param action
     * @return
     */
    private Result performAction(Long userId, Long mailboxId, Context context, Function<MBox, Result> action)
    {
        //
        if (context.getValidation().hasViolations())
        {
            return ApiResults.badRequest(context.getValidation().getViolations());
        }

        // get mailbox
        MBox mailbox = MBox.getById(mailboxId);
        if (mailbox == null)
        {
            return ApiResults.notFound();
        }

        // check if the current user is the owner of the mailbox
        if (mailbox.getUsr().getId() != userId)
        {
            return ApiResults.forbidden("id", "Mailbox belongs to another user.");
        }

        // perform the action with the mailbox
        return action.apply(mailbox);
    }

    private Result doCreateMailbox(MailboxData mailboxData, User user)
    {
        // check domain
        String desiredMailAddress = mailboxData.email;
        String[] mailAddressParts = HelperUtils.splitMailAddress(desiredMailAddress);
        if (!HelperUtils.checkEmailAddressValidness(mailAddressParts, xcmConfiguration.DOMAIN_LIST))
        {
            // mail is not in format "localpart@domain" or domain is not configured in XCMailr
            log.error("Email address invalid: " + desiredMailAddress);
            return ApiResults.forbidden("email", "Domain is not allowed.");
        }

        String localPart = mailAddressParts[0];
        String domain = mailAddressParts[1];

        // check if email address is already claimed by someone else
        MBox mailbox = MBox.find(localPart, domain);
        if (mailbox != null && mailbox.getUsr().getId() != user.getId())
        {
            log.debug("Email address is owned by user: " + mailbox.getUsr().getMail());
            return ApiResults.forbidden("email", "Email address is already taken.");
        }

        //
        if (mailbox != null)
        {
            // reactivate address
            log.debug("Reactivate mailbox: " + desiredMailAddress);
            mailbox.setTs_Active(mailboxData.expirationDate);
            mailbox.setExpired(mailboxData.expirationDate <= System.currentTimeMillis());
            mailbox.setForwardEmails(mailboxData.forwardEnabled);
            mailbox.save();

            mailboxData = new MailboxData(mailbox);

            return ApiResults.ok().render(mailboxData);
        }
        else
        {
            // create the address for the current user
            log.info("Create mailbox: " + desiredMailAddress);
            mailbox = new MBox(localPart, domain, mailboxData.expirationDate, false, user);
            mailbox.setForwardEmails(mailboxData.forwardEnabled);
            mailbox.save();

            mailboxData = new MailboxData(mailbox);

            return ApiResults.created().render(mailboxData);
        }
    }

    /**
     * Updates the mailbox with the values in the data object.
     * 
     * @param mailbox
     * @param mailboxData
     * @return
     */
    private Result doUpdateMailbox(MBox mailbox, MailboxData mailboxData)
    {
        // check domain
        String desiredMailAddress = mailboxData.email;
        String[] mailAddressParts = HelperUtils.splitMailAddress(desiredMailAddress);
        if (!HelperUtils.checkEmailAddressValidness(mailAddressParts, xcmConfiguration.DOMAIN_LIST))
        {
            // mail is not in format "localpart@domain" or domain is not configured in XCMailr
            log.error("Email address invalid: " + desiredMailAddress);
            return ApiResults.forbidden("email", "Domain is not allowed.");
        }

        String localPart = mailAddressParts[0];
        String domain = mailAddressParts[1];

        // check if email address is already claimed by another mailbox
        MBox mbx = MBox.find(localPart, domain);
        if (mbx != null && mbx.getId() != mailbox.getId())
        {
            log.debug("Email address is owned by user: " + mbx.getUsr().getMail());
            return ApiResults.forbidden("email", "Email address is already taken.");
        }

        // update
        mailbox.setAddress(localPart);
        mailbox.setDomain(domain);
        mailbox.setTs_Active(mailboxData.expirationDate);
        mailbox.setExpired(mailboxData.expirationDate <= System.currentTimeMillis());
        mailbox.setForwardEmails(mailboxData.forwardEnabled);
        mailbox.save();

        // return document
        mailboxData = new MailboxData(mailbox);

        return ApiResults.ok().render(mailboxData);
    }
}
