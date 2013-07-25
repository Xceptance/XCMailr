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
package controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import ninja.Context;
import ninja.FilterWith;
import ninja.Results;
import etc.HelperUtils;
import filters.SecureFilter;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import conf.XCMailrConf;
import models.MBox;
import models.MailBoxFormData;
import models.PageList;
import models.User;
import ninja.params.Param;
import ninja.params.PathParam;
import ninja.validation.JSR303Validation;
import ninja.validation.Validation;
import ninja.Result;

/**
 * Handles all actions for the (virtual) Mailboxes like add, delete and edit box
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */

@FilterWith(SecureFilter.class)
@Singleton
public class BoxHandler
{
    @Inject
    XCMailrConf xcmConfiguration;

    private static final Pattern PATTERN_CS_BOXIDS = Pattern.compile("[(\\d+)][(\\,)(\\d+)]*");
    /**
     * Shows the "new Mail-Forward"-Page <br/>
     * GET /mail/add
     * 
     * @param context
     *            the Context of this Request
     * @return a prepopulated "Add-Box"-Form
     */
    public Result addBoxForm(Context context)
    {
        MailBoxFormData mailboxFormData = new MailBoxFormData();
        // set the value of the random-name to 7
        // use the lowercase, we handle the address as case-insensitive
        String randomName = HelperUtils.getRandomString(7).toLowerCase();
        mailboxFormData.setAddress(randomName);

        // check that the generated mailname-proposal does not exist
        String[] domains = xcmConfiguration.DOMAIN_LIST;
        if (domains.length > 0)
        {// prevent OutOfBoundException
            while (MBox.mailExists(randomName, domains[0]))
            {
                randomName = HelperUtils.getRandomString(7).toLowerCase();
            }
        }

        // set a default entry for the validity-period
        // per default now+1h
        long nowPlusOneHour = DateTime.now().plusHours(1).getMillis();
        mailboxFormData.setDatetime(HelperUtils.parseStringTs(nowPlusOneHour));
        mailboxFormData.setDomain(domains[0]);

        return Results.html().render("domain", domains).render("mbFrmDat", mailboxFormData);
    }

    /**
     * Adds a Mailbox to the {@link User}-Account <br/>
     * POST of /mail/add
     * 
     * @param context
     *            the Context of this Request
     * @param mailboxFormData
     *            the Data of the Mailbox-Add-Form
     * @param validation
     *            Form validation
     * @return the Add-Box-Form (on Error) or the Box-Overview
     */
    public Result addBoxProcess(Context context, @JSR303Validation MailBoxFormData mailboxFormData,
                                Validation validation)
    {
        Result result = Results.html().template("/views/BoxHandler/addBoxForm.ftl.html");
        result.render("domain", xcmConfiguration.DOMAIN_LIST);
        result.render("mbFrmDat", mailboxFormData);

        if (validation.hasViolations())
        { // not all fields were filled (correctly)
            context.getFlashCookie().error("flash_FormError");
            return result;
        }
        else
        {
            // check for rfc 5321 compliant length of email (64 chars for local and 254 in total)
            String completeAddress = mailboxFormData.getAddress() + "@" + mailboxFormData.getDomain();
            if (mailboxFormData.getAddress().length() > 64 || completeAddress.length() > 254)
            {
                context.getFlashCookie().error("createEmail_Flash_MailTooLong");
                return result;
            }
            // checks whether the email address already exists
            if (MBox.mailExists(mailboxFormData.getAddress(), mailboxFormData.getDomain()))
            {
                // the mailbox already exists
                context.getFlashCookie().error("flash_MailExists");
                return result;
            }
            else
            {
                String mailBoxName = mailboxFormData.getAddress().toLowerCase();
                // set the data of the box
                String[] domains = xcmConfiguration.DOMAIN_LIST;
                if (!Arrays.asList(domains).contains(mailboxFormData.getDomain()))
                { // the new domain-name does not exist in the application.conf
                  // stop the process and return to the mailbox-overview page
                    return Results.redirect(context.getContextPath() + "/mail");
                }
                Long ts = HelperUtils.parseTimeString(mailboxFormData.getDatetime());
                if (ts == -1L)
                { // show an error-page if the timestamp is faulty
                    context.getFlashCookie().error("flash_FormError");
                    return result;
                }
                if ((ts != 0) && (ts < DateTime.now().getMillis()))
                { // the Timestamp lays in the past
                    context.getFlashCookie().error("createEmail_Past_Timestamp");
                    return result;
                }

                // create the MBox
                User user = context.getAttribute("user", User.class);
                MBox mailBox = new MBox(mailBoxName, mailboxFormData.getDomain(), ts, false, user);

                // creates the Box in the DB
                mailBox.save();
                return Results.redirect(context.getContextPath() + "/mail");
            }
        }
    }

