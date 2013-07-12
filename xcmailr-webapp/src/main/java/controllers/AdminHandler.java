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
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import conf.XCMailrConf;
import ninja.Context;
import ninja.FilterWith;
import ninja.Result;
import ninja.Results;
import ninja.i18n.Messages;
import ninja.params.Param;
import ninja.params.PathParam;
import etc.HelperUtils;
import filters.AdminFilter;
import filters.SecureFilter;
import models.Domain;
import models.MailTransaction;
import models.PageList;
import models.User;
import models.UserFormData;

/**
 * Handles all Actions for the Administration-Section
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany.
 */

@FilterWith(
    {
        SecureFilter.class, AdminFilter.class
    })
@Singleton
public class AdminHandler
{
    @Inject
    XCMailrConf xcmConfiguration;

    @Inject
    Messages messages;

    @Inject
    MailrMessageSenderFactory mailSender;

    @Inject
    MemCachedSessionHandler mcsh;

    /**
     * Shows the Administration-Index-Page<br/>
     * GET site/admin
     * 
     * @param context
     *            the Context of this Request
     * @return the Admin-Index-Page
     */
    public Result showAdmin(Context context)
    {
        return Results.html();
    }

    /**
     * Shows a List of all {@link models.User Users} in the DB <br/>
     * GET site/admin/users
     * 
     * @param context
     *            the Context of this Request
     * @return a List of all Users
     */
    public Result showUsers(Context context)
    {
        Result result = Results.html();
        User user = context.getAttribute("user", User.class);
        // render the userID in the result to identify the row where no buttons will be shown
        result.render("uid", user.getId());
        // set a default number or the number which the user had chosen
        HelperUtils.parseEntryValue(context, xcmConfiguration.APP_DEFAULT_ENTRYNO);

        // get the default number of entries per page
        int entries = Integer.parseInt(context.getSessionCookie().get("no"));

        // generate the paged-list to get pagination in the pattern
        PageList<User> pagedUserList;
        String searchString = context.getParameter("s", "");
        if (searchString.equals(""))
        { // there is no searchString
            pagedUserList = new PageList<User>(User.all(), entries);
        }
        else
        { // there is a searchString, search the related users
            pagedUserList = new PageList<User>(User.findUserLike(searchString), entries);
            result.render("searchValue", searchString);
        }
        // add the user-list
        result.render("users", pagedUserList);

        return result;
    }

    /**
     * Shows a List of all {@link models.Status Status} in the DB <br/>
     * GET site/admin/summedtx
     * 
     * @param context
     *            the Context of this Request
     * @return a List of all Status
     */
    public Result showSummedTransactions(Context context)
    {
        return Results.html().render("stats", MailTransaction.getStatusList());
    }

    /**
     * Shows a paginated List of all {@link models.MailTransaction Mailtransactions} in the DB <br/>
     * GET site/admin/mtxs
     * 
     * @param context
     *            the Context of this Request
     * @return the Page to show paginated MailTransactions
     */
    public Result pagedMTX(Context context)
    {
        // set a default number or the number which the user had chosen
        HelperUtils.parseEntryValue(context, xcmConfiguration.APP_DEFAULT_ENTRYNO);
        // get the default number of entries per page
        int entries = Integer.parseInt(context.getSessionCookie().get("no"));

        // generate the paged-list to get pagination on the page
        PageList<MailTransaction> pagedMailTransactionList = new PageList<MailTransaction>(
                                                                                           MailTransaction.all("ts desc"),
                                                                                           entries);

        return Results.html().render("plist", pagedMailTransactionList);
    }

    /**
     * Delete a time-specified number of MailTransactions <br/>
     * GET /admin/mtxs/delete/{time}
     * 
     * @param time
     *            the time in days (all before will be deleted)
     * @return to the MailTransaction-Page
     */

    public Result deleteMTXProcess(@PathParam("time") Integer time, Context context)
    {
        Result result = Results.html().template("/views/Application/index.ftl.html");
        if (time == null)
        {
            return result.redirect(context.getContextPath() + "/admin/mtxs");
        }
        if (time == -1)
        { // all entries will be deleted
            MailTransaction.deleteTxInPeriod(null);
        }
        else
        {
            // calculate the time and delete all entries before
            DateTime dt = DateTime.now().minusDays(time);
            MailTransaction.deleteTxInPeriod(dt.getMillis());
        }

        return result.redirect(context.getContextPath() + "/admin/mtxs");
    }

    /**
     * Activates or Deactivates the User with the given ID <br/>
     * POST /admin/activate/{id}
     * 
     * @param userId
     *            ID of a User
     * @param context
     *            the Context of this Request
     * @return the User-Overview-Page (/admin/users)
     */
    public Result activateUserProcess(@PathParam("id") Long userId, Context context)
    {
        Result result = Results.html().template("/views/Application/index.ftl.html");

        // get the user who executes this action
        User executingUser = context.getAttribute("user", User.class);
        if (executingUser.getId() != userId)
        { // the user to (de-)activate is not the user who performs this action

            // activate or deactivate the user
            boolean active = User.activate(userId);

            // generate the (de-)activation-information mail and send it to the user
            User user = User.getById(userId);
            String from = xcmConfiguration.ADMIN_ADDRESS;
            String host = xcmConfiguration.MB_HOST;

            Optional<String> optLanguage = Optional.of(user.getLanguage());

            if (active)
            { // the account is now active
              // generate the message title

                String subject = messages.get("user_Activate_Title", optLanguage, host).get();
                // generate the message body

                String content = messages.get("user_Activate_Message", optLanguage, user.getForename()).get();
                // send the mail
                mailSender.sendMail(from, user.getMail(), content, subject);
            }
            else
            {// the account is now inactive
             // generate the message title
                String subject = messages.get("user_Deactivate_Title", optLanguage, host).get();
                // generate the message body
                String content = messages.get("user_Deactivate_Message", optLanguage, user.getForename()).get();
                // send the mail
                mailSender.sendMail(from, user.getMail(), content, subject);

                // delete the sessions of this user
                mcsh.deleteUsersSessions(User.getById(userId));
            }
            return result.redirect(context.getContextPath() + "/admin/users");
        }
        else
        { // the admin wants to disable his own account, this is not allowed
            return result.redirect(context.getContextPath() + "/admin/users");
        }
    }

