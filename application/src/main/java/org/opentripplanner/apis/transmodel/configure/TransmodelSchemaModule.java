package org.opentripplanner.apis.transmodel.configure;

import dagger.Module;
import dagger.Provides;
import graphql.schema.GraphQLSchema;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.apis.transmodel.TransmodelGraphQLSchemaFactory;
import org.opentripplanner.apis.transmodel.mapping.FixedFeedIdGenerator;
import org.opentripplanner.ext.trias.id.HideFeedIdResolver;
import org.opentripplanner.ext.trias.id.IdResolver;
import org.opentripplanner.ext.trias.id.UseFeedIdResolver;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.standalone.config.RouterConfig;
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
    RouterConfig routerConfig
  ) {
    IdResolver idResolver;
    if (routerConfig.transmodelApi().hideFeedId()) {
      String fixedFeedId = FixedFeedIdGenerator.generate(timetableRepository.getAgencies());
      idResolver = new HideFeedIdResolver(fixedFeedId);
    } else {
      idResolver = new UseFeedIdResolver();
    }

    TransmodelGraphQLSchemaFactory factory = new TransmodelGraphQLSchemaFactory(
      defaultRouteRequest,
      timetableRepository.getTimeZone(),
      routerConfig.transitTuningConfig(),
      idResolver,
      routerConfig.server().apiDocumentationProfile()
    );

    return factory.create();
  }
}
