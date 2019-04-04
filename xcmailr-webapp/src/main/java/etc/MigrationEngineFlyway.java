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

        // We migrate automatically => if you do not want that (eg in production)
        // set ninja.migration.run=false in application.conf
        Flyway flyway = new Flyway();
        flyway.setDataSource(connectionUrl, connectionUsername, connectionPassword);
        flyway.setBaselineOnMigrate(ninjaProperties.getBooleanWithDefault("ninja.migration.baselineOnMigrate", false));

        // In testmode we are cleaning the database so that subsequent testcases
        // get a fresh database.
        if (ninjaProperties.getBooleanWithDefault(NinjaConstant.NINJA_MIGRATION_DROP_SCHEMA, ninjaProperties.isTest()))
        {
            flyway.clean();
        }

        flyway.migrate();
    }

}
