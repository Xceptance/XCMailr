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
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import ninja.Context;
import ninja.FilterWith;
import ninja.Results;
import etc.HelperUtils;
import filters.SecureFilter;
import filters.JsonSecureFilter;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import com.google.common.base.Optional;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import conf.XCMailrConf;
import models.JsonMBox;
import models.MBox;
import models.User;
import ninja.i18n.Messages;
import ninja.params.PathParam;
import ninja.validation.JSR303Validation;
import ninja.validation.Validation;
import ninja.Result;

/**
 * Handles all actions for the (virtual) Mailboxes like add, delete and edit box
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */

@Singleton
public class BoxHandler
{
    @Inject
    XCMailrConf xcmConfiguration;

    @Inject
    Messages messages;

    private static final Pattern PATTERN_CS_BOXIDS = Pattern.compile("[(\\d+)][(\\,)(\\d+)]*");

    /**
     * Opens the empty delete-box-dialog (just rendering the template)<br/>
     * GET /mail/deleteBoxDialog.html
     * 
     * @return the Delete-Box-Dialog
     */
    @FilterWith(SecureFilter.class)
    public Result deleteBoxDialog()
    {
        return Results.html();
    }

    /**
     * Opens the empty add- and edit-box-dialog (just rendering the template) <br/>
     * GET /mail/editBoxDialog.html
     * 
     * @return the Add- and Edit-Box-Dialog
     */
    @FilterWith(SecureFilter.class)
    public Result editBoxDialog()
    {
        // set a default entry for the validity-period
        // per default now+1h
        long nowPlusOneHour = DateTime.now().plusHours(1).getMillis();
        return Results.html().render("timeStamp", HelperUtils.parseStringTs(nowPlusOneHour));
    }

    /**
     * opens the empty new-Date-dialog (just rendering the template)<br/>
     * GET /mail/newDateDialog.html
     * 
     * @return the Add- and Edit-Box-Dialog
     */
    @FilterWith(SecureFilter.class)
    public Result newDateDialog()
    {
        long tsNew = DateTime.now().plusHours(1).getMillis();
        return Results.html().render("timeStampNew", HelperUtils.parseStringTs(tsNew));
    }

    /**
     * Generates the 'Angularized' Mailbox-Overview-Page of a {@link User}. <br/>
     * GET /mail
     * 
     * @param context
     *            the Context of this Request
     * @return the Mailbox-Overview-Page
     */
    @FilterWith(SecureFilter.class)
    public Result showAngularBoxOverview(Context context)
    {
        Result result = Results.html();
        // TODO do we need to handle the entryno?
        // set a default number or the number which the user had chosen
        // HelperUtils.parseEntryValue(context, xcmConfiguration.APP_DEFAULT_ENTRYNO);
        // get the default number of entries per page
        // int entries = Integer.parseInt(context.getSessionCookie().get("no"));
        // add a mboxes-object (boolean) for the header-menu
        return result.render("ts_now", DateTime.now().getMillis()).render("mboxes", true);
    }

    /**
     * Shows the "new Mail-Forward"-Page <br/>
     * GET /mail/addBoxData
     * 
     * @param context
     *            the Context of this Request
     * @return a prepopulated "Add-Box"-Form
     */
    @FilterWith(JsonSecureFilter.class)
    public Result addBoxJsonData(Context context)
    {
        JsonMBox jsonMailboxData = new JsonMBox();
        // set the value of the random-name to 7
        // use the lowercase, we handle the address as case-insensitive
        String randomName = HelperUtils.getRandomString(7).toLowerCase();
        jsonMailboxData.setAddress(randomName);

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
        jsonMailboxData.setDatetime(HelperUtils.parseStringTs(nowPlusOneHour));
        jsonMailboxData.setDomain(domains[0]);

        return Results.json().render("currentBox", jsonMailboxData);
    }

