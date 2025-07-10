package org.opentripplanner.apis.transmodel.configure;

import dagger.Module;
import dagger.Provides;
import graphql.schema.GraphQLSchema;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.apis.transmodel.TransmodelGraphQLSchema;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.config.routerconfig.TransitRoutingConfig;
import org.opentripplanner.transit.service.TimetableRepository;

@Module
public class TransmodelSchemaModule {

  @Provides
  @Singleton
  @Nullable
  @TransmodelSchema
  public GraphQLSchema provideTransmodelSchema(
    RouteRequest defaultRouteRequest,
    TimetableRepository timetableRepository,
    TransitRoutingConfig transitRoutingConfig,
    RouterConfig routerConfig
  ) {
    return TransmodelGraphQLSchema.create(
      defaultRouteRequest,
      timetableRepository.getTimeZone(),
      routerConfig.server().apiDocumentationProfile(),
      transitRoutingConfig
    );
  }

}
