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

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import javax.activation.DataSource;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.apache.commons.mail.util.MimeMessageUtils;
import org.hibernate.validator.internal.constraintvalidators.EmailValidator;
import org.joda.time.DateTime;
import org.slf4j.Logger;

import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import conf.XCMailrConf;
import etc.HelperUtils;
import etc.MailboxEntry;
import etc.TypeRef;
import filters.JsonSecureFilter;
import filters.SecureFilter;
import models.MBox;
import models.Mail;
import models.User;
import ninja.Context;
import ninja.FilterWith;
import ninja.Result;
import ninja.Results;
import ninja.i18n.Messages;
import ninja.params.Param;
import ninja.params.PathParam;
import ninja.validation.JSR303Validation;
import ninja.validation.Validation;

/**
 * Handles all actions for the (virtual) Mailboxes like add, delete and edit box
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */

@Singleton
public class BoxHandler
{
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy");

    @Inject
    XCMailrConf xcmConfiguration;

    @Inject
    Messages messages;

    @Inject
    Logger log;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    CachingSessionHandler cachingSessionHandler;

    /**
     * Opens the empty delete-box-dialog (just rendering the template).
     * 
     * @return the Delete-Box-Dialog
     */
    @FilterWith(SecureFilter.class)
    public Result deleteBoxDialog()
    {
        return Results.html();
    }

    /**
     * Opens the empty add- and edit-box-dialog (just rendering the template).
     * 
     * @return the Add- and Edit-Box-Dialog
     */
    @FilterWith(SecureFilter.class)
    public Result editBoxDialog()
    {
        // set a default entry for the validity-period
        // per default now+1h
        long nowPlusOneHour = DateTime.now().plusHours(1).getMillis();
        return Results.html().render("timeStamp", HelperUtils.parseStringTs(nowPlusOneHour)).render("tsMillis",
                                                                                                    nowPlusOneHour);
    }

    /**
     * Opens the empty new-Date-dialog (just rendering the template).
     * 
     * @return the Add- and Edit-Box-Dialog
     */
    @FilterWith(SecureFilter.class)
    public Result newDateDialog()
    {
        // get the new default-timestamp
        long tsNew = DateTime.now().plusHours(1).getMillis();
        // render it (as readable String and as millis)
        return Results.html().render("timeStampNew", HelperUtils.parseStringTs(tsNew)).render("tsMillis", tsNew);
    }

    /**
     * Generates the 'Angularized' Mailbox-Overview-Page of a {@link models.User user}.
     * 
     * @param context
     *            the Context of this Request
     * @return the Mailbox-Overview-Page
     */
    @FilterWith(SecureFilter.class)
    public Result showAngularBoxOverview(Context context)
    {
        Result result = Results.html();
        // add a mboxes-object (boolean) for the header-menu
        return result.render("ts_now", DateTime.now().getMillis()).render("mboxes", true);
    }

    /**
     * Shows the "new Mail-Forward"-Page.
     * 
     * @param context
     *            the Context of this Request
     * @return a prepopulated "Add-Box"-Form
     */
    @FilterWith(JsonSecureFilter.class)
    public Result addBoxJsonData(Context context)
    {
        MBox mailboxData = new MBox();
        // set the value of the random-name to 7
        // use the lowercase, we handle the address as case-insensitive
        String randomName = RandomStringUtils.randomAlphanumeric(7).toLowerCase();
        mailboxData.setAddress(randomName);

        // check that the generated mailname-proposal does not exist
        String[] domains = xcmConfiguration.DOMAIN_LIST;
        if (domains.length > 0)
        {// prevent OutOfBoundException
            while (MBox.mailExists(randomName, domains[0]))
            {
                randomName = RandomStringUtils.randomAlphanumeric(7).toLowerCase();
            }
        }
        mailboxData.setForwardEmails(true); // default value

        // set a default entry for the validity-period
        // per default now+1h
        long nowPlusOneHour = DateTime.now().plusHours(1).getMillis();
        mailboxData.setTs_Active(nowPlusOneHour);
        mailboxData.setDomain(domains[0]);

        return Results.json().render("currentBox", mailboxData);
    }

