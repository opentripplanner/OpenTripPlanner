package org.opentripplanner.apis.gtfs.configure;

import dagger.Module;
import dagger.Provides;
import graphql.schema.GraphQLSchema;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.SchemaFactory;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.routing.api.request.RouteRequest;

/**
 * The schema is used during application serve phase, not loading, and it depends on the default
 * route request, which is injected from the
 * {@link org.opentripplanner.standalone.config.RouterConfig}. The {@link GraphQLSchema} is only
 * constructed if the API feature flag is on.
 */
@Module
public class SchemaModule {

  @Provides
  @Singleton
  @Nullable
  public GraphQLSchema provideSchema(RouteRequest defaultRouteRequest) {
    return OTPFeature.GtfsGraphQlApi.isOn()
      ? SchemaFactory.createSchemaWithDefaultInjection(defaultRouteRequest)
      : null;
  }
}
