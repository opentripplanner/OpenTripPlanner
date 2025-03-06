package org.opentripplanner.apis.transmodel.model.plan;

import graphql.Scalars;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;

public class SelectInputType {

  static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType.newInputObject()
    .name("TripFilterSelectInput")
    .description(
      "A list of selectors for filter allow-list / exclude-list. " +
      "An empty list means that everything is allowed. " +
      "A trip/line will match with selectors if it matches with all non-empty lists. " +
      "The `select` is always applied first, then `not`. If only `not` not is present, the exclude " +
      "is applied to the existing set of lines. "
    )
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name("lines")
        .description("Set of ids for lines that should be included in/excluded from search")
        .type(new GraphQLList(new GraphQLNonNull(Scalars.GraphQLID)))
        .build()
    )
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name("authorities")
        .description("Set of ids for authorities that should be included in/excluded from search")
        .type(new GraphQLList(new GraphQLNonNull(Scalars.GraphQLID)))
        .build()
    )
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name("serviceJourneys")
        .description(
          "Set of ids for service journeys that should be included in/excluded from search"
        )
        .type(new GraphQLList(new GraphQLNonNull(Scalars.GraphQLID)))
        .build()
    )
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name("transportModes")
        .description(
          "The allowed modes for the transit part of the trip. Use an empty list to " +
          "disallow transit for this search. If the element is not present or null, it will " +
          "default to all transport modes."
        )
        .type(new GraphQLList(new GraphQLNonNull(ModeAndSubModeInputType.INPUT_TYPE)))
        .build()
    )
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name("groupOfLines")
        .description(
          "Set of ids for group of lines that should be included in/excluded from the search"
        )
        .type(new GraphQLList(new GraphQLNonNull(Scalars.GraphQLID)))
        .build()
    )
    .build();
}