    /**
     * Deletes a Box from the DB <br/>
     * POST /mail/delete/{id}
     * 
     * @param boxId
     *            the ID of the Mailbox
     * @param context
     *            the Context of this Request
     * @return the Mailbox-Overview-Page
     */
    public Result deleteBoxProcess(@PathParam("id") Long boxId, Context context)
    {
        User user = context.getAttribute("user", User.class);
        if (MBox.boxToUser(boxId, user.getId()))
        { // deletes the box from DB
            MBox.delete(boxId);
        }
        return Results.redirect(context.getContextPath() + "/mail");
    }

    /**
     * Edits a Mailbox <br/>
     * POST /mail/edit/{id}
     * 
     * @param context
     *            the Context of this Request
     * @param boxId
     *            the ID of a Mailbox
     * @param mailboxFormData
     *            the Data of the Mailbox-Edit-Form
     * @param validation
     *            Form validation
     * @return Mailbox-Overview-Page or the Mailbox-Form with an Error- or Success-Message
     */
    public Result editBoxProcess(Context context, @PathParam("id") Long boxId,
                                 @JSR303Validation MailBoxFormData mailboxFormData, Validation validation)
    {
        Result result = Results.html().template("/views/BoxHandler/editBoxForm.ftl.html");
        mailboxFormData.setBoxId(boxId);
        result.render("mbFrmDat", mailboxFormData);
        result.render("domain", xcmConfiguration.DOMAIN_LIST);

        if (validation.hasViolations())
        { // not all fields were filled
            context.getFlashCookie().error("flash_FormError");
            return result;
        }
        else
        { // the form was filled correctly
          // check for rfc 5322 compliant length of email (64 chars for local and 254 in total)
            String completeAddress = mailboxFormData.getAddress() + "@" + mailboxFormData.getDomain();
            if (mailboxFormData.getAddress().length() > 64 || completeAddress.length() >= 255)
            {
                context.getFlashCookie().error("editEmail_Flash_MailTooLong");
                return result;
            }
            // we got the boxID with the POST-Request
            MBox mailBox = MBox.getById(boxId);
            User usr = context.getAttribute("user", User.class);
            if (mailBox != null && mailBox.belongsTo(usr.getId()))
            { // the box with the given id exists and the current user is the owner of the mailbox
                boolean changes = false;
                String newLocalPartName = mailboxFormData.getAddress().toLowerCase();
                String newDomainPartName = mailboxFormData.getDomain().toLowerCase();

                if (!mailBox.getAddress().equals(newLocalPartName) || !mailBox.getDomain().equals(newDomainPartName))
                { // mailaddress was changed
                    if (!MBox.mailExists(newLocalPartName, newDomainPartName))
                    { // the new address does not exist
                        String[] domains = xcmConfiguration.DOMAIN_LIST;
                        // assume that the POST-Request was modified and the domainname does not exist in our app
                        if (!Arrays.asList(domains).contains(newDomainPartName))
                        {
                            // the new domainname does not exist in the application.conf
                            // stop the process and return to the mailbox-overview page
                            return Results.redirect(context.getContextPath() + "/mail");
                        }
                        mailBox.setAddress(newLocalPartName);
                        mailBox.setDomain(newDomainPartName);
                        changes = true;
                    }
                    else
                    {
                        // the email-address already exists
                        context.getFlashCookie().error("flash_MailExists");
                        return result;
                    }
                }
                Long ts = HelperUtils.parseTimeString(mailboxFormData.getDatetime());
                if (ts == -1)
                { // a faulty timestamp was given -> return an errorpage
                    context.getFlashCookie().error("flash_FormError");
                    return result;
                }
                if ((ts != 0) && (ts < DateTime.now().getMillis()))
                { // the Timestamp lays in the past
                    context.getFlashCookie().error("editEmail_Past_Timestamp");
                    return result;
                }

                if (mailBox.getTs_Active() != ts)
                { // check if the MBox-TS is unequal to the given TS in the form
                    mailBox.setTs_Active(ts);
                    changes = true;
                }

                // Updates the Box if changes were made
                if (changes)
                {
                    mailBox.setExpired(false);
                    mailBox.update();
                    context.getFlashCookie().success("flash_DataChangeSuccess");
                }

            }
        }
        // the current user is not the owner of the mailbox,
        // the given box-id does not exist,
        // or the editing-process was successful
        return Results.redirect(context.getContextPath() + "/mail");
    }

