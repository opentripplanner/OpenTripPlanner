package org.opentripplanner.apis.transmodel.configure;

import dagger.Module;
import dagger.Provides;
import graphql.schema.GraphQLSchema;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.api.model.transit.DefaultFeedIdMapper;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.api.model.transit.HideFeedIdMapper;
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
    FeedScopedIdMapper feedIdMapper;
    if (routerConfig.transmodelApi().hideFeedId()) {
      String fixedFeedId = FixedFeedIdGenerator.generate(timetableRepository.getAgencies());
      feedIdMapper = new HideFeedIdMapper(fixedFeedId);
    } else {
      feedIdMapper = new DefaultFeedIdMapper();
    }

    TransmodelGraphQLSchemaFactory factory = new TransmodelGraphQLSchemaFactory(
      defaultRouteRequest,
      timetableRepository.getTimeZone(),
      routerConfig.transitTuningConfig(),
      feedIdMapper,
      routerConfig.server().apiDocumentationProfile()
    );

    return factory.create();
  }
}
