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
package ninja.ebean;

import org.slf4j.Logger;

import io.ebean.EbeanServer;
import io.ebean.config.ServerConfig;
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
