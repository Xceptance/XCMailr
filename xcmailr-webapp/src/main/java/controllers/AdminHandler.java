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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import conf.XCMailrConf;
import etc.HelperUtils;
import filters.AdminFilter;
import filters.SecureFilter;
import filters.WhitelistFilter;
import models.Domain;
import models.MailTransaction;
import models.PageList;
import models.User;
import models.UserFormData;
import ninja.Context;
import ninja.FilterWith;
import ninja.Result;
import ninja.Results;
import ninja.i18n.Messages;
import ninja.params.Param;
import ninja.params.PathParam;

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
    CachingSessionHandler cachingSessionHandler;

    private static final Pattern PATTERN_DOMAINS = Pattern.compile("^[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*\\.[a-z]{2,6}");

    /**
     * Shows the Administration-Index-Page.
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
     * Shows a list of all {@link models.User users} in the DB.
     * 
     * @param context
     *            the context of this request
     * @return a list of all users
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
        int entries = Integer.parseInt(context.getSession().get("no"));

        String searchString = context.getParameter("s", "");
        // generate the paged-list to get pagination in the template
        PageList<User> pagedUserList = new PageList<User>(User.findUserLike(searchString), entries);
        // add the user-list
        result.render("users", pagedUserList);

        if (!searchString.equals(""))
        { // there is a searchString
            result.render("searchValue", searchString);
        }
        return result;
    }

    /**
     * Shows a list of all {@link models.Status status} in the DB.
     * 
     * @param context
     *            the context of this request
     * @return status list
     */
    public Result showSummedTransactions(Context context)
    {
        return Results.html().render("stats", MailTransaction.getStatusList());
    }

    /**
     * Shows a paginated list of all {@link models.MailTransaction mail-transactions} in the DB.
     * 
     * @param context
     *            the context of this request
     * @return the page to show paginated mail-transactions
     */
    public Result pagedMTX(Context context, @Param("p") int page)
    {
        // set a default number or the number which the user had chosen
        HelperUtils.parseEntryValue(context, xcmConfiguration.APP_DEFAULT_ENTRYNO);
        // get the default number of entries per page
        int entries = Integer.parseInt(context.getSession().get("no"));

        // set a default value if there's no one given
        page = (page == 0) ? 1 : page;

        // generate the paged-list to get pagination on the page
        PageList<MailTransaction> pagedMailTransactionList = new PageList<MailTransaction>(MailTransaction.getSortedAndLimitedList(xcmConfiguration.MTX_LIMIT),
                                                                                           entries);
        return Results.html().render("plist", pagedMailTransactionList).render("curPage", page);
    }

    /**
     * Delete a time-specified number of mail-transactions.
     * 
     * @param time
     *            the time in days (all before will be deleted)
     * @return mail-transactions overview page
     */
    public Result deleteMTXProcess(@PathParam("time") Integer time, Context context)
    {
        if (time == null)
            return Results.redirect(context.getContextPath() + "/admin/mtxs");

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
        return Results.redirect(context.getContextPath() + "/admin/mtxs");
    }

    /**
     * Activates or deactivates the user with the given ID.
     * 
     * @param userId
     *            ID of a user
     * @param context
     *            the context of this request
     * @return users overview page
     */
    public Result activateUserProcess(@PathParam("id") Long userId, Context context)
    {
        // get the user who executes this action
        User executingUser = context.getAttribute("user", User.class);
        if (executingUser.getId() == userId)
            // the admin wants to disable his own account, this is not allowed
            return Results.redirect(context.getContextPath() + "/admin/users");

        // activate or deactivate the user
        boolean active = User.activate(userId);

        // generate the (de-)activation-information mail and send it to the user
        User user = User.getById(userId);
        String from = xcmConfiguration.ADMIN_ADDRESS;
        String host = xcmConfiguration.MB_HOST;

        Optional<String> optLanguage = Optional.of(user.getLanguage());

        // generate the message title
        String subject = messages.get(active ? "user_Activate_Title" : "user_Deactivate_Title", optLanguage, host)
                                 .get();
        // generate the message body
        String content = messages.get(active ? "user_Activate_Message" : "user_Deactivate_Message", optLanguage,
                                      user.getForename())
                                 .get();
        // send the mail
        mailSender.sendMail(from, user.getMail(), content, subject);
        if (!active)
        { // delete the sessions of this user
            cachingSessionHandler.deleteUsersSessions(User.getById(userId));
        }
        return Results.redirect(context.getContextPath() + "/admin/users");
    }

    /**
     * Pro- or demotes the {@link models.User user} with the given ID.
     * 
     * @param userId
     *            ID of the user to pro/demote
     * @param context
     *            the context of this request
     * @return users overview page
     */
    public Result promoteUserProcess(@PathParam("id") Long userId, Context context)
    {
        User user = context.getAttribute("user", User.class);

        if (user.getId() != userId)
        { // the user to pro-/demote is not the user who performs this action
            User.promote(userId);
            // update all of the sessions
            cachingSessionHandler.updateUsersSessions(User.getById(userId));
        }
        return Results.redirect(context.getContextPath() + "/admin/users");
    }

    /**
     * Deletes the {@link models.User user} with the given ID.
     * 
     * @param deleteUserId
     *            the ID of the user to delete
     * @param context
     *            the context of this request
     * @return users overview page
     */
    public Result deleteUserProcess(@PathParam("id") Long deleteUserId, Context context)
    {
        User user = context.getAttribute("user", User.class);

        if (user.getId() != deleteUserId)
        { // the user to delete is not the user who performs this action
            cachingSessionHandler.deleteUsersSessions(User.getById(deleteUserId));
            User.delete(deleteUserId);
        }

        return Results.redirect(context.getContextPath() + "/admin/users");
    }

    /**
     * Searches for an {@link models.User user}.
     * 
     * @param context
     *            the context of this request
     * @return found users as JSON array
     */
    public Result jsonUserSearch(Context context)
    {
        List<User> userList;
        String searchString = context.getParameter("s", "");

        userList = (searchString.equals("")) ? new ArrayList<User>() : User.findUserLike(searchString);

        UserFormData userData;
        List<UserFormData> userDatalist = new ArrayList<UserFormData>();

        // GSON can't handle with cyclic references (the 1:m relation between user and MBox will end up in a cycle)
        // so we need to transform the data which does not contain the reference
        for (User currentUser : userList)
        {
            userData = UserFormData.prepopulate(currentUser);
            userDatalist.add(userData);
        }
        return Results.json().render(userDatalist);
    }

    /**
     * Shows a page that contains a list of all domains allowed for registration.
     * 
     * @param context
     *            the context of this request
     * @return overview of all white-listed domains
     */
    @FilterWith(WhitelistFilter.class)
    public Result showDomainWhitelist(Context context)
    {
        List<Domain> domainList = Domain.getAll();

        return Results.html().render("domains", domainList);
    }

    /**
     * Displays the Remove-Domain Page to decide whether the admin wants to delete all users to the requested domain or
     * just the domain itself.
     * 
     * @param context
     *            the context of this request
     * @param remDomainId
     *            the ID of the domain
     * @return the removeDomainConfirmation-Page
     */
    @FilterWith(WhitelistFilter.class)
    public Result callRemoveDomain(Context context, @Param("removeDomainsSelection") Long remDomainId)
    {
        Domain domain = Domain.getById(remDomainId);
        Result result = Results.html().template("/views/AdminHandler/removeDomainConfirmation.ftl.html");
        return result.render("domain", domain);
    }

    /**
     * Handles the action requested in the removeDomainConfirmation.
     * 
     * @param context
     *            the context of this request
     * @param action
     *            the action to do (abort, deleteUsersAndDomain or deleteDomain)
     * @param domainId
     *            the ID of the domain
     * @return overview of all white-listed domains
     */
    @FilterWith(WhitelistFilter.class)
    public Result handleRemoveDomain(Context context, @Param("action") String action, @Param("domainId") long domainId)
    {
        Result result = Results.redirect(context.getContextPath() + "/admin/whitelist");
        if (StringUtils.isBlank(action))
            return result;

        if (action.equals("deleteUsersAndDomain"))
        {
            Domain domain = Domain.getById(domainId);
            List<User> usersToDelete = User.getUsersOfDomain(domain.getDomainname());

            for (User userToDelete : usersToDelete)
            { // delete the sessions of the users and the account
                cachingSessionHandler.deleteUsersSessions(userToDelete);
                User.delete(userToDelete.getId());
            }
            domain.delete();
        }
        else if (action.equals("deleteDomain"))
        {// just delete the domain
            Domain.delete(domainId);
        }

        // if no action matches or the actions had been executed, redirect
        return result;
    }

    /**
     * Adds a domain to the white-list.
     * 
     * @param context
     *            the context of this request
     * @param domainName
     *            the name of the domain to add
     * @return overview of all white-listed domains
     */
    @FilterWith(WhitelistFilter.class)
    public Result addDomain(Context context, @Param("domainName") String domainName)
    {
        Result result = Results.redirect(context.getContextPath() + "/admin/whitelist");
        if (StringUtils.isBlank(domainName))
        {
            // the input-string was empty
            context.getFlashScope().error("adminAddDomain_Flash_EmptyField");
            return result;
        }

        if (!PATTERN_DOMAINS.matcher(domainName).matches())
        { // the validation of the domain-name failed
            context.getFlashScope().error("adminAddDomain_Flash_InvalidDomain");
            return result;
        }

        if (Domain.exists(domainName))
        { // the domain-name is already part of the domain-list
            context.getFlashScope().error("adminAddDomain_Flash_DomainExists");
            return result;
        }

        Domain domain = new Domain(domainName);
        domain.save();
        context.getFlashScope().success("adminAddDomain_Flash_Success");

        return result;
    }

    /**
     * Shows statistics about received emails
     * 
     * @param context
     *            the context of this request
     * @return
     */
    public Result showEmailStatistics(Context context)
    {
        List<Long> data = new LinkedList<>();
        List<Timestamp> timestamps = new LinkedList<>();

        SqlQuery query = Ebean.createSqlQuery("select parsedatetime(concat(t.DATE, ' ', t.QUARTER_HOUR / 4, ':', (t.QUARTER_HOUR % 4) * 15), 'yyyy-MM-dd HH:mm') as \"DATE\", t.DROPPED_MAIL_COUNT from V_EMAIL_STATISTICS_24 t");
        for (Iterator<SqlRow> iterator = query.findList().iterator(); iterator.hasNext();)
        {
            SqlRow sqlRow = iterator.next();
            timestamps.add(sqlRow.getTimestamp("DATE"));
            data.add(sqlRow.getLong("DROPPED_MAIL_COUNT"));
        }
        Result html = Results.html();
        html.render("summaryTimestamps", timestamps);
        html.render("summaryData", data);

        return html;
    }

}
