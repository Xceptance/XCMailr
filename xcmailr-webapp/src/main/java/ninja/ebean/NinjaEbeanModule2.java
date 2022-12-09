package ninja.ebean;

import io.ebean.EbeanServer;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class NinjaEbeanModule2 extends AbstractModule
{
    @Override
    protected void configure()
    {
        // nothing to bind...
    }

    @Provides
    @Singleton
    EbeanServer provideEbeanServer(NinjaEbeanServerLifecycle2 ninjaEbeanServerLifecycle)
    {
        return ninjaEbeanServerLifecycle.getEbeanServer();
    }

}
