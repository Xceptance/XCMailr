/**  
 *  Copyright 2020 by the original author or authors.
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
import controllers.restapi.util.AbstractApiController;
import controllers.restapi.util.ApiResults;
import controllers.restapi.util.DbId;
import controllers.restapi.util.Email;
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
 * REST API endpoint for the management of mailboxes.
 */
public class MailboxApiController extends AbstractApiController
{
    @Inject
    XCMailrConf xcmConfiguration;

    @Inject
    Logger log;

    /**
     * Lists all mailboxes of a certain user.
     * 
     * @param userId
     *            the ID of the user (derived from the passed API token in {@link ApiTokenFilter})
     * @param context
     *            the current context
     * @return the user's mailboxes as an array of {@link MailboxData} objects in JSON format
     */
    // @Get("/api/v1/mailboxes")
    public Result listMailboxes(@Attribute("userId") @DbId Long userId, Context context)
    {
        List<MBox> mailboxes = MBox.allUser(userId);
        List<MailboxData> mailboxDatas = mailboxes.stream().map(mb -> new MailboxData(mb)).collect(Collectors.toList());

        return ApiResults.ok().render(mailboxDatas);
    }

    /**
     * Creates a new mailbox for a certain user.
     * 
     * @param mailboxData
     *            the details of the new mailbox
     * @param user
     *            the user (derived from the passed API token in {@link ApiTokenFilter})
     * @param context
     *            the current context
     * @return the details of the new mailbox as a {@link MailboxData} object in JSON format
     */
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
     * Returns the details of a mailbox.
     * 
     * @param mailboxAddress
     *            the ID of the mailbox
     * @param userId
     *            the ID of the user (derived from the passed API token in {@link ApiTokenFilter})
     * @param context
     *            the current context
     * @return the details of the mailbox as a {@link MailboxData} object in JSON format
     */
    // @Get("/api/v1/mailboxes/{mailboxAddress}")
    public Result getMailbox(@PathParam("mailboxAddress") @Email String mailboxAddress,
                             @Attribute("userId") @DbId Long userId, Context context)
    {
        return performAction(userId, mailboxAddress, context, mailbox -> {
            MailboxData mailboxData = new MailboxData(mailbox);
            return ApiResults.ok().render(mailboxData);
        });
    }

    /**
     * Updates an existing mailbox.
     * 
     * @param mailboxAddress
     *            the ID of the mailbox
     * @param mailboxData
     *            the new details of the mailbox
     * @param userId
     *            the ID of the user (derived from the passed API token in {@link ApiTokenFilter})
     * @param context
     *            the current context
     * @return the details of the updated mailbox as a {@link MailboxData} object in JSON format
     */
    // @Put("/api/v1/mailboxes/<mailboxAddress>")
    public Result updateMailbox(@PathParam("mailboxAddress") @Email String mailboxAddress,
                                @JSR303Validation MailboxData mailboxData, @Attribute("userId") @DbId Long userId,
                                Context context)
    {
        return performAction(userId, mailboxAddress, context, mailbox -> doUpdateMailbox(mailbox, mailboxData));
    }

    /**
     * Deletes a mailbox.
     * 
     * @param mailboxAddress
     *            the ID of the mailbox
     * @param userId
     *            the ID of the user (derived from the passed API token in {@link ApiTokenFilter})
     * @param context
     *            the current context
     * @return an empty result
     */
    // @Delete("/api/v1/mailboxes/{mailboxAddress}")
    public Result deleteMailbox(@PathParam("mailboxAddress") @Email String mailboxAddress,
                                @Attribute("userId") @DbId Long userId, Context context)
    {
        return performAction(userId, mailboxAddress, context, mailbox -> {
            mailbox.delete();
            return ApiResults.noContent();
        });
    }

    /**
     * Performs the given action with a mailbox entity. Before the action is applied, the context is checked for
     * validation violations, then the mailbox in question is looked up in the database, and finally some other basic
     * access checks are made.
     * 
     * @param userId
     *            the ID of the user (derived from the passed API token in {@link ApiTokenFilter})
     * @param mailboxAddress
     *            the ID of the mailbox
     * @param context
     *            the current context
     * @param action
     *            the action to be performed with the mailbox
     * @return the result produced by the action, or an error result
     */
    private Result performAction(Long userId, String mailboxAddress, Context context, Function<MBox, Result> action)
    {
        // check the context for violations
        if (context.getValidation().hasViolations())
        {
            return ApiResults.badRequest(context.getValidation().getViolations());
        }

        // get mailbox
        MBox mailbox = MBox.getByAddress(mailboxAddress);
        if (mailbox == null)
        {
            return ApiResults.notFound();
        }

        // check if the current user is the owner of the mailbox
        if (mailbox.getUsr().getId() != userId)
        {
            return ApiResults.forbidden("mailboxAddress", "Mailbox belongs to another user.");
        }

        // perform the action with the mailbox
        return action.apply(mailbox);
    }