    /**
     * Pro- or Demotes the {@link models.User User} with the given ID <br/>
     * POST /admin/promote/{id}
     * 
     * @param userId
     *            ID of a {@link models.User User}
     * @param context
     *            the Context of this Request
     * @return the User-Overview-Page (/admin/users)
     */
    public Result promoteUserProcess(@PathParam("id") Long userId, Context context)
    {
        Result result = Results.html().template("/views/Application/index.ftl.html");
        User user = context.getAttribute("user", User.class);
        if (user.getId() != userId)
        { // the user to pro-/demote is not the user who performs this action
            User.promote(userId);
            // update all of the sessions
            mcsh.updateUsersSessions(User.getById(userId));
        }
        return result.redirect(context.getContextPath() + "/admin/users");
    }

    /**
     * Handles the {@link models.User User}-Delete-Function <br/>
     * POST /admin/delete/{id}
     * 
     * @param deleteUserId
     *            the ID of a {@link models.User User}
     * @param context
     *            the Context of this Request
     * @return the User-Overview-Page (/admin/users)
     */
    public Result deleteUserProcess(@PathParam("id") Long deleteUserId, Context context)
    {
        Result result = Results.html().template("/views/Application/index.ftl.html");
        User user = context.getAttribute("user", User.class);

        if (user.getId() != deleteUserId)
        { // the user to delete is not the user who performs this action
            mcsh.deleteUsersSessions(User.getById(deleteUserId));
            User.delete(deleteUserId);
        }

        return result.redirect(context.getContextPath() + "/admin/users");
    }

    /**
     * Handles JSON-Requests from the search <br/>
     * GET /admin/usersearch
     * 
     * @param context
     *            the Context of this Request
     * @return a JSON-Array with the userdatalist
     */
    public Result jsonUserSearch(Context context)
    {
        List<User> userList;
        Result result = Results.html();
        String searchString = context.getParameter("s", "");
        if (searchString.equals(""))
        {
            userList = new ArrayList<User>();
        }
        else
        {
            userList = User.findUserLike(searchString);
        }

        List<UserFormData> userDatalist = new ArrayList<UserFormData>();
        for (User currentUser : userList)
        {
            UserFormData mbd = UserFormData.prepopulate(currentUser);
            userDatalist.add(mbd);

        }
        return result.json().render(userDatalist);
    }

    /**
     * Shows a page that contains a list of all allowed domains of emails for registration <br/>
     * GET /admin/whitelist
     * 
     * @param context
     *            the Context of this Request
     * @return the domain-whitelist page
     */
    public Result showDomainWhitelist(Context context)
    {
        List<Domain> domainList = Domain.getAll();

        return Results.html().render("domains", domainList);
    }

    // TODO DOC
    public Result callRemoveDomain(Context context, @Param("removeDomainsSelection") Long remDomainId)
    {
        System.out.println(remDomainId);
        Domain domain = Domain.getById(remDomainId);
        Result result = Results.html().template("/views/AdminHandler/removeDomainConfirmation.ftl.html");
        return result.render("domain", domain);
        // return result.redirect(context.getContextPath() + "/admin/whitelist");
    }

    // TODO DOC
    public Result handleRemoveDomain(Context context, @Param("action") String action, @Param("domainId") long domainId)
    {

        Result result = Results.html().template("/views/system/noContent.ftl.html");
        // TODO add checks for the inputs (the params) here
        if (!StringUtils.isBlank(action))
        {
            if (action.equals("abort"))
            { // the admin wants to abort this action
                return result.redirect(context.getContextPath() + "/admin/whitelist");
            }
            if (action.equals("deleteUsersAndDomain"))
            {
                Domain domain = Domain.getById(domainId);
                List<User> usersToDelete = User.getUsersOfDomain(domain.getDomainname());

                // delete the sessions of the users
                for (User userToDelete : usersToDelete)
                {
                    mcsh.deleteUsersSessions(userToDelete);
                    User.delete(userToDelete.getId());
                }

                domain.delete();
                return result.redirect(context.getContextPath() + "/admin/whitelist");
            }
            if (action.equals("deleteDomain"))
            {// just delete the domain
                Domain.delete(domainId);
                return result.redirect(context.getContextPath() + "/admin/whitelist");
            }
        }
        result = Results.html().template("/views/system/noContent.ftl.html");
        return result.redirect(context.getContextPath() + "/admin/whitelist");
    }

    // TODO DOC
    public Result addDomain(Context context, @Param("domainName") String domainName)
    {
        // TODO add validation or sth for the domain
        Domain domain = new Domain(domainName);
        domain.save();
        Result result = Results.html().template("/views/system/noContent.ftl.html");
        return result.redirect(context.getContextPath() + "/admin/whitelist");
    }
}