    /**
     * Adds a Mailbox to the {@link User}-Account <br/>
     * POST /mail/addJson
     * 
     * @param context
     *            the Context of this Request
     * @param addBoxDialogData
     *            the Data of the Mailbox-Add-Form
     * @param validation
     *            Form validation
     * @return the Add-Box-Form (on Error) or the Box-Overview
     */
    @FilterWith(JsonSecureFilter.class)
    public Result addBoxJsonProcess(Context context, @JSR303Validation JsonMBox addBoxDialogData, Validation validation)
    {
        String errorMessage;
        Result result = Results.json();
        result.render("domain", xcmConfiguration.DOMAIN_LIST);

        if (validation.hasViolations() || addBoxDialogData == null)
        { // not all fields were filled (correctly)
            errorMessage = messages.get("flash_FormError", context, Optional.of(result)).get();
            result.render("currentBox", addBoxDialogData);
            return result.render("success", false).render("statusmsg", errorMessage);
        }
        else
        {
            // check for rfc 5321 compliant length of email (64 chars for local and 254 in total)
            String completeAddress = addBoxDialogData.getAddress() + "@" + addBoxDialogData.getDomain();

            if (addBoxDialogData.getAddress().length() > 64 || completeAddress.length() > 254)
            {
                errorMessage = messages.get("createEmail_Flash_MailTooLong", context, Optional.of(result)).get();
                result.render("currentBox", addBoxDialogData);
                return result.render("success", false).render("statusmsg", errorMessage);

            }
            // checks whether the email address already exists
            if (MBox.mailExists(addBoxDialogData.getAddress(), addBoxDialogData.getDomain()))
            {
                // the mailbox already exists
                errorMessage = messages.get("flash_MailExists", context, Optional.of(result)).get();
                result.render("currentBox", addBoxDialogData);
                return result.render("success", false).render("statusmsg", errorMessage);

            }
            else
            {
                String mailBoxName = addBoxDialogData.getAddress().toLowerCase();
                // set the data of the box
                String[] domains = xcmConfiguration.DOMAIN_LIST;
                if (!Arrays.asList(domains).contains(addBoxDialogData.getDomain()))
                { // the new domain-name does not exist in the application.conf
                  // stop the process and return to the mailbox-overview page
                    errorMessage = messages.get("editEmailDialog_JSValidation_MailInvalid", context,
                                                Optional.of(result)).get();
                    result.render("currentBox", addBoxDialogData);
                    return result.render("success", false).render("statusmsg", errorMessage);
                }
                Long ts = HelperUtils.parseTimeString(addBoxDialogData.getDatetime());
                if (ts == -1L)
                { // show an error-page if the timestamp is faulty
                    errorMessage = messages.get("flash_FormError", context, Optional.of(result)).get();
                    result.render("currentBox", addBoxDialogData);
                    return result.render("success", false).render("statusmsg", errorMessage);
                }
                if ((ts != 0) && (ts < DateTime.now().getMillis()))
                { // the Timestamp lays in the past
                    errorMessage = messages.get("createEmail_Past_Timestamp", context, Optional.of(result)).get();
                    result.render("currentBox", addBoxDialogData);
                    return result.render("success", false).render("statusmsg", errorMessage);
                }

                // create the MBox
                User user = context.getAttribute("user", User.class);
                MBox mailBox = new MBox(mailBoxName, addBoxDialogData.getDomain(), ts, false, user);

                // creates the Box in the DB
                mailBox.save();
                addBoxDialogData.prepopulateJS(mailBox);
                errorMessage = messages.get("flash_DataChangeSuccess", context, Optional.of(result)).get();
                result.render("currentBox", addBoxDialogData);
                return result.render("success", true).render("statusmsg", errorMessage);
            }
        }
    }

    /**
     * Deletes the boxes with the given IDs, given as a JSON-Object in the form <br/>
     * {id : boolean, id : boolean, id : boolean, ...} <br/>
     * POST /mail/bulkDelete
     * 
     * @param boxIds
     *            the Box-IDs as JSON-Object, with a boolean-value which indicates whether it will be deleted or not
     * @param context
     *            the context of this request
     * @return a json-object with a "success" key and a boolean value
     */
    @FilterWith(JsonSecureFilter.class)
    public Result bulkDeleteBoxes(JsonObject boxIds, Context context)
    {
        Result result = Results.json();
        if (boxIds == null)
        {
            return result.render("success", false);
        }
        String ids = getJsonArrayAsString(boxIds);
        User user = context.getAttribute("user", User.class);

        if (!StringUtils.isBlank(ids) && PATTERN_CS_BOXIDS.matcher(ids).matches())
        { // the list of boxIds have to be in the form of comma-separated-ids
            int nu = MBox.removeListOfBoxes(user.getId(), ids);
            return result.render("count", nu).render("success", true);
        }
        else
        { // the IDs are not in the expected pattern
            return result.render("success", false);
        }
    }