    /**
     * Creates a new or reactivates an existing, but inactive mailbox.
     * 
     * @param mailboxData
     *            the details of the mailbox
     * @param user
     *            the user
     * @return the details of the new mailbox as a {@link MailboxData} object in JSON format, or an error result
     */
    private Result doCreateMailbox(MailboxData mailboxData, User user)
    {
        // check domain
        String desiredMailAddress = mailboxData.address;
        String[] mailAddressParts = HelperUtils.splitMailAddress(desiredMailAddress);
        if (!HelperUtils.checkEmailAddressValidness(mailAddressParts, xcmConfiguration.DOMAIN_LIST))
        {
            // mail is not in format "localpart@domain" or domain is not configured in XCMailr
            log.error("Mailbox address invalid: " + desiredMailAddress);
            return ApiResults.forbidden("address", "Domain is not allowed");
        }

        String localPart = mailAddressParts[0];
        String domainPart = mailAddressParts[1];

        // check if email address is already claimed by someone else
        MBox mailbox = MBox.find(localPart, domainPart);
        if (mailbox != null && mailbox.getUsr().getId() != user.getId())
        {
            log.debug("Email address is owned by user: " + mailbox.getUsr().getMail());
            return ApiResults.forbidden("mailboxAddress", "Email address is already taken.");
        }

        // do the right thing
        if (mailbox != null)
        {
            // reactivate mailbox
            log.debug("Reactivate mailbox: " + desiredMailAddress);
            mailbox.setTs_Active(mailboxData.deactivationTime);
            mailbox.setExpired(mailboxData.deactivationTime <= System.currentTimeMillis());
            mailbox.setForwardEmails(mailboxData.forwardEnabled);
            mailbox.save();

            mailboxData = new MailboxData(mailbox);

            return ApiResults.ok().render(mailboxData);
        }
        else
        {
            // create mailbox
            log.info("Create mailbox: " + desiredMailAddress);
            mailbox = new MBox(localPart, domainPart, mailboxData.deactivationTime,
                               mailboxData.deactivationTime <= System.currentTimeMillis(), user);
            mailbox.setForwardEmails(mailboxData.forwardEnabled);
            mailbox.save();

            mailboxData = new MailboxData(mailbox);

            return ApiResults.created().render(mailboxData);
        }
    }

    /**
     * Updates an existing mailbox with new details.
     * 
     * @param mailbox
     *            the mailbox entity to update
     * @param mailboxData
     *            the new details of the mailbox
     * @return the details of the updated mailbox as a {@link MailboxData} object in JSON format, or an error result
     */
    private Result doUpdateMailbox(MBox mailbox, MailboxData mailboxData)
    {
        // check domain
        String desiredMailAddress = mailboxData.address;
        String[] mailAddressParts = HelperUtils.splitMailAddress(desiredMailAddress);
        if (!HelperUtils.checkEmailAddressValidness(mailAddressParts, xcmConfiguration.DOMAIN_LIST))
        {
            // mail is not in format "localpart@domain" or domain is not configured in XCMailr
            log.error("Email address invalid: " + desiredMailAddress);
            return ApiResults.forbidden("mailboxAddress", "Domain is not allowed.");
        }

        String localPart = mailAddressParts[0];
        String domainPart = mailAddressParts[1];

        // check if email address is already claimed by any other mailbox, even one of mine
        MBox mbx = MBox.find(localPart, domainPart);
        if (mbx != null && mbx.getId() != mailbox.getId())
        {
            log.debug("Email address is owned by user: " + mbx.getUsr().getMail());
            return ApiResults.forbidden("mailboxAddress", "Email address is already taken.");
        }

        // update
        mailbox.setAddress(localPart);
        mailbox.setDomain(domainPart);
        mailbox.setTs_Active(mailboxData.deactivationTime);
        mailbox.setExpired(mailboxData.deactivationTime <= System.currentTimeMillis());
        mailbox.setForwardEmails(mailboxData.forwardEnabled);
        mailbox.save();

        // return document
        mailboxData = new MailboxData(mailbox);

        return ApiResults.ok().render(mailboxData);
    }
}
