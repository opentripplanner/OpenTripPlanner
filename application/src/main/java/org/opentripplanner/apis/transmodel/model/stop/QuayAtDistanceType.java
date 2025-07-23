package org.opentripplanner.apis.transmodel.model.stop;

import graphql.Scalars;
import graphql.relay.Relay;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import java.util.Optional;
import org.opentripplanner.ext.trias.id.IdResolver;
import org.opentripplanner.routing.graphfinder.NearbyStop;

public class QuayAtDistanceType {

  private final IdResolver idResolver;

  public QuayAtDistanceType(IdResolver idResolver) {
    this.idResolver = idResolver;
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
                .map(
                  nearbyStop ->
                    nearbyStop.distance + ";" + idResolver.toString(nearbyStop.stop.getId())
                )
                .orElse(null)
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
