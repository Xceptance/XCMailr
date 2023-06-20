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

import static ninja.ebean.NinjaEbeanProperties.EBEAN_DATASOURCE_DATABASE_DRIVER;
import static ninja.ebean.NinjaEbeanProperties.EBEAN_DATASOURCE_DATABASE_URL;
import static ninja.ebean.NinjaEbeanProperties.EBEAN_DATASOURCE_HEARTBEAT_SQL;
import static ninja.ebean.NinjaEbeanProperties.EBEAN_DATASOURCE_MAX_CONNECTIONS;
import static ninja.ebean.NinjaEbeanProperties.EBEAN_DATASOURCE_MIN_CONNECTIONS;
import static ninja.ebean.NinjaEbeanProperties.EBEAN_DATASOURCE_NAME;
import static ninja.ebean.NinjaEbeanProperties.EBEAN_DATASOURCE_PASSWORD;
import static ninja.ebean.NinjaEbeanProperties.EBEAN_DATASOURCE_USERNAME;
import static ninja.ebean.NinjaEbeanProperties.EBEAN_DATASOURCE_CAPTURE_STACKTRACE;
import static ninja.ebean.NinjaEbeanProperties.EBEAN_DDL_GENERATE;
import static ninja.ebean.NinjaEbeanProperties.EBEAN_DDL_RUN;
import static ninja.ebean.NinjaEbeanProperties.EBEAN_DDL_INIT_SQL;
import static ninja.ebean.NinjaEbeanProperties.EBEAN_DDL_SEED_SQL;

import static ninja.ebean.NinjaEbeanProperties.EBEAN_MODELS;

import ninja.utils.NinjaProperties;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.config.DatabaseConfig;
import io.ebean.datasource.DataSourceConfig;

import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;

/**
 * This is an internal class of Ninja Ebeans support.
 * 
 * As end-user you should NOT use this method directly. Instead simply inject
 * Ebean's Database into the class you want to use. The interface Database is
 * configured by this class and provided by a provider.
 * 
 */
@Singleton
public class NinjaEbeanDatabaseLifecycle {

    private Database ebeanDatabase;

    private final NinjaProperties ninjaProperties;

    private final Logger logger;

    @Inject
    public NinjaEbeanDatabaseLifecycle(
            Logger logger,
            NinjaProperties ninjaProperties) {

        this.logger = logger;
        this.ninjaProperties = ninjaProperties;

        this.startServer();
    }

