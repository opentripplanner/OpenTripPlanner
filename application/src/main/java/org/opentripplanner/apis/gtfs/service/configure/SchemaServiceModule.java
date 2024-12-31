package org.opentripplanner.apis.gtfs.service.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.apis.gtfs.SchemaFactory;
import org.opentripplanner.apis.gtfs.service.SchemaService;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.routing.api.request.RouteRequest;

/**
 * The service is used during application serve phase, not loading, and it depends on the default
 * route request, which is injected from the
 * {@link org.opentripplanner.standalone.config.RouterConfig}. The {@link SchemaService} is only
 * constructed if the API feature flag is on.
 */
@Module
public class SchemaServiceModule {

  @Provides
  @Singleton
  public SchemaService provideSchemaService(RouteRequest defaultRouteRequest) {
    return OTPFeature.GtfsGraphQlApi.isOn()
      ? new SchemaService(SchemaFactory.createSchema(defaultRouteRequest))
      : null;
  }
}
