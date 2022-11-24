package org.opentripplanner.ext.transmodelapi.model.plan;

import graphql.Scalars;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;

public class FilterSelectorInputType {

  static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType
    .newInputObject()
    .name("FilterSelector")
    .description("TODO")
    .field(
      GraphQLInputObjectField
        .newInputObjectField()
        .name("feedIds")
        .description("TODO")
        .type(Scalars.GraphQLString)
        .build()
    )
    .build();
}
