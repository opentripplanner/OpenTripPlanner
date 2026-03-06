package org.opentripplanner.apis.transmodel.model.stop;

import graphql.Scalars;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import org.opentripplanner.apis.transmodel.model.ModeAndSubModeInputType;

public class EstimatedCallSelectInputType {

  static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType.newInputObject()
    .name("EstimatedCallSelectInput")
    .description(
      "A selector for filter allow-list / exclude-list. " +
        "An estimated call matches a selector if it matches all fields. " +
        "Within each field, a call matches if it matches any of the listed values."
    )
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name("lines")
        .description(
          "Set of ids for lines that should be included in/excluded from search. " +
            "A call matches if its line matches any of the given IDs. " +
            "An empty list is not allowed. Omit the field to match all lines."
        )
        .type(new GraphQLList(new GraphQLNonNull(Scalars.GraphQLID)))
        .build()
    )
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name("authorities")
        .description(
          "Set of ids for authorities that should be included in/excluded from search. " +
            "A call matches if its authority matches any of the given IDs. " +
            "An empty list is not allowed. Omit the field to match all authorities."
        )
        .type(new GraphQLList(new GraphQLNonNull(Scalars.GraphQLID)))
        .build()
    )
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name("transportModes")
        .description(
          "The allowed modes for the estimated call. " +
            "A call matches if its mode matches any of the listed modes. " +
            "An empty list is not allowed. Omit the field to match all modes."
        )
        .type(new GraphQLList(new GraphQLNonNull(ModeAndSubModeInputType.INPUT_TYPE)))
        .build()
    )
    .build();
}