    /**
     * Adds a Mailbox to the {@link User}-Account.
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
    public Result addBoxJsonProcess(Context context, @JSR303Validation MBox addBoxDialogData, Validation validation)
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
        {// the mailbox already exists
            errorMessage = messages.get("flash_MailExists", context, Optional.of(result)).get();
            result.render("currentBox", addBoxDialogData);
            return result.render("success", false).render("statusmsg", errorMessage);
        }

        // set the data of the box
        String[] domains = xcmConfiguration.DOMAIN_LIST;
        if (!Arrays.asList(domains).contains(addBoxDialogData.getDomain()))
        { // the new domain-name does not exist in the application.conf
          // stop the process and return to the mailbox-overview page
            errorMessage = messages.get("editEmailDialog_JSValidation_MailInvalid", context, Optional.of(result)).get();
            result.render("currentBox", addBoxDialogData);
            return result.render("success", false).render("statusmsg", errorMessage);
        }
        Long ts = addBoxDialogData.getTs_Active();
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
        addBoxDialogData.setUsr(user);
        addBoxDialogData.resetIdAndCounterFields();
        addBoxDialogData.save();

        errorMessage = messages.get("flash_DataChangeSuccess", context, Optional.of(result)).get();
        result.render("currentBox", addBoxDialogData);
        return result.render("success", true).render("statusmsg", errorMessage);
    }

    /**
     * Deletes the boxes with the given IDs, given as a JSON-Object in the form.
     *
     * @param boxIdMap
     *            the box-id map
     * @param context
     *            the context of this request
     * @return a json-object with a "success" key and a boolean value
     */
    @FilterWith(JsonSecureFilter.class)
    public Result bulkDeleteBoxes(Object parameter, Context context)
    {
        Map<String, Boolean> boxIdMap = null;
        if (parameter instanceof LinkedHashMap<?, ?>)
        {
            boxIdMap = (Map<String, Boolean>) parameter;
        }
        else
        {
            return Results.badRequest();
        }

        Result result = Results.json();
        if (boxIdMap == null || boxIdMap.isEmpty())
            return result.render("success", false);

        List<Long> boxIds = getIdListForMap(boxIdMap);
        User user = context.getAttribute("user", User.class);
        int nu = MBox.removeListOfBoxes(user.getId(), boxIds);
        return result.render("count", nu).render("success", nu >= 0);
    }

    /**
     * Disables the boxes with the given IDs.
     * 
     * @param boxIdMap
     *            the box-id map
     * @param context
     *            the context of this request
     * @return A Json-object, containing the key "success" with a boolean value whether it was successful and if true
     *         the number of changed items
     */
    @FilterWith(JsonSecureFilter.class)
    public Result bulkDisableBoxes(Object parameter, Context context)
    {
        Map<String, Boolean> boxIdMap = null;
        if (parameter instanceof LinkedHashMap<?, ?>)
        {
            boxIdMap = (Map<String, Boolean>) parameter;
        }
        else
        {
            return Results.badRequest();
        }

        Result result = Results.json();

        if (boxIdMap == null || boxIdMap.isEmpty())
            return result.render("success", false);

        List<Long> boxIds = getIdListForMap(boxIdMap);
        User user = context.getAttribute("user", User.class);
        int nu = MBox.disableListOfBoxes(user.getId(), boxIds);
        return result.render("count", nu).render("success", nu >= 0);
    }

    /**
     * Enables the boxes with the given IDs.
     * 
     * @param boxIdMap
     *            the box-id map
     * @param context
     *            the context of this request
     * @return A Json-object, containing the key "success" with a boolean value whether it was successful and if true
     *         the number of changed items
     */
    @FilterWith(JsonSecureFilter.class)
    public Result bulkEnablePossibleBoxes(Object parameter, Context context)
    {
        Map<String, Boolean> boxIdMap = null;
        if (parameter instanceof LinkedHashMap<?, ?>)
        {
            boxIdMap = (Map<String, Boolean>) parameter;
        }
        else
        {
            return Results.badRequest();
        }

        Result result = Results.json();

        if (boxIdMap == null || boxIdMap.isEmpty())
            return result.render("success", false);

        List<Long> boxIds = getIdListForMap(boxIdMap);
        User user = context.getAttribute("user", User.class);
        int nu = MBox.enableListOfBoxesIfPossible(user.getId(), boxIds);
        return result.render("count", nu).render("success", nu >= 0);
    }

