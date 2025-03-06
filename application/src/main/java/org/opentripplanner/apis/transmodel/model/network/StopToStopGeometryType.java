package org.opentripplanner.apis.transmodel.model.network;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import org.opentripplanner.apis.transmodel.model.framework.EncodedPolylineBeanWithStops;

public class StopToStopGeometryType {

  public static GraphQLObjectType create(
    GraphQLOutputType linkGeometryType,
    GraphQLOutputType quayType
  ) {
    return GraphQLObjectType.newObject()
      .name("StopToStopGeometry")
      .description("List of coordinates between two stops as a polyline")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("pointsOnLink")
          .description(
            "A list of coordinates encoded as a polyline string between two stops (see http://code.google.com/apis/maps/documentation/polylinealgorithm.html)"
          )
          .type(linkGeometryType)
          .dataFetcher(env -> ((EncodedPolylineBeanWithStops) env.getSource()).pointsOnLink())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("fromQuay")
          .description("Origin Quay")
          .type(quayType)
          .dataFetcher(env -> ((EncodedPolylineBeanWithStops) env.getSource()).fromQuay())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("toQuay")
          .description("Destination Quay")
          .type(quayType)
          .dataFetcher(env -> ((EncodedPolylineBeanWithStops) env.getSource()).toQuay())
          .build()
      )
      .build();
  }
}