    /**
     * This method reads the configuration properties from
     * your application.conf file and configures Ebean accordingly.
     * 
     */
    public final void startServer(){
        logger.info("Starting Ebeans Module.");

        // /////////////////////////////////////////////////////////////////////
        // Setup basic parameters
        // /////////////////////////////////////////////////////////////////////
        boolean ebeanDdlGenerate = ninjaProperties.getBooleanWithDefault(
                EBEAN_DDL_GENERATE, false);
        boolean ebeanDdlRun = ninjaProperties.getBooleanWithDefault(
                EBEAN_DDL_RUN, false);

        String ebeanDdlInitSql = ninjaProperties.get(EBEAN_DDL_INIT_SQL);
        String ebeanDdlSeedSql = ninjaProperties.get(EBEAN_DDL_SEED_SQL);

        String ebeanDatasourceName = ninjaProperties.getWithDefault(
                EBEAN_DATASOURCE_NAME, "default");
        String ebeanDatasourceUserName = ninjaProperties.getWithDefault(
                EBEAN_DATASOURCE_USERNAME, "test");
        String ebeanDatasourcePassword = ninjaProperties.getWithDefault(
                EBEAN_DATASOURCE_PASSWORD, "test");

        String ebeanDatasourceDatabaseUrl = ninjaProperties.getWithDefault(
                EBEAN_DATASOURCE_DATABASE_URL,
                "jdbc:h2:mem:tests;DB_CLOSE_DELAY=-1");

        String ebeanDatasourceDatabaseDriver = ninjaProperties.getWithDefault(
                EBEAN_DATASOURCE_DATABASE_DRIVER, "org.h2.Driver");
        int ebeanDatasourceMinConnections = ninjaProperties
                .getIntegerWithDefault(EBEAN_DATASOURCE_MIN_CONNECTIONS, 1);
        int ebeanDatasourceMaxConnections = ninjaProperties
                .getIntegerWithDefault(EBEAN_DATASOURCE_MAX_CONNECTIONS, 25);

        String ebeanDatasourceHeartbeatSql = ninjaProperties.getWithDefault(
                EBEAN_DATASOURCE_HEARTBEAT_SQL, "select 1");
        boolean ebeanDatasourceCaptureStacktrace = ninjaProperties.getBooleanWithDefault(
                EBEAN_DATASOURCE_CAPTURE_STACKTRACE, false);

        final DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setName(ebeanDatasourceName);
        dbConfig.loadFromProperties();

        // Define DataSource parameters
        DataSourceConfig dataSourceConfig = new DataSourceConfig();
        dataSourceConfig.setDriver(ebeanDatasourceDatabaseDriver);
        dataSourceConfig.setUsername(ebeanDatasourceUserName);
        dataSourceConfig.setPassword(ebeanDatasourcePassword);
        dataSourceConfig.setUrl(ebeanDatasourceDatabaseUrl);
        dataSourceConfig.setMinConnections(ebeanDatasourceMinConnections);
        dataSourceConfig.setMaxConnections(ebeanDatasourceMaxConnections);
        dataSourceConfig.setHeartbeatSql(ebeanDatasourceHeartbeatSql);
        dataSourceConfig.setCaptureStackTrace(ebeanDatasourceCaptureStacktrace);

        dbConfig.setDataSourceConfig(dataSourceConfig);

        // set DDL options...
        dbConfig.setDdlGenerate(ebeanDdlGenerate);
        dbConfig.setDdlRun(ebeanDdlRun);
        dbConfig.setDdlInitSql(ebeanDdlInitSql);
        dbConfig.setDdlSeedSql(ebeanDdlSeedSql);

        dbConfig.setDefaultServer(true);
        dbConfig.setRegister(true);

        // split models configuration into classes & packages
        Set<String> packageNames = new LinkedHashSet<>();
        Set<Class<?>> entityClasses = new LinkedHashSet<>();

        // models always added by default
        packageNames.add("models");

        // add manually listed classes from the property
        String[] manuallyListedModels = ninjaProperties.getStringArray(EBEAN_MODELS);

        if (manuallyListedModels != null) {
            for (String model: manuallyListedModels) {
                if (model.endsWith(".*")) {
                    // strip off .* at end
                    String packageName = model.substring(0, model.length() - 2);
                    packageNames.add(packageName);
                } else {
                    try {
                        entityClasses.add(Class.forName(model));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(
                            "Configuration error. Class listed/discovered via " + EBEAN_MODELS + " not found: " + model);
                    }
                }
            }
        }

        // if any packages were specified the reflections library MUST be available
        if (!packageNames.isEmpty()) {
            for (String packageName : packageNames) {
                dbConfig.addPackage(packageName);
            }
        }

        for (Class<?> entityClass : entityClasses) {
            dbConfig.addClass(entityClass);
        }

        // create the database instance
        this.ebeanDatabase = this.createEbeanDatabase(dbConfig);
    }

    /**
     * Creates the Ebean database with the prepared database config.  Provides a
     * last chance to modify the config in a subclass if you'd like to customize
     * the config further.
     * 
     * @param databaseConfig The prepared database config
     * @return The newly created Ebean database
     */
    public Database createEbeanDatabase(final DatabaseConfig databaseConfig) {
        return DatabaseFactory.create(databaseConfig);
    }

    public Database getEbeanDatabase() {
        return ebeanDatabase;
    }

}