    /**
     * Sets a new validity-period for the boxes with the given IDs.
     *
     * @param input
     *            the box-id map
     * @param context
     *            the context of this request
     * @return A Json-object, containing the key "success" with a boolean value whether it was successful and if true
     *         the number of changed items
     */
    @FilterWith(JsonSecureFilter.class)
    public Result bulkNewDate(Object parameter, Context context)
    {
        Map<String, Object> input = null;
        if (parameter instanceof LinkedHashMap<?, ?>)
        {
            input = (Map<String, Object>) parameter;
        }
        else
        {
            return Results.badRequest();
        }

        Result result = Results.json();
        User user = context.getAttribute("user", User.class);

        if (input == null || input.isEmpty())
        {
            return result.render("success", false);
        }
        String newDate = (String) input.get("newDateTime");
        Map<String, Boolean> boxIds = objectMapper.convertValue(input.get("boxes"), TypeRef.MAP_STRING_BOOLEAN);
        long dateTime = HelperUtils.parseTimeString(newDate);
        if (dateTime == -1 || boxIds == null)
            return result.render("success", false);

        List<Long> boxIdList = getIdListForMap(boxIds);
        int numberOfItems = MBox.setNewDateForListOfBoxes(user.getId(), boxIdList, dateTime);
        return result.render("count", numberOfItems).render("success", true);
    }

    /**
     * Resets the counters (suppressions and forwards) for the boxes with the given IDs.
     *
     * @param boxIdMap
     *            the box-id map
     * @param context
     *            the context of this request
     * @return A Json-object, containing the key "success" with a boolean value whether it was successful and if true
     *         the number of changed items
     */
    @FilterWith(JsonSecureFilter.class)
    public Result bulkResetBoxes(Object parameter, Context context)
    {
        Map<String, Boolean> boxIdMap = null;
        if (parameter instanceof LinkedHashMap<?, ?>)
        {
            boxIdMap = (Map<String, Boolean>) parameter;
        }
        else
        {
            return Results.badRequest();
        }

        Result result = Results.json();

        if (boxIdMap == null || boxIdMap.isEmpty())
            return result.render("success", false);

        List<Long> boxIds = getIdListForMap(boxIdMap);
        User user = context.getAttribute("user", User.class);
        int nu = MBox.resetListOfBoxes(user.getId(), boxIds);
        return result.render("count", nu).render("success", nu >= 0);
    }

    /**
     * Deletes a Box from the DB.
     * 
     * @param boxId
     *            the ID of the Mailbox
     * @param context
     *            the Context of this Request
     * @return A Json-object, containing the key "success" with a boolean value whether it was successful
     */
    @FilterWith(JsonSecureFilter.class)
    public Result deleteBoxByJson(@PathParam("id") Long boxId, Context context)
    {
        Result result = Results.json();
        User user = context.getAttribute("user", User.class);
        if (MBox.boxToUser(boxId, user.getId()))
        { // deletes the box from DB
            MBox.delete(boxId);
            return result.render("success", true);
        }
        else
        {
            String errorMessage = messages.get("flash_FormError", context, Optional.of(result)).get();
            return result.render("success", false).render("statusMsg", errorMessage);
        }
    }

