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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;

import models.User;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.MemcachedClient;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import conf.XCMailrConf;

/**
 * Handles all actions that belong to the Memcached-Server<br/>
 * This is almost a Wrapper-Class for the MemCached-Client
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */

@Singleton
public class MemCachedSessionHandler
{

    @Inject
    XCMailrConf xcmConf;

    @Inject
    Logger log;

    private String memHost;

    private int memPort;

    private String NAMESPACE = "";

    private boolean instantiated;

    private MemcachedClient client;

    /**
     * Call this Method to initialize all Data from application.conf (if this wasn't called before, it will be invoked
     * by all other Operations in this class automatically)
     */
    public void create()
    {
        try
        {
            memHost = xcmConf.MEMCA_HOST;
            memPort = xcmConf.MEMCA_PORT;
            NAMESPACE = xcmConf.APP_NAME;
            this.client = new MemcachedClient(new BinaryConnectionFactory(), AddrUtil.getAddresses(memHost + ":"
                                                                                                   + memPort));
            // indicates that the client was successfully instantiated

            instantiated = true;
        }
        catch (Exception e)
        {
            log.error(e.getMessage());
            instantiated = false;
        }

    }

    /**
     * Sets a new Object in the Memcache
     * 
     * @param key
     *            the Key to find the Object
     * @param timeToLive
     *            TimeToLive in Seconds
     * @param object
     *            the Object to store
     */
    public void set(String key, int timeToLive, final Object object)
    {
        getCache().set(NAMESPACE + key, timeToLive, object);

    }

    // TODO DOC
    public void setSessionUser(final User user, String sessionId, int timeToLive)
    {

        @SuppressWarnings("unchecked")
        List<String> sessions = (List<String>) getCache().get(user.getMail());
        // if there's no list, create a new one and add the session
        if (sessions == null)
        {
            sessions = new LinkedList<String>();
        }
        // if the session is not already stored in the list, add it
        if (!sessions.contains(sessionId))
        {
            sessions.add(sessionId);
        }

        set(user.getMail(), timeToLive, sessions);

    }

    public void deleteUsersSessions(final User user)
    {
        @SuppressWarnings("unchecked")
        // get the sessions of this user
        List<String> sessions = (List<String>) get(user.getMail());

        if (sessions != null)
        { // delete the sessionKeys of this user at memCached
            for (String sessionKey : sessions)
            {
                delete(sessionKey);
            }
        }
        // delete the sessionlist of this user
        delete(user.getMail());
    }

    /**
     * Updates the user-object for all sessions of this user <br/>
     * <b>WARNING:</b> if the email has been changed, use {@link #updateUsersSessions(User)} to change the
     * user-mail->session mapping
     * 
     * @param user
     *            the user-object to update
     */
    public void updateUsersSessions(final User user)
    {
        @SuppressWarnings("unchecked")
        // get the sessions of this user
        List<String> sessions = (List<String>) get(user.getMail());
        System.out.println("usradmin: "+user.isAdmin());
        if (sessions != null)
        { // update all sessions of this user at memCached
            for (String sessionKey : sessions)
            {
                set(sessionKey, xcmConf.COOKIE_EXPIRETIME, user);
            }
        }
    }

    /**
     * updates the user-mail -> session mapping-entries if the email has changed
     * 
     * @param oldEmail
     *            the old email-address of the user
     * @param newEmail
     *            the new email-address of the user
     */

    public void updateUsersSessionsOnChangedMail(String oldEmail, String newEmail)
    {
        @SuppressWarnings("unchecked")
        // get the sessions of this user
        List<String> sessions = (List<String>) get(oldEmail);
        @SuppressWarnings("unchecked")
        List<String> sessionsNew = (List<String>) get(newEmail);

        if (sessions != null)
        { // there is a session
          // check if such a session exists
            if (sessionsNew != null)
            {
                // add the old sessions to the old
                sessionsNew.addAll(sessions);
                set(newEmail, xcmConf.COOKIE_EXPIRETIME, sessionsNew);
            }
            else
            { // create a new mapping for the new address
                set(newEmail, xcmConf.COOKIE_EXPIRETIME, sessions);
            }
        }
        else
        { // theres no session -> do nothing

        }

    }

    /**
     * loads the Object belonging to the given Key <br/>
     * it will return null if the Key doesn't exist
     * 
     * @param key
     *            the Key
     * @return the specified Object
     */
    public Object get(String key)
    {
        Object object = null;
        Future<Object> futureObject = getCache().asyncGet(NAMESPACE + key);
        try
        {
            object = futureObject.get(5, TimeUnit.SECONDS);
        }
        catch (TimeoutException e)
        {
            futureObject.cancel(false);
        }
        catch (InterruptedException e)
        {
            futureObject.cancel(false);
        }
        catch (ExecutionException e)
        {
            futureObject.cancel(false);
        }
        return object;
    }

    /**
     * deletes the object to the given key
     * 
     * @param key
     *            the Key
     * @return an OperationFuture<Boolean>
     */

    public Object delete(String key)
    {
        return getCache().delete(NAMESPACE + key);
    }

    /**
     * @return the Client that handles the Connection to the MemCached-Server
     */
    public MemcachedClient getCache()
    {
        if (!instantiated)
        {
            create();
        }
        return this.client;
    }
}
