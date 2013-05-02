package controllers;

import java.util.Arrays;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import ninja.Context;
import ninja.FilterWith;
import ninja.Results;
import etc.HelperUtils;
import filters.SecureFilter;

import org.joda.time.DateTime;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import models.MBox;
import models.MbFrmDat;
import models.User;
import ninja.i18n.Messages;
import ninja.params.PathParam;
import ninja.utils.NinjaProperties;
import ninja.validation.JSR303Validation;
import ninja.validation.Validation;
import ninja.Result;

/**
 * Handles all actions for the Mailboxes like add, delete and edit box
 * 
 * @author Patrick Thum 2012 released under Apache 2.0 License
 */

@FilterWith(SecureFilter.class)
@Singleton
public class BoxHandler
{
    @Inject
    MemCachedSessionHandler mcsh;

    @Inject
    Messages msg;

    @Inject
    NinjaProperties ninjaProp;

    /**
     * Shows the "New Mailforward"-Page <br/>
     * GET /mail/add
     * 
     * @param context
     * @return
     */
    public Result showAddBox(Context context)
    {
        Map<String, Object> map = HelperUtils.getDomainsFromConfig(ninjaProp);
        MbFrmDat mbdat = new MbFrmDat();
        // set the value of the random-name to 7
        // use the lowercase, we handle the address as case-insensitive
        String name = HelperUtils.getRndString(7).toLowerCase();
        mbdat.setAddress(name);
        // set a default value
        mbdat.setDuration("1h,1d");

        // check that the generated mailname-proposal does not exist
        String[] domains = (String[]) map.get("domain");
        if (domains.length > 0)
        {// prevent OutOfBoundException
            while (MBox.mailExists(name, domains[0]))
            {
                name = HelperUtils.getRndString(7).toLowerCase();
            }
        }
        map.put("mbFrmDat", mbdat);

        return Results.html().render(map);
    }

    /**
     * Adds a Mailbox to the Useraccount <br/>
     * POST of /mail/add
     * 
     * @param context
     * @param mbdat
     * @param validation
     * @return
     */
    public Result addBox(Context context, @JSR303Validation MbFrmDat mbdat, Validation validation, HttpServletRequest req)
    {

        // Long id = new Long(context.getSessionCookie().get("id"));
        Map<String, Object> map = HelperUtils.getDomainsFromConfig(ninjaProp);
        map = HelperUtils.getDomainsFromConfig(ninjaProp);
        Result result = Results.html();
        String s;

        if (validation.hasViolations())
        { // not all fields were filled (correctly)
            s = msg.get("i18nmsg_formerr", context, result, (Object) null);
            context.getFlashCookie().error(s, (Object) null);
            map.put("mbFrmDat", mbdat);

            return Results.html().template("views/BoxHandler/showAddBox.ftl.html").render(map);
        }
        else
        {
            // checks whether the Box already exists
            if (!MBox.mailExists(mbdat.getAddress(), mbdat.getDomain()))
            {
                String mbName = mbdat.getAddress().toLowerCase();

                // set the data of the box
                String[] dom = (String[]) HelperUtils.getDomainsFromConfig(ninjaProp).get("domain");
                if (!Arrays.asList(dom).contains(mbdat.getDomain()))
                { // the new domainname does not exist in the application.conf
                  // stop the process and return to the mailbox-overview page
                    return Results.redirect("/mail");
                }
                Long ts = HelperUtils.parseDuration(mbdat.getDuration());
                if (ts == -1)
                { // show an error-page if the timestamp is faulty
                    s = msg.get("i18nmsg_wrongf", context, result, (Object) null);
                    context.getFlashCookie().error(s, (Object) null);
                    map.put("mbFrmDat", mbdat);

                    return Results.html().template("views/BoxHandler/showAddBox.ftl.html").render(map);
                }
                // create the MBox
                User usr = (User) mcsh.get(req.getSession().getId());
                MBox mb = new MBox(mbName, mbdat.getDomain(), ts, false, false, usr);

                // creates the Box in the DB
                mb.save();

                return Results.redirect("/mail");
            }
            else
            {
                // the mailbox already exists
                s = msg.get("i18nmsg_mailex", context, result, (Object) null);
                context.getFlashCookie().error(s, (Object) null);
                map.put("mbFrmDat", mbdat);

                return Results.html().template("views/BoxHandler/showAddBox.ftl.html").render(map);
            }
        }
    }

