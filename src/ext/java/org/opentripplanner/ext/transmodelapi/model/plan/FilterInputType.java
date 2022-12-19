package org.opentripplanner.ext.transmodelapi.model.plan;

import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;

public class FilterInputType {

  static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType
    .newInputObject()
    .name("TripFilterInput")
    // TODO: 2022-12-19 filters: Correct documentation. Give some examples
    .description(
      "a collection of allow/exclude-lists that defines which trips or lines should be included during search" +
      "" +
      "" +
      "" +
      ". A trip or line has to match with " +
      "with at least one filter in order to be included in search. Not specifying any filters means that everything should be included. " +
      "If a search include this parameter, \"whiteListed\", \"banned\" & \"modes.transportModes\" filters will be ignored."
    )
    .field(
      GraphQLInputObjectField
        .newInputObjectField()
        .name("select")
        .description(
          "A list of selectors for what lines/trips should be allowed during search." +
          " I order to be accepted a trip/line has to match with at least one selector." +
          "An empty list means that everything should be allowed. "
        )
        .type(new GraphQLList(SelectInputType.INPUT_TYPE))
        .build()
    )
    .field(
      GraphQLInputObjectField
        .newInputObjectField()
        .name("not")
        .description(
          "A list of selectors for what lines/trips should be excluded during the search. " +
          "If line/trip matches with at least one selector it will be excluded."
        )
        .type(new GraphQLList(SelectInputType.INPUT_TYPE))
        .build()
    )
    .build();
}