    /**
     * Disables the boxes with the given IDs, given as a JSON-Object in the form: <br/>
     * {id : boolean, id : boolean, id : boolean, ...} <br/>
     * POST /mail/bulkDisable
     * 
     * @param boxIds
     *            the Box-IDs as JSON-Object, with a boolean-value which indicates whether it will be disabled or not
     * @param context
     *            the context of this request
     * @return
     */
    @FilterWith(JsonSecureFilter.class)
    public Result bulkDisableBoxes(JsonObject boxIds, Context context)
    {
        Result result = Results.json();
        if (boxIds == null)
        {
            return result.render("success", false);
        }
        String ids = getJsonArrayAsString(boxIds);
        User user = context.getAttribute("user", User.class);

        if (!StringUtils.isBlank(ids) && PATTERN_CS_BOXIDS.matcher(ids).matches())
        { // the list of boxIds have to be in the form of comma-separated-ids
            int nu = MBox.disableListOfBoxes(user.getId(), ids);
            return result.render("count", nu).render("success", true);
        }
        else
        { // the IDs are not in the expected pattern
            return result.render("success", false);
        }
    }

    /**
     * Enables the boxes with the given IDs, given as a JSON-Object in the form: <br/>
     * {id : boolean, id : boolean, id : boolean, ...} <br/>
     * POST /mail/bulkEnablePossible
     * 
     * @param boxIds
     *            the Box-IDs as JSON-Object, with a boolean-value which indicates whether it will be enabled or not
     * @param context
     *            the context of this request
     * @return
     */
    @FilterWith(JsonSecureFilter.class)
    public Result bulkEnablePossibleBoxes(JsonObject boxIds, Context context)
    {
        Result result = Results.json();
        if (boxIds == null)
        {
            return result.render("success", false);
        }
        String ids = getJsonArrayAsString(boxIds);
        User user = context.getAttribute("user", User.class);

        if (!StringUtils.isBlank(ids) && PATTERN_CS_BOXIDS.matcher(ids).matches())
        { // the list of boxIds have to be in the form of comma-separated-ids
            int nu = MBox.enableListOfBoxesIfPossible(user.getId(), ids);
            return result.render("count", nu).render("success", true);
        }
        else
        { // the IDs are not in the expected pattern
            return result.render("success", false);
        }
    }

    /**
     * Sets a new validity-period for the boxes with the given IDs, given as a JSON-Object in the form: <br/>
     * {id : boolean, id : boolean, id : boolean, ...} <br/>
     * POST /mail/bulkNewDate
     * 
     * @param boxIds
     *            the Box-IDs as JSON-Object, with a boolean-value which indicates whether there will be set a new
     *            Timestamp or not
     * @param context
     *            the context of this request
     * @return
     */
    @FilterWith(JsonSecureFilter.class)
    public Result bulkNewDate(JsonObject jsobject, Context context)
    {
        Result result = Results.json();
        String[] info = getBoxTimeArrayFromJSon(jsobject);
        User user = context.getAttribute("user", User.class);
        long dateTime = HelperUtils.parseTimeString(info[1]);
        String ids = info[0];
        if (!StringUtils.isBlank(ids) && PATTERN_CS_BOXIDS.matcher(ids).matches())
        { // the list of boxIds have to be in the form of comma-separated-ids
            if (dateTime != -1)
            {
                int nu = MBox.setNewDateForListOfBoxes(user.getId(), ids, dateTime);
                return result.render("count", nu).render("success", true);
            }
            else
            {
                return result.render("success", false);
            }
        }
        else
        { // the IDs are not in the expected pattern
            return result.render("success", false);
        }
    }

