package org.opentripplanner.apis.transmodel.model.stop;

import graphql.Scalars;
import graphql.relay.Relay;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import java.util.Optional;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.transit.service.TransitService;

public class QuayAtDistanceType {

  private final FeedScopedIdMapper idMapper;

  public QuayAtDistanceType(FeedScopedIdMapper idMapper) {
    this.idMapper = idMapper;
  }

  public GraphQLObjectType createQD(GraphQLOutputType quayType, Relay relay) {
    return GraphQLObjectType.newObject()
      .name("QuayAtDistance")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("id")
          .type(new GraphQLNonNull(Scalars.GraphQLID))
          .dataFetcher(environment ->
            relay.toGlobalId(
              "QAD",
              Optional.ofNullable((NearbyStop) environment.getSource())
                .map(nearbyStop -> nearbyStop.distance + ";" + idMapper.mapToApi(nearbyStop.stopId))
                .orElse(null)
            )
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("quay")
          .type(quayType)
          .dataFetcher(environment ->
            getTransitService(environment).getStopLocation(
              ((NearbyStop) environment.getSource()).stopId
            )
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("distance")
          .type(Scalars.GraphQLFloat)
          .description("The distance in meters to the given quay.")
          .dataFetcher(environment -> ((NearbyStop) environment.getSource()).distance)
          .build()
      )
      .build();
  }

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<GraphQLRequestContext>getContext().transitService();
  }
}
