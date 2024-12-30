package org.opentripplanner.apis.gtfs.service.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.apis.gtfs.SchemaFactory;
import org.opentripplanner.apis.gtfs.service.SchemaService;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;

/**
 * The service is used during application serve phase, not loading, and it depends on the default
 * routing preferences, which are injected from the
 * {@link org.opentripplanner.standalone.config.RouterConfig}.
 */
@Module
public class SchemaServiceModule {

  @Provides
  @Singleton
  public SchemaService provideSchemaService(RoutingPreferences defaultPreferences) {
    return new SchemaService(SchemaFactory.createSchema(defaultPreferences));
  }
}
