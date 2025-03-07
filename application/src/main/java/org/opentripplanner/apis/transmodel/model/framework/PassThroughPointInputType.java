package org.opentripplanner.apis.transmodel.model.framework;

import graphql.Scalars;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;

public class PassThroughPointInputType {

  public static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType.newInputObject()
    .name("PassThroughPoint")
    .description("Defines one point which the journey must pass through.")
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name("name")
        .description(
          "Optional name of the pass-through point for debugging and logging. It is not used in routing."
        )
        .type(Scalars.GraphQLString)
        .build()
    )
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name("placeIds")
        .description(
          """
          The list of *stop location ids* which define the pass-through point. At least one id is required.
          Quay, StopPlace, multimodal StopPlace, and GroupOfStopPlaces are supported location types.
          The journey must pass through at least one of these entities - not all of them."""
        )
        .type(new GraphQLList(new GraphQLNonNull(Scalars.GraphQLString)))
        .build()
    )
    .build();
}
