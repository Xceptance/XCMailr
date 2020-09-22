package ninja.ebean;

import org.slf4j.Logger;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.config.ServerConfig;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import ninja.utils.NinjaProperties;

@Singleton
public class NinjaEbeanServerLifecycle2 extends NinjaEbeanServerLifecycle
{
    private static boolean captureStackTrace;
    
    @Inject
    public NinjaEbeanServerLifecycle2(Logger logger, NinjaProperties ninjaProperties)
    {
        super(logger, configure(ninjaProperties));
    }

    private static NinjaProperties configure(final NinjaProperties ninjaProperties)
    {
        captureStackTrace = ninjaProperties.getBooleanWithDefault("ebean.datasource.captureStackTrace", false);
        return ninjaProperties;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public EbeanServer createEbeanServer(ServerConfig serverConfig)
    {
        serverConfig.getDataSourceConfig().setCaptureStackTrace(captureStackTrace);

        return super.createEbeanServer(serverConfig);
    }
}