    /**
     * Deletes a Box from the DB
     * 
     * @param boxid
     *            the ID of the Mailbox
     * @return the Mailbox-Overviewpage
     */
    public Result deleteBox(@PathParam("id") Long boxid, Context context, HttpServletRequest req)
    {
        User usr = (User) mcsh.get(req.getSession().getId());
        
        if (MBox.boxToUser(boxid, usr.getId()))
        {
            // deletes the box from DB
            MBox.delete(boxid);
        }
        return Results.redirect("/mail");

    }

    /**
     * Edits a Mailbox <br/>
     * POST /mail/edit/{id}
     * 
     * @param boxId
     * @return error/success-page
     */
    public Result editBox(Context context, @PathParam("id") Long boxId, @JSR303Validation MbFrmDat mbdat,
                          Validation validation, HttpServletRequest req)
    {
        Result result = Results.html();
        String s;

        if (validation.hasViolations())
        { // not all fields were filled
            s = msg.get("i18nmsg_formerr", context, result, (Object) null);
            context.getFlashCookie().error(s, (Object) null);

            return Results.redirect("/mail/edit/" + boxId.toString()).render(mbdat);
        }
        else
        { // the form was filled correctly

            // we got the boxID with the POST-Request
            MBox mb = MBox.getById(boxId);
            if (!(mb == null))
            { // the box with the given id exists
                User usr = (User) mcsh.get(req.getSession().getId());
                

                if (mb.belongsTo(usr.getId()))
                { // the current user is the owner of the mailbox
                    boolean changes = false;
                    String newLName = mbdat.getAddress().toLowerCase();
                    String newDName = mbdat.getDomain().toLowerCase();

                    if (MBox.mailChanged(newLName, newDName, boxId))
                    { // this is only true when the address changed and the new address does not exist

                        String[] dom = (String[]) HelperUtils.getDomainsFromConfig(ninjaProp).get("domain");
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
                    Long ts = HelperUtils.parseDuration(mbdat.getDuration());
                    if (ts == -1)
                    { // a faulty timestamp was given -> return an errorpage
                        s = msg.get("i18nmsg_wrongf", context, result, (Object) null);
                        context.getFlashCookie().error(s, (Object) null);

                        return Results.redirect("/mail/edit/" + boxId.toString());
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
                else
                { // the current user is not the owner of the mailbox
                    return Results.redirect("/mail");
                }
            }
            else
            { // the given id does not exist
                return Results.redirect("/mail");
            }
            return Results.redirect("/mail");

        }
    }

    /**
     * Shows the edit-form for the box with boxId. GET /mail/edit/:boxid
     * 
     * @param boxId
     *            ID of the Box
     * @return the edit-form
     */
    public Result showEditBox(Context context, @PathParam("id") Long boxId, HttpServletRequest req)
    {
        MBox mb = MBox.getById(boxId);
        if (mb.equals(null))
        { // there's no box with that id
            return Results.redirect("/mail");
        }
        else
        { // the box exists, go on!
            User usr = (User) mcsh.get(req.getSession().getId());
            if (mb.belongsTo(usr.getId()))
            { // prevent the edit of a mbox that is not belonging to the user
                MbFrmDat mbdat = new MbFrmDat();
                mbdat.setBoxId(boxId);
                mbdat.setAddress(mb.getAddress());
                mbdat.setDomain(mb.getDomain());
                mbdat.setDuration(HelperUtils.parseTime(mb.getTs_Active()));
                Map<String, Object> map = HelperUtils.getDomainsFromConfig(ninjaProp);
                map.put("mbFrmDat", mbdat);

                return Results.html().render(map);
            }
            else
            {
                return Results.redirect("/mail");
            }
        }
    }

    /**
     * Generates the mailbox-overview-page of a user.
     * 
     * @param context
     * @return the mailbox-overview-page
     */

    public Result showBoxes(Context context, HttpServletRequest req)
    {
        
        User usr = (User) mcsh.get(req.getSession().getId());
        return Results.html().render(MBox.allUserMap(usr.getId()));
    }

    /**
     * sets the box valid/invalid
     * 
     * @param id
     *            the ID of the mailbox
     * @return the rendered mailbox-overview-page
     */

    public Result expireBox(@PathParam("id") Long id, Context context, HttpServletRequest req)
    {
        MBox mb = MBox.getById(id);
        User usr = (User) mcsh.get(req.getSession().getId());
        if (mb.belongsTo(usr.getId()))
        {// check if the mailbox belongs to the current user
            DateTime dt = new DateTime();
            if (!(mb.getTs_Active() == 0) && (mb.getTs_Active() < dt.getMillis()))
            {
                // if the validity period is over return the Edit page
                return Results.redirect("/mail/edit/" + id);
            }
            else
            {
                // otherwise just set the new status
                mb.enable();
            }
        }
        return Results.redirect("/mail");
    }
}
