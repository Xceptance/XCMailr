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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
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

    public MemcachedClient client;

    /**
     * Call this Method to initialize all Data from application.conf (if this wasn't called before, it will be invoked
     * by all other Operations in this class automatically)
     */
    public void create()
    {
        try
        {
            memHost = xcmConf.MC_HOST;
            memPort = xcmConf.MC_PORT;
            NAMESPACE = xcmConf.APP_NAME;
            client = new MemcachedClient(new BinaryConnectionFactory(), AddrUtil.getAddresses(memHost + ":" + memPort));
        }
        catch (Exception e)
        {
            log.error(e.getMessage());
            instantiated = false;
        }
        // indicates that the client was successfully instantiated
        instantiated = true;
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
        return client;
    }
}
