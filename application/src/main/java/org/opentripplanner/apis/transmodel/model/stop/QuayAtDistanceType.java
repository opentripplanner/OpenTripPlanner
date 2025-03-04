package org.opentripplanner.apis.transmodel.model.stop;

import graphql.Scalars;
import graphql.relay.Relay;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import org.opentripplanner.apis.transmodel.mapping.TransitIdMapper;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;

public class QuayAtDistanceType {

  public static GraphQLObjectType createQD(GraphQLOutputType quayType, Relay relay) {
    return GraphQLObjectType.newObject()
      .name("QuayAtDistance")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("id")
          .type(new GraphQLNonNull(Scalars.GraphQLID))
          .dataFetcher(environment ->
            relay.toGlobalId(
              "QAD",
              ((NearbyStop) environment.getSource()).distance +
              ";" +
              TransitIdMapper.mapEntityIDToApi(
                (AbstractTransitEntity) ((NearbyStop) environment.getSource()).stop
              )
            )
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("quay")
          .type(quayType)
          .dataFetcher(environment -> ((NearbyStop) environment.getSource()).stop)
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
}