    /**
     * Shows the Edit-Form for the Box with the given boxId. <br/>
     * GET /mail/edit/{id}
     * 
     * @param context
     *            the Context of this Request
     * @param boxId
     *            ID of the Box
     * @return the Mailbox-Edit-Form with prepopulated values
     */
    public Result editBoxForm(Context context, @PathParam("id") Long boxId)
    {

        MBox mailBox = MBox.getById(boxId);

        if (mailBox == null)
        { // there's no box with that id
            return Results.redirect(context.getContextPath() + "/mail");
        }
        else
        { // the box exists, go on!
            User usr = context.getAttribute("user", User.class);

            if (mailBox.belongsTo(usr.getId()))
            { // the MBox belongs to this user
              // render the box-data and domains

                if ((mailBox.getTs_Active() <= DateTime.now().getMillis()) && mailBox.getTs_Active() != 0)
                {
                    // set a new activity-timestamp if the current one is in the past
                    DateTime dateTime = new DateTime().plusHours(1);
                    mailBox.setTs_Active(dateTime.getMillis());
                }
                MailBoxFormData mailBoxFormData = MailBoxFormData.prepopulate(mailBox);
                return Results.html().template("/views/BoxHandler/editBoxForm.ftl.html")
                              .render("mbFrmDat", mailBoxFormData).render("domain", xcmConfiguration.DOMAIN_LIST);
            }
            else
            { // the MBox does not belong to this user
                return Results.redirect(context.getContextPath() + "/mail");
            }
        }
    }

    /**
     * Generates the Mailbox-Overview-Page of a {@link User}. <br/>
     * GET /mail
     * 
     * @param context
     *            the Context of this Request
     * @return the Mailbox-Overview-Page
     */

    public Result showBoxOverview(Context context)
    {
        Result result = Results.html();
        User user = context.getAttribute("user", User.class);
        // set a default number or the number which the user had chosen
        HelperUtils.parseEntryValue(context, xcmConfiguration.APP_DEFAULT_ENTRYNO);
        // get the default number of entries per page
        int entries = Integer.parseInt(context.getSessionCookie().get("no"));

        String searchString = context.getParameter("s", "");
        PageList<MBox> plist = new PageList<MBox>(MBox.findBoxLike(searchString, user.getId()), entries);

        if (!searchString.isEmpty())
        {
            result.render("searchValue", searchString);
        }

        result.render("mboxes", plist);

        long nowPlusOneHour = DateTime.now().plusHours(1).getMillis();
        result.render("datetime", HelperUtils.parseStringTs(nowPlusOneHour));
        return result;
    }

    /**
     * Sets the Box valid/invalid <br/>
     * POST /mail/expire/{id}
     * 
     * @param boxId
     *            the ID of the Mailbox
     * @param context
     *            the Context of this Request
     * @return the rendered Mailbox-Overview-Page
     */

