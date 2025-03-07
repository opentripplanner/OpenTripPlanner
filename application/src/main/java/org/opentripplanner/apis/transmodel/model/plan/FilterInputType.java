package org.opentripplanner.apis.transmodel.model.plan;

import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;

public class FilterInputType {

  public static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType.newInputObject()
    .name("TripFilterInput")
    .description(
      "A collection of selectors for what lines/trips should be included in / excluded from search"
    )
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name("select")
        .description(
          "A list of selectors for what lines/trips should be allowed during search. " +
          "In order to be accepted a trip/line has to match with at least one selector. " +
          "An empty list means that everything should be allowed. "
        )
        .type(new GraphQLList(new GraphQLNonNull(SelectInputType.INPUT_TYPE)))
        .build()
    )
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name("not")
        .description(
          "A list of selectors for what lines/trips should be excluded during the search. " +
          "If line/trip matches with at least one selector it will be excluded."
        )
        .type(new GraphQLList(new GraphQLNonNull(SelectInputType.INPUT_TYPE)))
        .build()
    )
    .build();
}
