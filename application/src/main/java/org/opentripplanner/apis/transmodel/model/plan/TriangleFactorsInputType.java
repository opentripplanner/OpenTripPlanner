package org.opentripplanner.apis.transmodel.model.plan;

import graphql.Scalars;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLNonNull;

public class TriangleFactorsInputType {

  public static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType.newInputObject()
    .name("TriangleFactors")
    .description(
      "How much the factors safety, slope and distance are weighted relative to each other when routing bicycle legs. In total all three values need to add up to 1."
    )
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name("safety")
        .description("How important is bicycle safety expressed as a fraction of 1.")
        .type(new GraphQLNonNull(Scalars.GraphQLFloat))
        .build()
    )
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name("slope")
        .description("How important is slope/elevation expressed as a fraction of 1.")
        .type(new GraphQLNonNull(Scalars.GraphQLFloat))
        .build()
    )
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name("time")
        .description(
          "How important is time expressed as a fraction of 1. Note that what this really optimises for is distance (even if that means going over terrible surfaces, so I might be slower than the safe route)."
        )
        .type(new GraphQLNonNull(Scalars.GraphQLFloat))
        .build()
    )
    .build();
}