    /**
     * Edits a Mailbox.
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
    public Result editBoxJson(Context context, @PathParam("id") Long boxId, @JSR303Validation MBox mailboxFormData,
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

        // the form was filled correctly
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
        if (mailBox == null || !mailBox.belongsTo(usr.getId()))
        { // box does not belong to this user or does not exist
            errorMessage = messages.get("flash_FormError", context, Optional.of(result)).get();
            result.render("success", false);
            return result.render("statusmsg", errorMessage);
        }
        // the box with the given id exists and the current user is the owner of the mailbox
        boolean changes = false;
        String newLocalPartName = mailboxFormData.getAddress().toLowerCase();
        String newDomainPartName = mailboxFormData.getDomain().toLowerCase();

        if (!mailBox.getAddress().equals(newLocalPartName) || !mailBox.getDomain().equals(newDomainPartName))
        { // email-address changed
            if (MBox.mailExists(newLocalPartName, newDomainPartName))
            {// the email-address already exists -> error
                errorMessage = messages.get("flash_MailExists", context, Optional.of(result)).get();
                result.render("success", false).render("currentBox", mailboxFormData);
                return result.render("statusmsg", errorMessage);
            }
            // the new address does not exist
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
        Long ts = mailboxFormData.getTs_Active();
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

        if (mailBox.isForwardEmails() != mailboxFormData.isForwardEmails())
        {
            mailBox.setForwardEmails(mailboxFormData.isForwardEmails());
            changes = true;
        }

        // Updates the Box if changes were made
        if (changes)
        {
            mailBox.setExpired(false);
            mailBox.update();
            mailboxFormData = MBox.getById(mailBox.getId());
            errorMessage = messages.get("flash_DataChangeSuccess", context, Optional.of(result)).get();
            result.render("success", true).render("currentBox", mailboxFormData);
            return result.render("statusmsg", errorMessage);
        }

        // no changes were made
        mailboxFormData = MBox.getById(mailBox.getId());

        return result.render("success", true).render("currentBox", mailboxFormData).render("statusmsg",
                                                                                           "No changes made");
    }

    /**
     * Sets the Box valid/invalid
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
        if (!mailBox.belongsTo(user.getId()))
        { // box does not belong to this user -> error
            errorMessage = messages.get("flash_BoxToUser", context, Optional.of(result)).get();
            return result.render("success", false).render("statusmsg", errorMessage);
        }
        // check, whether the mailbox belongs to the current user
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

    /**
     * Handles JSON-XHR-Requests for the mailbox-overview-page
     * 
     * @param context
     *            the context of this request
     * @return a JSON-Array with the boxes
     */
    @FilterWith(JsonSecureFilter.class)
    public Result jsonBox(Context context)
    {
        User user = context.getAttribute("user", User.class);

        Result result = Results.json();
        String searchString = context.getParameter("s", "");

        List<MBox> boxList = MBox.findBoxLike(searchString, user.getId());
        return result.json().render(boxList);
    }

    /**
     * Handles JSON-Requests for the domain list.
     * 
     * @param context
     *            the context of this request
     * @return a JSON-Array with the domain list
     */
    @FilterWith(JsonSecureFilter.class)
    public Result jsonDomainList(Context context)
    {
        return Results.json().render(xcmConfiguration.DOMAIN_LIST);
    }

    /**
     * Sets the Values of the Counters for the Box, given by their ID, to zero.
     * 
     * @param boxId
     *            the ID of the Mailbox
     * @param context
     *            the context of this request
     * @return the Mailbox-Overview-Page
     */
    @FilterWith(JsonSecureFilter.class)
    public Result resetBoxCounterProcessXhr(@PathParam("id") Long boxId, Context context)
    {
        Result result = Results.json();

        MBox mailBox = MBox.getById(boxId);
        User user = context.getAttribute("user", User.class);

        // check if the mailbox belongs to the current user
        if (!mailBox.belongsTo(user.getId()))
            return result.render("success", false);

        mailBox.resetForwards();
        mailBox.resetSuppressions();
        mailBox.update();
        return result.render("success", true);
    }

    /**
     * Returns the user's email addresses as plain text.
     * 
     * @param context
     *            the context of this request
     * @return a text page with all addresses of a user
     */
    @FilterWith(SecureFilter.class)
    public Result showMailsAsTextList(Context context)
    {
        User user = context.getAttribute("user", User.class);
        return Results.contentType("text/plain").render(MBox.getMailsForTxt(user.getId()));
    }

    /**
     * Returns the user's active email addresses as plain text.
     * 
     * @param context
     *            the context of this request
     * @return a text page with all active addresses of a user
     */
    @FilterWith(SecureFilter.class)
    public Result showActiveMailsAsTextList(Context context)
    {
        User user = context.getAttribute("user", User.class);
        return Results.contentType("text/plain").render(MBox.getActiveMailsForTxt(user.getId()));
    }

    /**
     * Returns a text-page with all selected addresses of a user.
     * 
     * @param context
     *            the context of this request
     * @return a text page with all selected addresses of a user
     */
    @FilterWith(SecureFilter.class)
    public Result showSelectedMailsAsTextList(@Param("jsonObj") String inputList, Context context)
    {
        Result result = Results.text();
        String errorMessage = messages.get("mailbox_Flash_NoBoxSelected", context, Optional.of(result)).get();
        if (inputList == null)
            return result.render(errorMessage);

        Map<String, Boolean> boxIdMap = getMapFoMaprStrings(inputList);
        if (boxIdMap == null || boxIdMap.isEmpty())
            return result.render(errorMessage);

        User user = context.getAttribute("user", User.class);
        List<Long> boxes = getIdListForMap(boxIdMap);

        return result.render(MBox.getSelectedMailsForTxt(user.getId(), boxes));
    }

