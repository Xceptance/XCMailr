package controllers;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.MemcachedClient;
import ninja.utils.NinjaProperties;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Handles all actions that belong to the memcached-server<br/>
 * This is almost a Wrapper-Class for the MemCachedClient
 * 
 * @author Patrick Thum
 */

@Singleton
public class MemCachedSessionHandler
{
    @Inject
    public NinjaProperties ninjaProp;

    //private final String NAMESPACE = ninjaProp.getWithDefault("application.name", "XCMAILR");
    private String NAMESPACE = "XCMAILR";

    public MemcachedClient client;

    @Inject
    public MemCachedSessionHandler()
    {
        try
        {
//            String memHost = ninjaProp.getWithDefault("memcached.host", "127.0.0.1");
//            String memPort = ninjaProp.getWithDefault("memcached.port", "11211");
            //client = new MemcachedClient(new BinaryConnectionFactory(), AddrUtil.getAddresses(memHost + ":" + memPort));
            client = new MemcachedClient(new BinaryConnectionFactory(), AddrUtil.getAddresses("127.0.0.1:11211"));
            // TODO no. of clients
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Sets a new Object in the Memcache
     * 
     * @param key
     *            - the key to find the object
     * @param ttl
     *            - TimeToLive in Seconds
     * @param o
     *            - the object to store
     */
    public void set(String key, int ttl, final Object o)
    {
        client.set(NAMESPACE + key, ttl, o);

    }

    /**
     * loads the object to the given key will return null if the key doesn't exist
     * 
     * @param key
     * @return the specified object
     */
    public Object get(String key)
    {
        Object o = getCache().get(NAMESPACE + key);
        return o;
    }

    /**
     * deletes the object to the given key
     * 
     * @param key
     * @return an OperationFuture<Boolean>
     */

    public Object delete(String key)
    {
        return getCache().delete(NAMESPACE + key);
    }

    /**
     * @return the client that handles the connection to the MemCachedServer
     */
    public MemcachedClient getCache()
    {
        return client;
    }
}
