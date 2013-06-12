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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
import models.MbFrmDat;
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
    XCMailrConf xcmConf;

    /**
     * Shows the "new Mail-Forward"-Page <br/>
     * GET /mail/add
     * 
     * @param context
     *            the Context of this Request
     * @return a prepopulated "Add-Box"-Form
     */
    public Result showAddBox(Context context)
    {
        Map<String, Object> map = xcmConf.getDomListAsMap();
        MbFrmDat mbdat = new MbFrmDat();
        // set the value of the random-name to 7
        // use the lowercase, we handle the address as case-insensitive
        String name = HelperUtils.getRndString(7).toLowerCase();
        mbdat.setAddress(name);

        // check that the generated mailname-proposal does not exist
        String[] domains = xcmConf.DM_LIST;
        if (domains.length > 0)
        {// prevent OutOfBoundException
            while (MBox.mailExists(name, domains[0]))
            {
                name = HelperUtils.getRndString(7).toLowerCase();
            }
        }

        // set a default entry for the validity-period
        // per default now+1h
        long nowPlusOneHour = DateTime.now().plusHours(1).getMillis();
        mbdat.setDatetime(HelperUtils.parseStringTs(nowPlusOneHour));
        mbdat.setDomain(domains[0]);
        map.put("mbFrmDat", mbdat);

        return Results.html().render(map);
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
    public Result addBox(Context context, @JSR303Validation MbFrmDat mbdat, Validation validation)
    {
        Result result = Results.html().template("/views/Application/index.ftl.html");
        Map<String, Object> map = xcmConf.getDomListAsMap();

        if (validation.hasViolations())
        { // not all fields were filled (correctly)

            context.getFlashCookie().error("i18nMsg_FormErr");
            if ((mbdat.getAddress() == null) || (mbdat.getDomain() == null) || (mbdat.getDatetime() == null))
            {
                return result.redirect("/mail/add");
            }
            map.put("mbFrmDat", mbdat);
            return result.template("/views/BoxHandler/showAddBox.ftl.html").render(map);
        }
        else
        {
            // checks whether the Box already exists
            if (!MBox.mailExists(mbdat.getAddress(), mbdat.getDomain()))
            {
                String mbName = mbdat.getAddress().toLowerCase();
                // set the data of the box
                String[] dom = xcmConf.DM_LIST;
                if (!Arrays.asList(dom).contains(mbdat.getDomain()))
                { // the new domainname does not exist in the application.conf
                  // stop the process and return to the mailbox-overview page
                    return result.redirect("/mail");
                }
                Long ts = HelperUtils.parseTimeString(mbdat.getDatetime());
                if (ts == -1L)
                { // show an error-page if the timestamp is faulty
                    context.getFlashCookie().error("i18nMsg_WrongF");
                    map.put("mbFrmDat", mbdat);

                    return result.template("/views/BoxHandler/showAddBox.ftl.html").render(map);
                }
                if ((ts != 0) && (ts < DateTime.now().getMillis()))
                { // the Timestamp lays in the past
                    context.getFlashCookie().error("i18nCreateMail_Past_Timestamp");
                    return result.template("/views/BoxHandler/showAddBox.ftl.html").render(map);
                }

                // create the MBox
                User usr = context.getAttribute("user", User.class);
                MBox mb = new MBox(mbName, mbdat.getDomain(), ts, false, usr);

                // creates the Box in the DB
                mb.save();

                return result.redirect("/mail");
            }
            else
            {
                // the mailbox already exists
                context.getFlashCookie().error("i18nMsg_MailEx");
                map.put("mbFrmDat", mbdat);

                return result.template("/views/BoxHandler/showAddBox.ftl.html").render(map);
            }
        }
    }

    /**
     * Deletes a Box from the DB
     * 
     * @param boxid
     *            the ID of the Mailbox
     * @param context
     *            the Context of this Request
     * @return the Mailbox-Overview-Page
     */
    public Result deleteBox(@PathParam("id") Long boxid, Context context)
    {
        Result result = Results.html().template("/views/BoxHandler/showBoxes.ftl.html");
        User usr = context.getAttribute("user", User.class);
        if (MBox.boxToUser(boxid, usr.getId()))
        {
            // deletes the box from DB
            MBox.delete(boxid);
        }
        return result.redirect("/mail");
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
    public Result editBox(Context context, @PathParam("id") Long boxId, @JSR303Validation MbFrmDat mbdat,
                          Validation validation)
    {
        Result result = Results.html().template("/views/BoxHandler/showBoxes.ftl.html");
        if (validation.hasViolations())
        { // not all fields were filled
            context.getFlashCookie().error("i18nMsg_FormErr");
            Map<String, Object> map = xcmConf.getDomListAsMap();
            if ((mbdat.getAddress() == null) || (mbdat.getDomain() == null) || (mbdat.getDatetime() == null))
            {
                return result.redirect("/mail/edit/" + boxId.toString());
            }
            map.put("mbFrmDat", mbdat);
            return result.template("/views/BoxHandler/showEditBox.ftl.html").render(mbdat);
        }
        else
        { // the form was filled correctly

            // we got the boxID with the POST-Request
            MBox mb = MBox.getById(boxId);
            if (mb != null)
            { // the box with the given id exists
                User usr = context.getAttribute("user", User.class);

                if (mb.belongsTo(usr.getId()))
                { // the current user is the owner of the mailbox
                    boolean changes = false;
                    String newLName = mbdat.getAddress().toLowerCase();
                    String newDName = mbdat.getDomain().toLowerCase();
                    if (MBox.mailChanged(newLName, newDName, boxId))
                    { // this is only true when the address changed and the new address does not exist

                        String[] dom = xcmConf.DM_LIST;
                        // assume that the POST-Request was modified and the domainname does not exist in our app
                        if (!Arrays.asList(dom).contains(mbdat.getDomain()))
                        {
                            // the new domainname does not exist in the application.conf
                            // stop the process and return to the mailbox-overview page
                            return Results.redirect("/mail");
                        }
                        mb.setAddress(newLName);
                        mb.setDomain(newDName);
                        changes = true;
                    }
                    Long ts = HelperUtils.parseTimeString(mbdat.getDatetime());
                    if (ts == -1)
                    { // a faulty timestamp was given -> return an errorpage
                        context.getFlashCookie().error("i18nMsg_WrongF");
                        return result.template("/views/BoxHandler/showEditBox.ftl.html").redirect("/mail/edit/" + boxId.toString());
                    }
                    if ((ts != 0) && (ts < DateTime.now().getMillis()))
                    { // the Timestamp lays in the past
                        context.getFlashCookie().error("i18nEditEmail_Past_Timestamp");
                        return result.template("/views/BoxHandler/showEditBox.ftl.html").redirect("/mail/edit/" + boxId.toString());
                    }

                    if (!(mb.getTs_Active() == ts))
                    { // check if the MBox-TS is unequal to the given TS in the form
                        mb.setTs_Active(ts);
                        changes = true;
                    }

                    // Updates the Box if changes were made
                    if (changes)
                    {
                        mb.setExpired(false);
                        mb.update();
                    }
                }
            }
        }
        // the current user is not the owner of the mailbox,
        // the given box-id does not exist,
        // or the editing-process was successful
        return result.redirect("/mail");
    }

    /**
     * Shows the Edit-Form for the Box with the given boxId. <br/>
     * GET /mail/edit/:boxid
     * 
     * @param context
     *            the Context of this Request
     * @param boxId
     *            ID of the Box
     * @return the Mailbox-Edit-Form with prepopulated values
     */
    public Result showEditBox(Context context, @PathParam("id") Long boxId)
    {
        MBox mb = MBox.getById(boxId);

        if (mb == null)
        { // there's no box with that id
            return Results.redirect("/mail");
        }
        else
        { // the box exists, go on!
            User usr = context.getAttribute("user", User.class);

            if (mb.belongsTo(usr.getId()))
            { // the MBox belongs to this user
                MbFrmDat mbdat = new MbFrmDat();
                mbdat.setBoxId(boxId);
                mbdat.setAddress(mb.getAddress());
                mbdat.setDomain(mb.getDomain());
                mbdat.setDatetime(mb.getTSAsStringWithNull());

                Map<String, Object> map = xcmConf.getDomListAsMap();
                map.put("mbFrmDat", mbdat);

                return Results.html().render(map);
            }
            else
            { // the MBox does not belong to this user
                return Results.redirect("/mail");
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

    public Result showBoxes(Context context)
    {
        User usr = context.getAttribute("user", User.class);

        // set a default number or the number which the user had chosen
        HelperUtils.parseEntryValue(context, xcmConf.APP_DEFAULT_ENTRYNO);
        // get the default number of entries per page
        int entries = Integer.parseInt(context.getSessionCookie().get("no"));

        // generate the paged-list to get pagination in the pattern
        PageList<MBox> plist = new PageList<MBox>(MBox.allUser(usr.getId()), entries);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("mboxes", plist);

        return Results.html().render(map);
    }

    /**
     * Sets the Box valid/invalid
     * 
     * @param id
     *            the ID of the Mailbox
     * @param context
     *            the Context of this Request
     * @return the rendered Mailbox-Overview-Page
     */

    public Result expireBox(@PathParam("id") Long id, Context context)
    {
        Result result = Results.html().template("/views/BoxHandler/showBoxes.ftl.html");
        MBox mb = MBox.getById(id);
        User usr = context.getAttribute("user", User.class);

        if (mb.belongsTo(usr.getId()))
        {// check if the mailbox belongs to the current user
            if (!(mb.getTs_Active() == 0) && (mb.getTs_Active() < DateTime.now().getMillis()))
            { // if the validity period is over, return the Edit page
                return result.redirect("/mail/edit/" + id);
            }
            else
            { // otherwise just set the new status
                mb.enable();
            }
        }
        return result.redirect("/mail");
    }

    /**
     * Sets the Values of the Counters for the Box, given by their ID, to zero
     * 
     * @param id
     *            the ID of the Mailbox
     * @param context
     *            the Context of this Request
     * @return the Mailbox-Overview-Page
     */
    public Result resetBoxCounters(@PathParam("id") Long id, Context context)
    {
        Result result = Results.html().template("/views/BoxHandler/showBoxes.ftl.html");
        MBox mb = MBox.getById(id);
        User usr = context.getAttribute("user", User.class);

        // check if the mailbox belongs to the current user
        if (mb.belongsTo(usr.getId()))
        {
            mb.resetForwards();
            mb.resetSuppressions();
            mb.update();
        }
        return result.redirect("/mail");
    }
}
