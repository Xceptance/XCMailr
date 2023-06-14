/*
 * Copyright (c) 2013-2023 Xceptance Software Technologies GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package controllers;

import java.util.LinkedList;
import java.util.List;

import models.User;
import ninja.cache.NinjaCache;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import conf.XCMailrConf;

/**
 * Handles all actions that belong to the Caching-Server. This is almost a Wrapper-Class for the NinjaCache.
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
@Singleton
public class CachingSessionHandler
{

    @Inject
    XCMailrConf xcmConf;

    @Inject
    NinjaCache ninjaCache;

    @Inject
    Logger log;

    /**
     * Sets a new Object to the caching-server
     * 
     * @param key
     *            the Key to find the Object
     * @param timeToLive
     *            TimeToLive in Seconds
     * @param object
     *            the Object to store
     */
    void set(String key, int timeToLive, final Object object)
    {
        ninjaCache.safeSet(xcmConf.APP_NAME + key, object, timeToLive + "s");
    }

    void replace(String key, int timeToLive, final Object object)
    {
        ninjaCache.safeReplace(xcmConf.APP_NAME + key, object, timeToLive + "s");
    }

    /**
     * Sets the session to an user-mail in the caching-server
     * 
     * @param user
     *            the user-object
     * @param sessionId
     *            the session-id
     * @param timeToLive
     *            the lifetime of this object
     */
    public void setSessionUser(final User user, String sessionId, int timeToLive)
    {
        @SuppressWarnings("unchecked")
        List<String> sessions = (List<String>) get(user.getMail());
        // if there's no list, create a new one and add the session
        if (sessions == null)
        {
            sessions = new LinkedList<String>();
            // set it to the caching-server
            set(user.getMail(), timeToLive, sessions);
        }
        // if the session is not already stored in the list, add it
        if (!sessions.contains(sessionId))
        {
            sessions.add(sessionId);
            replace(user.getMail(), timeToLive, sessions);
        }
    }

    /**
     * deletes all session mappings and user-email mappings of this specified user
     * 
     * @param user
     *            the user object
     */
    public void deleteUsersSessions(final User user)
    {
        @SuppressWarnings("unchecked")
        // get the sessions of this user
        List<String> sessions = (List<String>) get(user.getMail());

        if (sessions != null)
        { // delete the sessionKeys of this user at caching-server
            for (String sessionKey : sessions)
            {
                delete(sessionKey);
            }
        }
        // delete the sessionlist of this user
        delete(user.getMail());
    }

    /**
     * Updates the user-object for all sessions of this user.
     * <p>
     * <b>WARNING:</b> if the email has been changed, use {@link #updateUsersSessions(User)} to change the user-session
     * mapping too
     * </p>
     * 
     * @param user
     *            the user-object to update
     */
    public void updateUsersSessions(final User user)
    {
        @SuppressWarnings("unchecked")
        // get the sessions of this user
        List<String> sessions = (List<String>) get(user.getMail());
        if (sessions == null)
            return;
        // update all sessions of this user at memCached
        for (String sessionKey : sessions)
        {
            // replace all user-objects for all sessions
            replace(sessionKey, xcmConf.COOKIE_EXPIRETIME, user);
        }

    }

    /**
     * Updates the user-session mapping entries if the email has changed
     * 
     * @param oldEmail
     *            the old email-address of the user
     * @param newEmail
     *            the new email-address of the user
     */
    public void updateUsersSessionsOnChangedMail(String oldEmail, String newEmail)
    {
        @SuppressWarnings("unchecked")
        // get the sessions of this user with the old address
        List<String> oldAddressSessions = (List<String>) get(oldEmail);

        // get the sessions of this user with the new address (if there are some existing)
        @SuppressWarnings("unchecked")
        List<String> newAddressSessions = (List<String>) get(newEmail);

        if (oldAddressSessions != null)
        { // there is at least one session with the old address
          // check if a session with the new address exists
            if (newAddressSessions != null)
            {
                // add the old session-ids to the new
                newAddressSessions.addAll(oldAddressSessions);
                // replace the existing new session-object with the updated one
                replace(newEmail, xcmConf.COOKIE_EXPIRETIME, newAddressSessions);

            }
            else
            { // no new session-> create a new mapping for the new address
                set(newEmail, xcmConf.COOKIE_EXPIRETIME, oldAddressSessions);
            }
            // delete the session entries for the old email
            delete(oldEmail);
        }
        else
        { // there's no session -> do nothing

        }
    }

    /**
     * Returns the object belonging to the given key or <code>null</code> if the key doesn't exist.
     * 
     * @param key
     *            the Key
     * @return the specified Object
     */
    public Object get(String key)
    {
        return ninjaCache.get(xcmConf.APP_NAME + key);
    }

    /**
     * Deletes the object to the given key.
     * 
     * @param key
     *            the Key
     */
    void delete(String key)
    {
        ninjaCache.safeDelete(xcmConf.APP_NAME + key);
    }
}