    private Map<String, Boolean> getMapFoMaprStrings(String input)
    {
        Map<String, Boolean> boxIdMap = null;
        try
        {
            boxIdMap = objectMapper.readValue(input, new TypeReference<HashMap<String, Boolean>>()
            {
            });
        }
        catch (IOException e)
        {
            log.error(e.getLocalizedMessage());
            log.debug("trace:" + e);
        }
        if (boxIdMap == null || boxIdMap.isEmpty())
            return null;

        return boxIdMap;
    }

    private List<Long> getIdListForMap(Map<String, Boolean> boxIdMap)
    {
        List<Long> boxIds = new ArrayList<Long>();
        if (boxIdMap == null || boxIdMap.isEmpty())
            return boxIds;

        for (Entry<String, Boolean> entry : boxIdMap.entrySet())
        {
            log.info("key:" + entry.getKey() + " value:" + entry.getValue());
            if (entry.getValue())
            {
                long boxId = Long.valueOf(entry.getKey());
                boxIds.add(boxId);
            }
        }
        return boxIds;
    }

    public Result createTemporaryMailAddress(@PathParam("token") String apiToken,
                                             @PathParam("mailAddress") String desiredMailAddress,
                                             @PathParam("validTime") String validTime, Context context)
    {
        if (apiToken == null || desiredMailAddress == null || validTime == null)
            return Results.badRequest();

        if (!new EmailValidator().isValid(desiredMailAddress, null))
            return Results.badRequest();

        // check token
        final User user = User.findUserByToken(apiToken);
        if (user == null)
        {
            // there is no user assigned with that api token
            log.error("Token invalid");
            return Results.forbidden();
        }

        // check desired mail address
        String[] mailAddressParts = HelperUtils.splitMailAddress(desiredMailAddress.toLowerCase());
        if (!HelperUtils.checkEmailAddressValidness(mailAddressParts, xcmConfiguration.DOMAIN_LIST))
        { // mail is not in format "localpart@domain" or domain is not configured in XCMailr
            log.error("Email address invalid: " + desiredMailAddress);
            return Results.forbidden();
        }

        int parsedValidTimeMinutes;
        // check valid time
        try
        {
            parsedValidTimeMinutes = Integer.valueOf(validTime);
            if (parsedValidTimeMinutes < 1 || parsedValidTimeMinutes > xcmConfiguration.TEMPORARY_MAIL_MAX_VALID_TIME)
            {
                return Results.badRequest();
            }
        }
        catch (NumberFormatException e)
        {
            // invalid format
            log.error("Email valid time invalid: " + validTime);
            return Results.badRequest();
        }

        // check if that email address is already claimed by someone
        final MBox mailbox = Ebean.find(MBox.class).where()//
                                  .eq("address", mailAddressParts[0]) //
                                  .eq("domain", mailAddressParts[1]).findUnique();

        final Instant validUntil = Instant.now().plus(parsedValidTimeMinutes, ChronoUnit.MINUTES);
        final long validUntil_ts = validUntil.toEpochMilli();
        if (mailbox != null)
        {
            // mailbox exists, check if the user releated to it is the same as the token bearer
            if (mailbox.getUsr().getId() == user.getId())
            {
                log.info("Reactivate mailbox: " + desiredMailAddress);
                // reactivate address
                mailbox.enable();
                mailbox.setTs_Active(validUntil_ts);
                mailbox.save();
            }
            else
            {
                // another user owns that address
                log.info("Email address is owned by user: " + mailbox.getUsr().getMail());
                return Results.forbidden();
            }
        }
        else
        {
            log.info("Create mailbox " + desiredMailAddress);
            // create the address for the current user
            new MBox(mailAddressParts[0], mailAddressParts[1], validUntil_ts, false, user).save();
        }

        final Map<String, Object> data = new HashMap<>();
        data.put("emailAddress", desiredMailAddress);
        data.put("emailValidity", Integer.toString(parsedValidTimeMinutes));
        data.put("emailValidUntil", Long.toString(validUntil_ts));
        data.put("emailValidUntilDate", validUntil.atZone(ZoneId.of("UTC")).format(DATE_FORMAT));

        final String formatParameter = context.getParameter("format", "html").toLowerCase();
        if ("html".equals(formatParameter))
        {
            return Results.html().render(data);
        }
        else if ("json".equals(formatParameter))
        {
            return Results.json().render(data);
        }
        else
        {
            return Results.forbidden();
        }
    }

