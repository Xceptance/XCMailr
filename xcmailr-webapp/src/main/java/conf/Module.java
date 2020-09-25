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

import com.google.inject.AbstractModule;

import controllers.CheckDBForMailAddressDuplicates;
import controllers.DeleteExpiredMailBoxes;
import controllers.JobController;
import ninja.ebean.NinjaEbeanModule2;

public class Module extends AbstractModule
{

    public Module()
    {
        super();
    }

    @Override
    protected void configure()
    {
        // install the ebean module
        install(new NinjaEbeanModule2());
        // bind configuration-class and jobcontroller
        bind(XCMailrConf.class);
        bind(JobController.class);

        // bind jobs
        bind(CheckDBForMailAddressDuplicates.class);
        bind(DeleteExpiredMailBoxes.class);
    }

}
