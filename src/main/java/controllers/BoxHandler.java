package controllers;

import ninja.Context;
import ninja.FilterWith;
import ninja.Results;
import filters.SecureFilter;

import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.joda.time.DateTime;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import models.MBox;
import models.MbFrmDat;
import models.User;
import ninja.i18n.Lang;
import ninja.i18n.Messages;
import ninja.params.PathParam;
import ninja.validation.JSR303Validation;
import ninja.validation.Validation;
import ninja.Result;

/**
 * Handles all actions for the Mailboxes like add, delete and edit box
 * 
 * @author Patrick Thum 2012 released under Apache 2.0 License
 */

@FilterWith(SecureFilter.class)
public class BoxHandler
{

    @Inject
    Lang lang;

    @Inject
    Messages msg;

    public Result showAddBox()
    {

        return Results.html();
    }

    /**
     * Adds a Mailbox to the Useraccount POST of /mail/add
     * 
     * @return the Mailbox-Overviewpage
     */
    public Result addBox(Context context, @JSR303Validation MbFrmDat mbdat, Validation validation)
    {

        Long id = new Long(context.getSessionCookie().get("id"));

        Result result = Results.html();
        String s;

        if (validation.hasViolations())
        { // TODO prepopulate
            // not all fields were filled
            s = msg.get("msg_formerr", context, result, "String");
            context.getFlashCookie().error(s, (Object) null);
            return Results.redirect("/mail");
        }
        else
        {
            // checks whether the Box already exists
            if (!MBox.mailExists(mbdat.getAddress(), mbdat.getDomain()))
            {

                MBox mb = new MBox();
                String mbName = mbdat.getAddress().toLowerCase();
                // deletes all special characters
                // TODO return an error-page if there are some special-chars in the address...
                mbName = mbName.replaceAll("[^a-zA-Z0-9.]", "");
                // deletes a the dot if its placed at the end of the mailaddress
                // TODO return an error-page if there is a dot at the end
                // TODO there should be another mail-exists-check after all deletions..
                if (mbName.endsWith("."))
                {
                    mbName = mbName.substring(0, mbName.length() - 1);
                }

                // set the data of the box
                // TODO check whether the next cmd is redundant, maybe it can be removed?
                mb.setDomain(mbdat.getDomain());
                mb.setAddress(mbName);
                mb.setExpired(false);
                // TODO check the existence of the new domainname
                // TODO make the domainname dynamic
                mb.setDomain("test.ccmailr");

                Long ts = parseDuration(mbdat.getDuration());

                if (ts == -1)
                { // show an error-page if the timestamp is faulty
                    s = msg.get("msg_wrongf", context, result, "String");
                    context.getFlashCookie().put(s, (Object) null);
                    return Results.redirect("/mail");

                }
                // sets the activity-time of the mailbox
                mb.setTS_Active(ts);

                mb.setUsr(User.getById(id));

                String fwd = mb.getUsr().getMail();
                // creates the Box on the MailServer and sets the FWD
                // jmc.addUser(mb.getAdress(), mb.getDomain(), fwd);
                // creates the Box in the DB
                MBox.createMBox(mb);

                return Results.redirect("/mail");

            }
            else
            {
                // the mailbox already exists
                s = msg.get("msg_mailex", context, result, "String");
                context.getFlashCookie().put(s, (Object) null);
                return Results.redirect("/mail");
            }
        }
    }

    /**
     * Deletes a Box from Mailserver and DB
     * 
     * @param boxid
     *            the ID of the Mailbox
     * @return the Mailbox-Overviewpage
     */
    public Result deleteBox(@PathParam("id") Long boxid, Context context)
    {

        // TODO maybe we could save memory by using a Query instead of creating an Object..
        // deletes the box from mailserver
        MBox mb = MBox.getById(boxid);
        Long uid = Long.parseLong(context.getSessionCookie().get("id"));
        if (mb.belongsTo(uid))
        {
            // deletes the box from DB
            MBox.delete(boxid);
        }
        return Results.redirect("/mail");

    }

