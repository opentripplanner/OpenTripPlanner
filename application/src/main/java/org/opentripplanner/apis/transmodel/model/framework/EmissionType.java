package org.opentripplanner.apis.transmodel.model.framework;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.model.plan.Emission;

public class EmissionType {

  public static GraphQLObjectType create() {
    return GraphQLObjectType.newObject()
      .name("Emission")
      .description("Emission information for a trip-pattern or legs.")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("co2")
          .description("The average CO₂ emission per passenger in grams.")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(e -> ((Emission) e.getSource()).co2().asDouble())
          .build()
      )
      .build();
  }
}
