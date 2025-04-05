package org.opentripplanner.apis.transmodel.model.framework;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.framework.geometry.EncodedPolyline;

public class PointsOnLinkType {

  public static GraphQLObjectType create() {
    return GraphQLObjectType.newObject()
      .name("PointsOnLink")
      .description(
        "A list of coordinates encoded as a polyline string (see http://code.google.com/apis/maps/documentation/polylinealgorithm.html)"
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("length")
          .description("The number of points in the string")
          .type(Scalars.GraphQLInt)
          .dataFetcher(environment -> ((EncodedPolyline) environment.getSource()).length())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("points")
          .description(
            "The encoded points of the polyline. Be aware that the string could contain escape characters that need to be accounted for. " +
            "(https://www.freeformatter.com/javascript-escape.html)"
          )
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> ((EncodedPolyline) environment.getSource()).points())
          .build()
      )
      .build();
  }
}
