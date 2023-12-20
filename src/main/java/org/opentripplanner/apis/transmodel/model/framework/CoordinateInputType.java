package org.opentripplanner.apis.transmodel.model.framework;

import graphql.Scalars;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLNonNull;

public class CoordinateInputType {

  public static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType
    .newInputObject()
    .name("InputCoordinates")
    .description("Input type for coordinates in the WGS84 system")
    .field(
      GraphQLInputObjectField
        .newInputObjectField()
        .name("latitude")
        .description("The latitude of the place.")
        .type(new GraphQLNonNull(Scalars.GraphQLFloat))
        .build()
    )
    .field(
      GraphQLInputObjectField
        .newInputObjectField()
        .name("longitude")
        .description("The longitude of the place.")
        .type(new GraphQLNonNull(Scalars.GraphQLFloat))
        .build()
    )
    .build();
}