    public Result expireBoxProcess(@PathParam("id") Long boxId, Context context)
    {
        MBox mailBox = MBox.getById(boxId);
        User user = context.getAttribute("user", User.class);

        if (mailBox.belongsTo(user.getId()))
        {// check, whether the mailbox belongs to the current user
            if ((mailBox.getTs_Active() != 0) && (mailBox.getTs_Active() < DateTime.now().getMillis()))
            { // if the validity period is over, return the Edit page and give the user a response why he gets there

                context.getFlashCookie().put("info", "expireEmail_Flash_Expired");
                return Results.redirect(context.getContextPath() + "/mail/edit/" + boxId);
            }
            else
            { // otherwise just set the new status
                mailBox.enable();
            }
        }
        return Results.redirect(context.getContextPath() + "/mail");
    }

    /**
     * Sets the Values of the Counters for the Box, given by their ID, to zero <br/>
     * POST /mail/reset/{id}
     * 
     * @param boxId
     *            the ID of the Mailbox
     * @param context
     *            the Context of this Request
     * @return the Mailbox-Overview-Page
     */
    public Result resetBoxCounterProcess(@PathParam("id") Long boxId, Context context)
    {
        MBox mailBox = MBox.getById(boxId);
        User user = context.getAttribute("user", User.class);

        // check if the mailbox belongs to the current user
        if (mailBox.belongsTo(user.getId()))
        {
            mailBox.resetForwards();
            mailBox.resetSuppressions();
            mailBox.update();
        }
        return Results.redirect(context.getContextPath() + "/mail");
    }

    /**
     * Processes the given action on the given Mail-Addresses<br/>
     * GET /mail/bulkChange
     * 
     * @param action
     *            the action to do (may be 'reset','delete', 'change' or 'enable'
     * @param boxIds
     *            the IDs of the mailboxes/mailaddresses to modify, separated by a colon
     * @param duration
     *            the new duration for the mailboxes in action 'change', must be in format "dd.MM.yyyy hh:mm"
     * @param context
     *            the context of this request
     * @return to the mailbox-overview-page
     */
    public Result bulkChangeBoxes(@Param("action") String action, @Param("ids") String boxIds,
                                  @Param("duration") String duration, Context context)
    {
        Result result = Results.redirect(context.getContextPath() + "/mail");
        User user = context.getAttribute("user", User.class);

        if (!StringUtils.isBlank(action) && !StringUtils.isBlank(boxIds))
        { 
            if (PATTERN_CS_BOXIDS.matcher(boxIds).matches())
            {// the list of boxIds have to be in the form of comma-separated-ids
                String[] splittedIds = boxIds.split("\\,");

                Long boxId;
                if (splittedIds.length > 0)
                { // the length of the IDs are at least one
                    switch (Actions.valueOf(action))
                    {
                        case reset: // reset the list of boxes, we'll abort if there's a box with an id that does not
                                    // belong to the user
                            for (String boxIdString : splittedIds)
                            {
                                boxId = Long.valueOf(boxIdString);
                                MBox mailBox = MBox.getById(boxId);
                                if (mailBox.belongsTo(user.getId()))
                                { // box belongs to the user
                                    mailBox.resetForwards();
                                    mailBox.resetSuppressions();
                                    mailBox.update();
                                }
                                else
                                {
                                    context.getFlashCookie().error("bulkChange_Flash_BoxToUser");
                                }
                            }

                            return result;

                        case delete:
                            // delete the list of boxes, we'll abort if there's a box with an id, that does not belong
                            // to this user
                            for (String boxIdString : splittedIds)
                            {
                                boxId = Long.valueOf(boxIdString);
                                if (MBox.boxToUser(boxId, user.getId()))
                                { // box belongs to the user
                                    MBox.delete(boxId);
                                }
                                else
                                {
                                    context.getFlashCookie().error("bulkChange_Flash_BoxToUser");
                                }
                            }
                            return result;

                        case change:
                            // change the duration of the boxes, we'll abort if there's a box with an id, that does not
                            // belong to this user

                            Long ts = HelperUtils.parseTimeString(duration);
                            if (ts == -1L)
                            { // show an error-page if the timestamp is faulty
                                context.getFlashCookie().error("mailbox_Wrong_Timestamp");
                                return result;
                            }
                            if ((ts != 0) && (ts < DateTime.now().getMillis()))
                            { // the Timestamp lays in the past
                                context.getFlashCookie().error("createEmail_Past_Timestamp");
                                return result;
                            }

                            for (String boxIdString : splittedIds)
                            {
                                boxId = Long.valueOf(boxIdString);
                                MBox mailBox = MBox.getById(boxId);
                                if (mailBox.belongsTo(user.getId()))
                                { // box belongs to the user
                                    mailBox.setTs_Active(ts);
                                    mailBox.setExpired(false);
                                    mailBox.update();
                                }
                                else
                                { // set an error-message
                                    context.getFlashCookie().error("bulkChange_Flash_BoxToUser");
                                }
                            }
                            return result;

                        case enable:
                            // enable or disable the boxes
                            // all active boxes will then be inactive and vice versa
                            for (String boxIdString : splittedIds)
                            {
                                boxId = Long.valueOf(boxIdString);
                                MBox mailBox = MBox.getById(boxId);
                                if (mailBox.belongsTo(user.getId()))
                                { // box belongs to the user
                                    if ((mailBox.getTs_Active() != 0)
                                        && (mailBox.getTs_Active() < DateTime.now().getMillis()))
                                    { // if the validity period is over, return the Edit page
                                        context.getFlashCookie().error("mailbox_Flash_NotEnabled");
                                    }
                                    else
                                    { // otherwise just set the new status
                                        mailBox.enable();
                                    }
                                }
                                else
                                { // box does not belong to the user
                                    context.getFlashCookie().error("bulkChange_Flash_BoxToUser");
                                }
                            }
                            return result;

                        default:
                            // we got an action that is not defined
                            // we're ignoring it and simply redirect to the overview-page
                            return result;
                    }// end switch
                }
                else
                { // the IDs have a wrong separator
                    return result;
                }
            }
            else
            { // the IDs are not in the expected pattern
                return result;
            }
        }
        else
        { // the action or IDs-parameter is empty or null
            return result;
        }
    }

