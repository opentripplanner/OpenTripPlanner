package org.opentripplanner.apis.transmodel.model.network;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.model.TripTimeOnDate;

public class DestinationDisplayType {

  public static GraphQLObjectType create() {
    return GraphQLObjectType.newObject()
      .name("DestinationDisplay")
      .description(
        "An advertised destination of a specific journey pattern, usually displayed on a head sign or at other on-board locations."
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("frontText")
          .description("Name of destination to show on front of vehicle.")
          .type(Scalars.GraphQLString)
          .dataFetcher(e -> ((TripTimeOnDate) e.getSource()).getHeadsign())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("via")
          .description(
            "Intermediary destinations which the vehicle will pass before reaching its final destination."
          )
          .type(new GraphQLList(Scalars.GraphQLString))
          .dataFetcher(e -> ((TripTimeOnDate) e.getSource()).getHeadsignVias())
          .build()
      )
      .build();
  }
}