    public Result queryMailbox(@PathParam("token") String apiToken, @PathParam("mailAddress") String mailAddress,
                               Context context)
        throws Exception
    {
        if (apiToken == null || mailAddress == null)
            return Results.badRequest();

        log.trace("passed null check");
        User user = User.findUserByToken(apiToken);

        if (user == null)
        {
            // there is no user assigned with that api token
            log.error("Token invalid");
            return Results.unauthorized();
        }

        // we put the username into the cookie, but use the id of the cookie for authentication
        String sessionKey = context.getSession().getId();
        cachingSessionHandler.set(sessionKey, xcmConfiguration.COOKIE_EXPIRETIME, user);
        // set a reverse mapped user-mail -> sessionId-list in the memcached server to handle
        // session-expiration for admin-actions (e.g. if an admin deletes a user that is currently
        // logged-in)
        cachingSessionHandler.setSessionUser(user, sessionKey, xcmConfiguration.COOKIE_EXPIRETIME);
        context.getSession().put("username", user.getMail());

        String[] mailAddressParts = HelperUtils.splitMailAddress(mailAddress.toLowerCase());
        MBox mailbox = MBox.getByName(mailAddressParts[0], mailAddressParts[1]);

        if (mailbox == null)
        {
            log.info("Mailbox not found: " + mailAddress);
            return Results.badRequest();
        }

        if (!mailbox.belongsTo(user.getId()))
        {
            log.error("Mailbox belongs to another user");
            return Results.badRequest();
        }

        List<MBox> userMailBoxes = new LinkedList<>();
        userMailBoxes.add(mailbox);

        List<Mail> emails = Ebean.find(Mail.class).where() //
                                 .eq("mailbox_id", mailbox.getId()) //
                                 .order("receiveTime")//
                                 .findList();

        String senderRegex = context.getParameter("from");
        String subjectRegex = context.getParameter("subject");
        String plainTextRegex = context.getParameter("textContent");
        String htmlTextRegex = context.getParameter("htmlContent");
        String headerRegex = context.getParameter("mailHeader");
        boolean lastMatch = context.getParameter("lastMatch") != null;

        final Pattern senderPattern;
        final Pattern subjectPattern;
        final Pattern plainTextPattern;
        final Pattern htmlTextPattern;
        final Pattern headerPattern;
        try
        {
            senderPattern = senderRegex != null ? Pattern.compile(senderRegex, Pattern.MULTILINE | Pattern.DOTALL)
                                                : null;
            subjectPattern = subjectRegex != null ? Pattern.compile(subjectRegex, Pattern.MULTILINE | Pattern.DOTALL)
                                                  : null;
            plainTextPattern = plainTextRegex != null ? Pattern.compile(plainTextRegex,
                                                                        Pattern.MULTILINE | Pattern.DOTALL)
                                                      : null;
            htmlTextPattern = htmlTextRegex != null ? Pattern.compile(htmlTextRegex, Pattern.MULTILINE | Pattern.DOTALL)
                                                    : null;
            headerPattern = headerRegex != null ? Pattern.compile(headerRegex, Pattern.MULTILINE | Pattern.DOTALL)
                                                : null;
        }
        catch (PatternSyntaxException e)
        {
            return Results.badRequest();
        }

        final List<MailboxEntry> entries = new LinkedList<>();
        for (int i = 0; i < emails.size(); i++)
        {
            Mail email = emails.get(i);

            MailboxEntry mailboxEntry = new MailboxEntry(mailAddress, email);
            if ((senderPattern == null || senderPattern.matcher(mailboxEntry.sender).find()) //
                && (subjectPattern == null || subjectPattern.matcher(mailboxEntry.subject).find()) //
                && (plainTextPattern == null || plainTextPattern.matcher(mailboxEntry.textContent).find()) //
                && (htmlTextPattern == null || htmlTextPattern.matcher(mailboxEntry.htmlContent).find()) //
                && (headerPattern == null || headerPattern.matcher(mailboxEntry.mailHeader).find()))
            {
                entries.add(mailboxEntry);
            }
        }

        final String formatParameter = context.getParameter("format", "html").toLowerCase();
        if ((entries.size() > 1 && lastMatch) || "header".equals(formatParameter))
        {
            // only retrieve the last match, also for plain format since we can not distinct multiple entries in the
            // output
            if (entries.size() >= 1)
            {
                MailboxEntry lastEntry = entries.get(entries.size() - 1);
                entries.clear();
                entries.add(lastEntry);
            }
        }

        if ("html".equals(formatParameter))
        {
            // display content embedded in the site
            return Results.html().render("accountEmails", entries).render("mailaddress", mailAddress);
        }
        else if ("json".equals(formatParameter))
        {
            // return content as json structure

            return Results.json().render(entries);
        }
        else if ("header".equals(formatParameter))
        {
            // output plain mail

            // safety check
            if (entries.size() == 0 || entries.size() > 1)
            {
                return Results.badRequest();
            }

            return Results.text().render(entries.get(0).mailHeader);
        }
        else
        {
            return Results.forbidden();
        }
    }

