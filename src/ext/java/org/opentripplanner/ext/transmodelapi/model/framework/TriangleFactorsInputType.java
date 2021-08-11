package org.opentripplanner.ext.transmodelapi.model.framework;

import graphql.Scalars;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;

public class TriangleFactorsInputType {

    public static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType.newInputObject()
            .name("TriangleFactors")
            .description(
                    "Input format for specifying a location through either a place reference (id), coordinates or both. If both place and coordinates are provided the place ref will be used if found, coordinates will only be used if place is not known.")
            .field(GraphQLInputObjectField.newInputObjectField()
                    .name("safety")
                    .description("The name of the location. This is pass-through information and is not used in routing.")
                    .type(Scalars.GraphQLFloat)
                    .defaultValue(0.4)
                    .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                    .name("slope")
                    .description("The name of the location. This is pass-through information and is not used in routing.")
                    .type(Scalars.GraphQLFloat)
                    .defaultValue(0.3)
                    .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                    .name("distance")
                    .description("The name of the location. This is pass-through information and is not used in routing.")
                    .defaultValue(0.3)
                    .type(Scalars.GraphQLFloat)
                    .build())
            .build();
}