    /**
     * Resets the counters (suppressions and forwards) for the boxes with the given IDs, given as a JSON-Object in the
     * form: <br/>
     * {id : boolean, id : boolean, id : boolean, ...} <br/>
     * POST /mail/bulkReset
     * 
     * @param boxIds
     *            the Box-IDs as JSON-Object, with a boolean-value which indicates whether it will be reseted or not
     * @param context
     *            the context of this request
     * @return
     */
    @FilterWith(JsonSecureFilter.class)
    public Result bulkResetBoxes(JsonObject boxIds, Context context)
    {
        Result result = Results.json();
        if (boxIds == null)
        {
            return result.render("success", false);
        }
        String ids = getJsonArrayAsString(boxIds);
        User user = context.getAttribute("user", User.class);

        if (!StringUtils.isBlank(ids) && PATTERN_CS_BOXIDS.matcher(ids).matches())
        { // the list of boxIds have to be in the form of comma-separated-ids
            int nu = MBox.resetListOfBoxes(user.getId(), ids);
            return result.render("count", nu).render("success", true);
        }
        else
        { // the IDs are not in the expected pattern
            return result.render("success", false);
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
    @FilterWith(JsonSecureFilter.class)
    public Result deleteBoxByJson(@PathParam("id") Long boxId, Context context)
    {
        User user = context.getAttribute("user", User.class);
        if (MBox.boxToUser(boxId, user.getId()))
        { // deletes the box from DB
            MBox.delete(boxId);
            return Results.json().render("success", true);
        }
        return Results.json().render("success", false);
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
    @FilterWith(JsonSecureFilter.class)
    public Result editBoxJson(Context context, @PathParam("id") Long boxId, @JSR303Validation JsonMBox mailboxFormData,
                              Validation validation)
    {
        String errorMessage;
        Result result = Results.json();
        mailboxFormData.setId(boxId);
        result.render("domains", xcmConfiguration.DOMAIN_LIST);

        if (validation.hasViolations() || mailboxFormData == null)
        { // not all fields were filled
            errorMessage = messages.get("flash_FormError", context, Optional.of(result)).get();
            result.render("success", false).render("currentBox", mailboxFormData);
            return result.render("error", errorMessage);
        }
        else
        { // the form was filled correctly
          // check for rfc 5322 compliant length of email (64 chars for local and 254 in total)
            String completeAddress = mailboxFormData.getAddress() + "@" + mailboxFormData.getDomain();
            if (mailboxFormData.getAddress().length() > 64 || completeAddress.length() >= 255)
            {
                errorMessage = messages.get("editEmail_Flash_MailTooLong", context, Optional.of(result)).get();
                result.render("success", false).render("currentBox", mailboxFormData);
                return result.render("error", errorMessage);
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
                { // email-address changed
                    if (!MBox.mailExists(newLocalPartName, newDomainPartName))
                    { // the new address does not exist
                        String[] domains = xcmConfiguration.DOMAIN_LIST;
                        // assume that the POST-Request was modified and the domainname does not exist in our app
                        if (!Arrays.asList(domains).contains(newDomainPartName))
                        {
                            // the new domainname does not exist in the application.conf
                            // stop the process and return to the mailbox-overview page
                            errorMessage = "";
                            result.render("success", false).render("currentBox", mailboxFormData);
                            return result.render("error", errorMessage);
                        }
                        mailBox.setAddress(newLocalPartName);
                        mailBox.setDomain(newDomainPartName);
                        changes = true;
                    }
                    else
                    {
                        // the email-address already exists
                        errorMessage = messages.get("flash_MailExists", context, Optional.of(result)).get();
                        result.render("success", false).render("currentBox", mailboxFormData);
                        return result.render("statusmsg", errorMessage);
                    }
                }
                Long ts = HelperUtils.parseTimeString(mailboxFormData.getDatetime());
                if (ts == -1)
                { // a faulty timestamp was given -> return an errorpage
                    errorMessage = messages.get("flash_FormError", context, Optional.of(result)).get();
                    result.render("success", false).render("currentBox", mailboxFormData);
                    return result.render("statusmsg", errorMessage);
                }
                if ((ts != 0) && (ts < DateTime.now().getMillis()))
                { // the Timestamp lays in the past
                    errorMessage = messages.get("editEmail_Past_Timestamp", context, Optional.of(result)).get();
                    result.render("success", false).render("currentBox", mailboxFormData);
                    return result.render("statusmsg", errorMessage);
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
                    mailboxFormData.prepopulateJS(mailBox);
                    errorMessage = messages.get("flash_DataChangeSuccess", context, Optional.of(result)).get();
                    result.render("success", true).render("currentBox", mailboxFormData);
                    return result.render("statusmsg", errorMessage);
                }
                else
                { // no changes were made
                    mailboxFormData.prepopulateJS(mailBox);

                    return result.render("success", true).render("currentBox", mailboxFormData)
                                 .render("statusmsg", "No changes made");
                }

            }
            else
            { // box does not belong to this user or does not exist
                if (mailBox != null)
                {
                    mailboxFormData.prepopulateJS(mailBox);
                    result.render("currentBox", mailboxFormData);
                }
                errorMessage = messages.get("flash_FormError", context, Optional.of(result)).get();
                result.render("success", false);
                return result.render("statusmsg", errorMessage);
            }
        }
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
    @FilterWith(JsonSecureFilter.class)
    public Result expireBoxJson(@PathParam("id") Long boxId, Context context)
    {
        MBox mailBox = MBox.getById(boxId);
        User user = context.getAttribute("user", User.class);
        Result result = Results.json();
        String errorMessage = "";
        if (mailBox.belongsTo(user.getId()))
        {// check, whether the mailbox belongs to the current user
            if ((mailBox.getTs_Active() != 0) && (mailBox.getTs_Active() < DateTime.now().getMillis()))
            { // if the validity period is over, return the Edit page and give the user a response why he gets there
                errorMessage = messages.get("expireEmail_Flash_Expired", context, Optional.of(result)).get();
                return Results.json().render("success", false).render("statusmsg", errorMessage);
            }
            else
            { // otherwise just set the new status
                mailBox.enable();
                return result.render("success", true);
            }
        }
        else
        { // box does not belong to this user
            errorMessage = "Box does not belong to this user";
            return result.render("success", false).render("statusmsg", errorMessage);
        }
    }

    /**
     * Handles JSON-XHR-Requests for the mailbox-overview-page <br/>
     * GET /mail/getmails
     * 
     * @param context
     *            the Context of this Request
     * @return a JSON-Array with the boxes
     */
    @FilterWith(JsonSecureFilter.class)
    public Result jsonBox(Context context)
    {
        User user = context.getAttribute("user", User.class);

        Result result = Results.json();
        String searchString = context.getParameter("s", "");

        List<MBox> boxList = MBox.findBoxLike(searchString, user.getId());
        // GSON can't handle with cyclic references (the 1:m relation between user and MBox will end up in a cycle)
        // so we need to transform the data which does not contain the reference
        List<JsonMBox> mbdlist = new ArrayList<JsonMBox>();
        for (MBox mb : boxList)
        {
            JsonMBox mailboxdata = new JsonMBox();
            mailboxdata.prepopulateJS(mb);
            mbdlist.add(mailboxdata);
        }
        return result.json().render(mbdlist);
    }

    /**
     * Handles JSON-Requests for the domainlist<br/>
     * GET /mail/domainlist
     * 
     * @param context
     *            the Context of this Request
     * @return a JSON-Array with the domainlist
     */
    @FilterWith(JsonSecureFilter.class)
    public Result jsonDomainList(Context context)
    {
        String[] domains = xcmConfiguration.DOMAIN_LIST;
        return Results.json().render(domains);
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
    @FilterWith(JsonSecureFilter.class)
    public Result resetBoxCounterProcessXhr(@PathParam("id") Long boxId, Context context)
    {
        Result result = Results.json();

        MBox mailBox = MBox.getById(boxId);
        User user = context.getAttribute("user", User.class);

        // check if the mailbox belongs to the current user
        if (mailBox.belongsTo(user.getId()))
        {
            mailBox.resetForwards();
            mailBox.resetSuppressions();
            mailBox.update();
            return result.render("success", true);
        }
        else
        {
            return result.render("success", false);
        }
    }

    /**
     * returns a text-page with all addresses of a user<br/>
     * GET /mail/mymaillist.txt
     * 
     * @param context
     *            the Context of this Request
     * @return a text page with all addresses of a user
     */
    @FilterWith(SecureFilter.class)
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
    @FilterWith(SecureFilter.class)
    public Result showActiveMailsAsTextList(Context context)
    {
        User user = context.getAttribute("user", User.class);
        return Results.contentType("text/plain").render(MBox.getActiveMailsForTxt(user.getId()));
    }

    /**
     * We expect a JSON-Object in the form <br/>
     * { id : boolean, id : boolean,... }
     * 
     * @param json
     *            the Json-Object
     * @return a String in the form "id,id,id,id"
     */
    private String getJsonArrayAsString(JsonObject json)
    {
        if (json == null)
        {
            return "";
        }
        Set<Entry<String, JsonElement>> entrys = json.entrySet();
        StringBuilder sb = new StringBuilder();
        for (Entry<String, JsonElement> entry : entrys)
        {
            if (entry.getValue().getAsBoolean())
            {
                sb.append(entry.getKey()).append(",");
            }
        }
        if (sb.length() > 0)
        {
            sb.delete(sb.length() - 1, sb.length());
        }
        return sb.toString();
    }

    /**
     * we expect a json-object in the form {"boxes":{ id: boolean, id:boolean,.. }, "newDateTime" : "yyyy-MM-dd hh:mm"}
     * 
     * @param json
     * @return a String Array with two elements, firstly the boxids as string, separated by "," (generated by
     *         {@link #getJsonArrayAsString})
     */
    private String[] getBoxTimeArrayFromJSon(JsonObject json)
    {
        String[] info = new String[2];
        if (json == null)
        {
            return info;
        }
        Set<Entry<String, JsonElement>> entries = json.entrySet();
        for (Entry<String, JsonElement> entry : entries)
        {
            if (entry.getKey().equals("boxes"))
            {
                info[0] = getJsonArrayAsString(entry.getValue().getAsJsonObject());
            }
            else if (entry.getKey().equals("newDateTime"))
            {
                info[1] = entry.getValue().getAsString();
            }
        }
        return info;
    }
}