    @FilterWith(SecureFilter.class)
    public Result queryAllMailboxes(Context context, @Param("offset") final Integer offset,
                                    @Param("limit") final Integer limit, @Param("sort") String sort,
                                    @Param("order") String order, @Param("search") String search)
        throws Exception
    {
        final String formatParameter = context.getParameter("format", "html").toLowerCase();

        if ("html".equals(formatParameter))
        {
            return Results.html();
        }
        else if ("json".equals(formatParameter))
        {

            final int iOffset = offset == null ? 0 : Math.max(0, offset);
            final int iLimit = limit == null ? 0 : Math.max(limit, 1);

            sort = getOrderColumn(sort);
            order = getOrderDirection(order);

            User user = context.getAttribute("user", User.class);
            List<MBox> mailboxes = user.getBoxes();
            List<Long> mailboxIds = mailboxes.stream().map(mbox -> mbox.getId()).collect(Collectors.toList());

            final List<Mail> mails = Ebean.find(Mail.class).where().in("mailbox_id", mailboxIds)
                                          .orderBy(sort + " " + order).findList();

            final List<MailboxEntry> matches = new ArrayList<>();

            // there is a searchphrase. prefilter the results
            // apparent we can't do this in the db since messages are stored encoded

            for (Mail mail : mails)
            {
                MailboxEntry mailboxEntry = new MailboxEntry(mail.getMailbox().getFullAddress(), mail);

                if (StringUtils.isBlank(search) || mailboxEntry.matchesSearchPhrase(search))
                {
                    matches.add(mailboxEntry);
                }
            }

            final int nbMatches = matches.size();
            final List<MailboxEntry> result;
            if (iOffset >= nbMatches || iLimit == 0)
            {
                result = Collections.emptyList();
            }
            else
            {
                result = matches.subList(iOffset, Math.min(iOffset + iLimit, nbMatches));
            }

            return Results.json().render("rows", result).render("total", mails.size());
        }
        else
        {
            return Results.forbidden();
        }
    }

    private String getOrderColumn(String orderBy)
    {
        return ("subject".equals(orderBy) || "sender".equals(orderBy)) ? orderBy : "receive_time";
    }

    private String getOrderDirection(String orderBy)
    {
        return ("asc".equals(orderBy)) ? orderBy : "desc";
    }

    @FilterWith(SecureFilter.class)
    public Result downloadMailAttachment(Context context, @PathParam("downloadToken") String downloadToken,
                                         @PathParam("filename") String filename)
        throws Exception
    {
        List<Mail> foundMails = Ebean.find(Mail.class).where().eq("uuid", downloadToken).findList();

        if (foundMails.isEmpty())
        {
            return Results.badRequest();
        }
        Mail mail = foundMails.get(0);

        MimeMessage mimeMessage = MimeMessageUtils.createMimeMessage(null, mail.getMessage());
        MimeMessageParser mimeMessageParser = new MimeMessageParser(mimeMessage);
        mimeMessageParser.parse();

        DataSource foundAttachment = null;
        for (DataSource attachment : mimeMessageParser.getAttachmentList())
        {
            if (attachment.getName().equals(filename))
            {
                foundAttachment = attachment;
                break;
            }
        }

        if (foundAttachment == null)
        {
            return Results.badRequest();
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        try (final InputStream is = foundAttachment.getInputStream())
        {
            IOUtils.copy(is, baos);
        }

        return Results.ok().contentType(foundAttachment.getContentType()).renderRaw(baos.toByteArray());
    }
}
