/**
 * Copyright (C) 2012 the original author or authors.
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

/**
 * All currently supported Ebean properties in file
 * application.conf.
 * 
 * Add them to your application.conf file to alter them.
 * 
 */
public interface NinjaEbeanProperties {
    
    /** 
     * Comma separated list of Ebean models which should be registered
     * at the EbeanServer.
     * 
     * Usually The NinjaCasino plugin registers all models it
     * can get from sub directory. But sometimes that is not enough
     * (especially when using external jars.). To
     * make that reliable you can use that property.
     * 
     * ebean.models=model.myModel1, model.myModel2
     * 
     * To register all classes in a package, simply append a ".*" at the end
     * of your model name.
     * 
     * ebean.models=model.MyModel1,com.company.models.*
     * 
     */
    public final String EBEAN_MODELS = "ebean.models";

    public final String EBEAN_DDL_GENERATE = "ebean.ddl.generate";
    public final String EBEAN_DDL_RUN = "ebean.ddl.run";

    public final String EBEAN_DDL_SEED_SQL = "ebean.ddl.seedSql";
    public final String EBEAN_DDL_INIT_SQL = "ebean.ddl.initSql";
     
    public final String EBEAN_DATASOURCE_USERNAME = "ebean.datasource.username";
    public final String EBEAN_DATASOURCE_PASSWORD = "ebean.datasource.password";
    
    public final String EBEAN_DATASOURCE_NAME = "ebean.datasource.name";
    public final String EBEAN_DATASOURCE_DATABASE_URL = "ebean.datasource.databaseUrl";
    public final String EBEAN_DATASOURCE_DATABASE_DRIVER = "ebean.datasource.databaseDriver";
    public final String EBEAN_DATASOURCE_MIN_CONNECTIONS = "ebean.datasource.minConnections";
    public final String EBEAN_DATASOURCE_MAX_CONNECTIONS = "ebean.datasource.maxConnections";
    public final String EBEAN_DATASOURCE_HEARTBEAT_SQL = "ebean.datasource.heartbeatsql";
    public final String EBEAN_DATASOURCE_ISOLATION_LEVEL = "ebean.datasource.isolationlevel";

    
}
