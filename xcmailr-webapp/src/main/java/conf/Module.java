/**
 * Copyright (C) 2013 the original author or authors.
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

package conf;

import org.slf4j.bridge.SLF4JBridgeHandler;

import com.google.inject.AbstractModule;

import ninja.ebean.NinjaEbeanModule2;
import services.CheckDBForMailAddressDuplicates;
import services.MailService;

public class Module extends AbstractModule
{
    @Override
    protected void configure()
    {
        // install jul-to-SLF4j Bridge
        install(new JulToSlf4jModule());
        // install the ebean module
        install(new NinjaEbeanModule2());
        // bind Jackson setup service
        bind(JacksonSetup.class);
        // bind configuration-class
        bind(XCMailrConf.class);

        // bind services and jobs
        bind(MailService.class);
        bind(CheckDBForMailAddressDuplicates.class);
    }

    private static class JulToSlf4jModule extends AbstractModule
    {
        @Override
        protected void configure()
        {
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();
        }
    }
}
