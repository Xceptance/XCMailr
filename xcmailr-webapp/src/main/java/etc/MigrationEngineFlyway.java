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
package etc;

import org.flywaydb.core.Flyway;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import ninja.ebean.NinjaEbeanProperties;
import ninja.migrations.MigrationEngine;
import ninja.utils.NinjaConstant;
import ninja.utils.NinjaProperties;

/**
 * Code taken from {@link ninja.migrations.flyway.MigrationEngineFlyway}.
 * <p>
 * Customized to use EBean property values and to enable automatic baseline of DB.
 */
@Singleton
public class MigrationEngineFlyway implements MigrationEngine
{
    private final NinjaProperties ninjaProperties;

    @Inject
    public MigrationEngineFlyway(NinjaProperties ninjaProperties)
    {
        this.ninjaProperties = ninjaProperties;
    }

    @Override
    public void migrate()
    {
        // Get the connection credentials from application.conf
        String connectionUrl = ninjaProperties.getOrDie(NinjaEbeanProperties.EBEAN_DATASOURCE_DATABASE_URL);
        String connectionUsername = ninjaProperties.getOrDie(NinjaEbeanProperties.EBEAN_DATASOURCE_USERNAME);
        String connectionPassword = ninjaProperties.getOrDie(NinjaEbeanProperties.EBEAN_DATASOURCE_PASSWORD);

        boolean baselineOnMigrate = ninjaProperties.getBooleanWithDefault("ninja.migration.baselineOnMigrate", false);

        // We migrate automatically => if you do not want that (eg in production)
        // set ninja.migration.run=false in application.conf
        Flyway flyway = Flyway.configure().dataSource(connectionUrl, connectionUsername, connectionPassword)
                              .baselineOnMigrate(baselineOnMigrate).load();

        // In testmode we are cleaning the database so that subsequent testcases
        // get a fresh database.
        if (ninjaProperties.getBooleanWithDefault(NinjaConstant.NINJA_MIGRATION_DROP_SCHEMA, ninjaProperties.isTest()))
        {
            flyway.clean();
        }

        flyway.migrate();
    }

}
