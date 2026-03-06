package org.opentripplanner.apis.transmodel.model.stop;

import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;

public class EstimatedCallFilterInputType {

  public static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType.newInputObject()
    .name("EstimatedCallFilterInput")
    .description(
      "A collection of selectors for what estimated calls should be included / excluded. " +
        "At least one of `select` or `not` must be provided. " +
        "The `select` is always applied first, then `not`. " +
        "If only `not` is present, the exclude is applied to the existing set of estimated calls."
    )
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name("select")
        .description(
          "A list of selectors for what estimated calls should be included. " +
            "A call is included if it matches at least one selector. " +
            "Omit the field to include all calls. An empty list is not allowed."
        )
        .type(new GraphQLList(new GraphQLNonNull(EstimatedCallSelectInputType.INPUT_TYPE)))
        .build()
    )
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name("not")
        .description(
          "A list of selectors for what estimated calls should be excluded during the search. " +
            "If a call matches with at least one selector it will be excluded."
        )
        .type(new GraphQLList(new GraphQLNonNull(EstimatedCallSelectInputType.INPUT_TYPE)))
        .build()
    )
    .build();
}
