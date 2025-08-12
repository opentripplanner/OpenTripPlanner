package org.opentripplanner.apis.transmodel.configure;

import dagger.Module;
import dagger.Provides;
import graphql.schema.GraphQLSchema;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.api.model.transit.HideFeedIdMapper;
import org.opentripplanner.api.model.transit.UseFeedIdMapper;
import org.opentripplanner.apis.transmodel.TransmodelGraphQLSchemaFactory;
import org.opentripplanner.apis.transmodel.mapping.FixedFeedIdGenerator;
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
    FeedScopedIdMapper idResolver;
    if (routerConfig.transmodelApi().hideFeedId()) {
      String fixedFeedId = FixedFeedIdGenerator.generate(timetableRepository.getAgencies());
      idResolver = new HideFeedIdMapper(fixedFeedId);
    } else {
      idResolver = new UseFeedIdMapper();
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