    /**
     * Edits a Mailbox POST /mail/edit/{id}
     * 
     * @param boxId
     * @return error/success-page
     */
    public Result editBox(Context context, @PathParam("id") Long boxId, @JSR303Validation MbFrmDat mbdat,
                          Validation validation)
    {
        Result result = Results.html();
        String s;

        if (validation.hasViolations())
        {
            // not all fields were filled
            s = msg.get("msg_formerr", context, result, "String");
            context.getFlashCookie().error(s, (Object) null);

            return Results.redirect("/mail/edit/" + boxId.toString()).render(mbdat);
        }
        else
        { // the form was filled correctly

            if (!MBox.mailExists(mbdat.getAddress(), mbdat.getDomain(), boxId))
            {
                // the given mailbox exists
                boolean changes = false;
                // we get the boxID with the POST-Request
                // TODO check if the user who sends the POST equals to the owner of the box
                MBox mb = MBox.getById(boxId);
                String newLName = mbdat.getAddress().toLowerCase();
                String newDName = mbdat.getDomain().toLowerCase();
                String oldLName = mb.getAddress();
                String oldDName = mb.getDomain();
                String fwd = mb.getUsr().getMail();

                if (!newLName.equals(oldLName))
                { // a new localname was chosen

                    // TODO return an error-page if there are some special-chars in the address...
                    // deletes a the dot if its placed at the end of the mailaddress
                    // TODO return an error-page if there is a dot at the end
                    // TODO there should be another mail-exists-check after all deletions..
                    newLName = newLName.replaceAll("[^a-zA-Z0-9.]", "");
                    if (newLName.endsWith("."))
                    {
                        newLName = newLName.substring(0, newLName.length() - 1);
                    }
                    mb.setAddress(newLName);
                    changes = true;
                }

                if (!newDName.equals(oldDName))
                { // a new domainname was chosen
                    // TODO check the existence of the new domainname
                    mb.setDomain(newDName);
                    changes = true;
                }

                Long ts = parseDuration(mbdat.getDuration());
                if (ts == -1)
                { // a faulty timestamp was given -> return an errorpage
                    s = msg.get("msg_wrongf", context, result, "String");
                    context.getFlashCookie().put(s, (Object) null);
                    return Results.redirect("/mail/edit");
                }

                if (!(mb.getTS_Active() == ts))
                {
                    // check if the MBox-TS is unequal to the given TS in the form
                    mb.setTS_Active(ts);
                    changes = true;
                }

                // Updates the Box if changes were made
                if (changes)
                {
                    mb.setExpired(false);
                    // TODO consider a failure in update-process
                    MBox.updateMBox(mb);
                }

                return Results.redirect("/mail");
            }
            else
            {
                // mailexists-check was false
                s = msg.get("msg_mailex", context, result, "String");
                context.getFlashCookie().put(s, (Object) null);
                return Results.redirect("/mail/edit");
            }
        }
    }

    /**
     * Shows the edit-form for the box with boxId. GET /mail/edit/:boxid
     * 
     * @param boxId
     *            ID of the Box
     * @return the edit-form
     */
    public Result showEditBox(Context context, @PathParam("id") Long boxId)
    {
        // TODO handle nullpointerEx if the box doesnt exist
        MbFrmDat mbdat = new MbFrmDat();
        MBox mb = MBox.getById(boxId);
        Long uid = Long.parseLong(context.getSessionCookie().get("id"));

        if (mb.belongsTo(uid))
        { // prevent the edit of a mbox that is not belonging to the user
            mbdat.setBoxId(boxId);
            mbdat.setAddress(mb.getAddress());
            mbdat.setDomain(mb.getDomain());
            mbdat.setDuration(parseTime(mb.getTS_Active()));

            return Results.html().render(mbdat);
        }
        else
        {
            return Results.redirect("/mail");
        }

    }

    /**
     * Generates the mailbox-overview-page of a user. with prepopulated values for the mail-address
     * 
     * @param context
     * @return the mailbox-overview-page
     */
    @SuppressWarnings("unchecked")
    public Result showBoxes(Context context)
    {

        Long id = new Long(context.getSessionCookie().get("id"));
        return Results.html().render(MBox.allUserMap(id));
    }

