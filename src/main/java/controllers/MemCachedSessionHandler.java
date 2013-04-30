package controllers;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.MemcachedClient;
import ninja.utils.NinjaProperties;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MemCachedSessionHandler
{
    @Inject
    public NinjaProperties ninjaProp;

    private final String NAMESPACE = "XCMAILR";

    public MemcachedClient client;

    @Inject
    public MemCachedSessionHandler()
    {
        try
        {
             client = new MemcachedClient(new BinaryConnectionFactory(),
                                                         AddrUtil.getAddresses("127.0.0.1:11211"));
            // mclients[0]=client;
            // TODO make namespace variable, make address variable and the no of clients too
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void set(String key, int ttl, final Object o)
    {
        client.set(NAMESPACE + key, ttl, o);
//        MemcachedClient mcl = getCache();
        // mcl.set(NAMESPACE + key, ttl, o);

    }
    

    public Object get(String key)
    {
        Object o = getCache().get(NAMESPACE + key);
        return o;
    }

    public Object delete(String key)
    {
        return getCache().delete(NAMESPACE + key);
    }

    public MemcachedClient getCache()
    {
        return client;
    }
}
