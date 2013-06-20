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

import ninja.Context;
import ninja.FilterWith;
import ninja.Results;
import etc.HelperUtils;
import filters.SecureFilter;
import org.joda.time.DateTime;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import conf.XCMailrConf;
import models.MBox;
import models.MailBoxFormData;
import models.PageList;
import models.User;
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
     * @param mbdat
     *            the Data of the Mailbox-Add-Form
     * @param validation
     *            Form validation
     * @return the Add-Box-Form (on Error) or the Box-Overview
     */
    public Result addBoxProcess(Context context, @JSR303Validation MailBoxFormData mailboxFormData,
                                Validation validation)
    {
        Result result = Results.html().template("/views/Application/index.ftl.html");
        result.render("domain", xcmConfiguration.DOMAIN_LIST);

        if (validation.hasViolations())
        { // not all fields were filled (correctly)
            context.getFlashCookie().error("flash_FormError");
            if ((mailboxFormData.getAddress() == null) || (mailboxFormData.getDomain() == null)
                || (mailboxFormData.getDatetime() == null))
            {
                return result.redirect("/mail/add");
            }

            result.render("mbFrmDat", mailboxFormData);
            return result.template("/views/BoxHandler/addBoxForm.ftl.html");
        }
        else
        {
            // check for rfc 5321 compliant length of email (64 chars for local and 254 in total)
            String completeAddress = mailboxFormData.getAddress() + "@" + mailboxFormData.getDomain();
            if (completeAddress.length() >= 255)
            {
                context.getFlashCookie().error("flash_MailTooLong");

                result.render("mbFrmDat", mailboxFormData);
                return result.template("/views/BoxHandler/addBoxForm.ftl.html");
            }

            // checks whether the Box already exists
            if (!MBox.mailExists(mailboxFormData.getAddress(), mailboxFormData.getDomain()))
            {
                String mailBoxName = mailboxFormData.getAddress().toLowerCase();
                // set the data of the box
                String[] domains = xcmConfiguration.DOMAIN_LIST;
                if (!Arrays.asList(domains).contains(mailboxFormData.getDomain()))
                { // the new domain-name does not exist in the application.conf
                  // stop the process and return to the mailbox-overview page
                    return result.redirect("/mail");
                }
                Long ts = HelperUtils.parseTimeString(mailboxFormData.getDatetime());
                if (ts == -1L)
                { // show an error-page if the timestamp is faulty
                    context.getFlashCookie().error("msg_WrongF");
                    result.render("mbFrmDat", mailboxFormData);
                    return result.template("/views/BoxHandler/addBoxForm.ftl.html");
                }
                if ((ts != 0) && (ts < DateTime.now().getMillis()))
                { // the Timestamp lays in the past
                    context.getFlashCookie().error("createMail_Past_Timestamp");
                    result.render("mbFrmDat", mailboxFormData);
                    return result.template("/views/BoxHandler/addBoxForm.ftl.html");
                }

                // create the MBox
                User user = context.getAttribute("user", User.class);
                MBox mailBox = new MBox(mailBoxName, mailboxFormData.getDomain(), ts, false, user);

                // creates the Box in the DB
                mailBox.save();

                return result.redirect(context.getContextPath() + "/mail");
            }
            else
            {
                // the mailbox already exists
                context.getFlashCookie().error("flash_MailExists");

                result.render("mbFrmDat", mailboxFormData);
                return result.template("/views/BoxHandler/addBoxForm.ftl.html");
            }
        }
    }

    /**
     * Deletes a Box from the DB POST /mail/delete/{id}
     * 
     * @param boxid
     *            the ID of the Mailbox
     * @param context
     *            the Context of this Request
     * @return the Mailbox-Overview-Page
     */
    public Result deleteBoxProcess(@PathParam("id") Long boxId, Context context)
    {
        Result result = Results.html().template("/views/Application/index.ftl.html");
        User user = context.getAttribute("user", User.class);
        if (MBox.boxToUser(boxId, user.getId()))
        {
            // deletes the box from DB
            MBox.delete(boxId);
        }
        return result.redirect(context.getContextPath() + "/mail");
    }

    /**
     * Edits a Mailbox <br/>
     * POST /mail/edit/{id}
     * 
     * @param context
     *            the Context of this Request
     * @param boxId
     *            the ID of a Mailbox
     * @param mbdat
     *            the Data of the Mailbox-Edit-Form
     * @param validation
     *            Form validation
     * @return Mailbox-Overview-Page or the Mailbox-Form with an Error- or Success-Message
     */
    public Result editBoxProcess(Context context, @PathParam("id") Long boxId,
                                 @JSR303Validation MailBoxFormData mailboxFormData, Validation validation)
    {
        Result result = Results.html().template("/views/Application/index.ftl.html");
        result.render("domain", xcmConfiguration.DOMAIN_LIST);
        if (validation.hasViolations())
        { // not all fields were filled
            context.getFlashCookie().error("flash_FormError");

            if ((mailboxFormData.getAddress() == null) || (mailboxFormData.getDomain() == null)
                || (mailboxFormData.getDatetime() == null))
            {
                return result.redirect(context.getContextPath() + "/mail/edit/" + boxId.toString());
            }
            result.render("mbFrmDat", mailboxFormData);
            return result.template("/views/BoxHandler/editBoxForm.ftl.html");
        }
        else
        { // the form was filled correctly
          // check for rfc 5322 compliant length of email (64 chars for local and 254 in total)
            String completeAddress = mailboxFormData.getAddress() + "@" + mailboxFormData.getDomain();
            if (completeAddress.length() >= 255)
            {
                context.getFlashCookie().error("flash_MailTooLong");

                result.render("mbFrmDat", mailboxFormData);
                return result.template("/views/BoxHandler/addBoxForm.ftl.html");
            }
            // we got the boxID with the POST-Request
            MBox mailBox = MBox.getById(boxId);
            if (mailBox != null)
            { // the box with the given id exists
                User usr = context.getAttribute("user", User.class);

                if (mailBox.belongsTo(usr.getId()))
                { // the current user is the owner of the mailbox
                    boolean changes = false;
                    String newLocalPartName = mailboxFormData.getAddress().toLowerCase();
                    String newDomainPartName = mailboxFormData.getDomain().toLowerCase();
                    if (MBox.mailChanged(newLocalPartName, newDomainPartName, boxId))
                    { // this is only true when the address changed and the new address does not exist

                        String[] domains = xcmConfiguration.DOMAIN_LIST;
                        // assume that the POST-Request was modified and the domainname does not exist in our app
                        if (!Arrays.asList(domains).contains(mailboxFormData.getDomain()))
                        {
                            // the new domainname does not exist in the application.conf
                            // stop the process and return to the mailbox-overview page
                            return result.redirect(context.getContextPath() + "/mail");
                        }
                        mailBox.setAddress(newLocalPartName);
                        mailBox.setDomain(newDomainPartName);
                        changes = true;
                    }
                    Long ts = HelperUtils.parseTimeString(mailboxFormData.getDatetime());
                    if (ts == -1)
                    { // a faulty timestamp was given -> return an errorpage
                        context.getFlashCookie().error("msg_WrongF");
                        return result.redirect(context.getContextPath() + "/mail/edit/" + boxId.toString());
                    }
                    if ((ts != 0) && (ts < DateTime.now().getMillis()))
                    { // the Timestamp lays in the past
                        context.getFlashCookie().error("editEmail_Past_Timestamp");
                        return result.redirect(context.getContextPath() + "/mail/edit/" + boxId.toString());
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
                    }
                }
            }
        }
        // the current user is not the owner of the mailbox,
        // the given box-id does not exist,
        // or the editing-process was successful
        return result.redirect(context.getContextPath() + "/mail");
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

                if (mailBox.getTs_Active() <= DateTime.now().getMillis())
                {
                    // set a new activity-timestamp if the current one is in the past
                    DateTime dateTime = new DateTime().plusHours(1);
                    mailBox.setTs_Active(dateTime.getMillis());
                }
                MailBoxFormData mailBoxFormData = MailBoxFormData.prepopulate(mailBox);
                return Results.html().render("mbFrmDat", mailBoxFormData)
                              .render("domain", xcmConfiguration.DOMAIN_LIST);
            }
            else
            { // the MBox does not belong to this user
                return Results.redirect(context.getContextPath() + "/mail");
            }
        }
    }

    /**
     * Generates the Mailbox-Overview-Page of a {@link User}.
     * 
     * @param context
     *            the Context of this Request
     * @return the Mailbox-Overview-Page
     */

    public Result showBoxOverview(Context context)
    {
        User user = context.getAttribute("user", User.class);
        // set a default number or the number which the user had chosen
        HelperUtils.parseEntryValue(context, xcmConfiguration.APP_DEFAULT_ENTRYNO);
        // get the default number of entries per page
        int entries = Integer.parseInt(context.getSessionCookie().get("no"));

        PageList<MBox> plist;

        Result result = Results.html();
        String searchString = context.getParameter("s", "");
        if (searchString.equals(""))
        { // if theres no parameter, simply render all boxes
            plist = new PageList<MBox>(MBox.allUser(user.getId()), entries);
        }
        else
        { // theres a search parameter with input, get the related boxes
            plist = new PageList<MBox>(MBox.findBoxLike(searchString, user.getId()), entries);
        }

        return result.render("mboxes", plist);
    }

    /**
     * Sets the Box valid/invalid POST /mail/expire/{id}
     * 
     * @param boxId
     *            the ID of the Mailbox
     * @param context
     *            the Context of this Request
     * @return the rendered Mailbox-Overview-Page
     */

    public Result expireBoxProcess(@PathParam("id") Long boxId, Context context)
    {
        Result result = Results.html().template("/views/Application/index.ftl.html");
        MBox mailBox = MBox.getById(boxId);
        User user = context.getAttribute("user", User.class);

        if (mailBox.belongsTo(user.getId()))
        {// check if the mailbox belongs to the current user
            if (!(mailBox.getTs_Active() == 0) && (mailBox.getTs_Active() < DateTime.now().getMillis()))
            { // if the validity period is over, return the Edit page
                return result.redirect(context.getContextPath() + "/mail/edit/" + boxId);
            }
            else
            { // otherwise just set the new status
                mailBox.enable();
            }
        }
        return result.redirect(context.getContextPath() + "/mail");
    }

    /**
     * Sets the Values of the Counters for the Box, given by their ID, to zero POST /mail/reset/{id}
     * 
     * @param boxId
     *            the ID of the Mailbox
     * @param context
     *            the Context of this Request
     * @return the Mailbox-Overview-Page
     */
    public Result resetBoxCounterProcess(@PathParam("id") Long boxId, Context context)
    {
        Result result = Results.html().template("/views/Application/index.ftl.html");
        MBox mailBox = MBox.getById(boxId);
        User user = context.getAttribute("user", User.class);
        System.out.println("resette box mit der id: " + boxId);
        // check if the mailbox belongs to the current user
        if (mailBox.belongsTo(user.getId()))
        {
            mailBox.resetForwards();
            mailBox.resetSuppressions();
            mailBox.update();
        }
        return result.redirect(context.getContextPath() + "/mail");
    }

    /**
     * Handles JSON-Requests from the search
     * 
     * @param context
     * @return a JSON-Array with the boxes
     */
    public Result jsonBoxSearch(Context context)
    {
        User user = context.getAttribute("user", User.class);
        List<MBox> boxList;
        Result result = Results.html();
        String searchString = context.getParameter("s", "");
        if (searchString.equals(""))
        {
            boxList = new ArrayList<MBox>();
        }
        else
        {
            boxList = MBox.findBoxLike(searchString, user.getId());
        }

        List<MailBoxFormData> mbdlist = new ArrayList<MailBoxFormData>();
        for (MBox mb : boxList)
        {
            MailBoxFormData mbd = MailBoxFormData.prepopulate(mb);
            mbdlist.add(mbd);

        }

        return result.json().render(mbdlist);
    }

}