    /**
     * Generates a random name for the mailbox
     * 
     * @return a random name
     */
    private static String getRndName()
    {
        // TODO modify this function, also use at least digits and uppercase-letters and a variable length
        Random rand = new Random();
        StringBuffer strBuf = new StringBuffer();
        for (int i = 0; i < 7; i++)
        {
            // generates a random char between a and z (an ascii a is 97, z is 122 in dec)
            strBuf.append((char) ((Math.abs(rand.nextInt()) % 26) + 97));
        }
        return strBuf.toString();
    }

    /**
     * sets the box valid/invalid
     * 
     * @param id
     *            the ID of the mailbox
     * @return the rendered mailbox-overview-page
     */

    public Result expireBox(@PathParam("id") Long id, Context context)
    {

        // checks whether its valid or invalid at the DB
        MBox mb = MBox.getById(id);
        Long uid = Long.parseLong(context.getSessionCookie().get("id"));
        if (mb.belongsTo(uid))
        {// check if the mailbox belongs to the current user

            DateTime dt = new DateTime();
            if (!(mb.getTS_Active() == 0) && (mb.getTS_Active() < dt.getMillis()))
            {
                // if the validity period is over return the Edit page
                return Results.redirect("/mail/edit");
            }
            else
            {
                // otherwise just set the new status
                MBox.enable(id);
            }
        }
        return Results.redirect("/mail");
    }

    public long parseDuration(String s)
    {
        int d = 0;
        int h = 0;
        if (parseHelp2(s) >= 0)
        {
            // checks if the string is in the right format
            // there should be 1 or 2 values (d,h or h,d or h or d or 0)
            String[] str = s.split(",");
            String helper = "";

            for (int i = 0; i < str.length; i++)
            {
                str[i] = str[i].toLowerCase();

                if (str[i].contains("d"))
                {
                    helper = str[i].substring(0, str[i].indexOf('d'));
                    d = digitsOnly(helper);
                }
                else if (str[i].contains("h"))
                {
                    helper = str[i].substring(0, str[i].indexOf('h'));
                    h = digitsOnly(helper);
                }
            }
            if ((d == -1) || (h == -1))
            {
                return -1;
            }
            s = s.trim();
            if (s.equals("0"))
            {
                return 0;
            }
        }
        else
        {
            //
            return -1;
        }

        // everything is okay with the String
        // so return the milisecs
        if (h >= 24)
        {
            d += h / 24;
            h = h % 24;
        }
        if ((d > 30) || ((d == 30) && (h >= 0)))
        {
            // max 30days allowed, higher means unlimited
            return 0;
        }

        DateTime dt = new DateTime();
        return dt.plusDays(d).plusHours(h).getMillis();
    }

    /**
     * helper function for parseDuration() checks if a string consists only of digits
     * 
     * @param helper
     *            string to check
     * @return the integer value of the string or -1 if the string does not match
     */
    private int digitsOnly(String helper)
    {
        helper = helper.trim();
        if (helper.matches("[0-9]+"))
        {
            return Integer.parseInt(helper);

        }
        else
        {
            return -1;
        }

    }

    /**
     * @param helper
     * @return
     */
    private int parseHelp2(String helper)
    {
        helper = helper.trim();
        if (helper.matches("[\\d+][d|h][,][\\d][d|h]") || helper.matches("[\\d+][d|h]") || helper.matches("[0]"))
        {
            return 0;

        }
        else
        {
            return -1;
        }
    }

    private String parseTime(Long milis)
    {
        DateTime dt = new DateTime();
        float times = (milis - dt.getMillis()) / 3600000.0f; // in hours
        if (milis == 0)
        {
            // the box is "unlimited"
            return "0";
        }
        if (times < 0)
        {
            // the box is expired
            return "1h,1d";
        }

        int hours = Math.round(times);
        int days = hours / 24;
        hours = hours % 24;

        return hours + "h," + days + "d";
    }
}