    private enum Actions
    {
        reset, delete, change, enable
    }

    /**
     * Handles JSON-Requests for the search <br/>
     * GET /mail/search
     * 
     * @param context
     *            the Context of this Request
     * @return a JSON-Array with the boxes
     */
    public Result jsonBoxSearch(Context context)
    {
        User user = context.getAttribute("user", User.class);
        List<MBox> boxList;
        Result result = Results.json();
        String searchString = context.getParameter("s", "");

        boxList = (searchString.equals("")) ? new ArrayList<MBox>() : MBox.findBoxLike(searchString, user.getId());

        // GSON can't handle with cyclic references (the 1:m relation between user and MBox will end up in a cycle)
        // so we need to transform the data which does not contain the reference
        List<MailBoxFormData> mbdlist = new ArrayList<MailBoxFormData>();
        for (MBox mb : boxList)
        {
            MailBoxFormData mbd = MailBoxFormData.prepopulate(mb);
            mbdlist.add(mbd);
        }
        return result.json().render(mbdlist);
    }

    /**
     * returns a text-page with all addresses of a user<br/>
     * GET /mail/mymaillist.txt
     * 
     * @param context
     *            the Context of this Request
     * @return a text page with all addresses of a user
     */
    public Result showMailsAsTextList(Context context)
    {
        User user = context.getAttribute("user", User.class);
        return Results.contentType("text/plain").render(MBox.getMailsForTxt(user.getId()));
    }

    /**
     * returns a text-page with all active addresses of a user<br/>
     * GET /mail/myactivemaillist.txt
     * 
     * @param context
     *            the Context of this Request
     * @return a text page with all active addresses of a user
     */

    public Result showActiveMailsAsTextList(Context context)
    {
        User user = context.getAttribute("user", User.class);
        return Results.contentType("text/plain").render(MBox.getActiveMailsForTxt(user.getId()));
    }
}
