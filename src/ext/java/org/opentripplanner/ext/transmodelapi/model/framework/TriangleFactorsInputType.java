package org.opentripplanner.ext.transmodelapi.model.framework;

import graphql.Scalars;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;

public class TriangleFactorsInputType {

    public static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType.newInputObject()
            .name("TriangleFactors")
            .description(
                    "How much the factors safety, slope and distance are weighted relative to each other when routing bicycle legs. In total all three values need to add up to 1.")
            .field(GraphQLInputObjectField.newInputObjectField()
                    .name("safety")
                    .description("How important is bicycle safety expressed as a fraction of 1.")
                    .type(Scalars.GraphQLFloat)
                    .defaultValue(0.4)
                    .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                    .name("slope")
                    .description("How important is slope/elevation expressed as a fraction of 1.")
                    .type(Scalars.GraphQLFloat)
                    .defaultValue(0.3)
                    .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                    .name("distance")
                    .description("How important is raw distance expressed as a fraction of 1.")
                    .defaultValue(0.3)
                    .type(Scalars.GraphQLFloat)
                    .build())
            .build();
}
