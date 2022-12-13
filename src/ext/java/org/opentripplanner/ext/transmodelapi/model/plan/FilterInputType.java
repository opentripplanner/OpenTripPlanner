package org.opentripplanner.ext.transmodelapi.model.plan;

import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;

public class FilterInputType {

  static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType
    .newInputObject()
    .name("TripFilterInput")
    .description(
      "List of filters that should be applied on the search. A trip or line has to match with " +
      "with at least one filter in order to be included in search. Not specifying any filters means that everything should be included. " +
      "If a search include this parameter, \"whiteListed\", \"banned\" & \"modes.transportModes\" filters will be ignored."
    )
    .field(
      GraphQLInputObjectField
        .newInputObjectField()
        .name("select")
        .description(
          "Combination of allow-lists for which lines/trips should be included. In order to be accepted " +
          "trip/line has to match with all allow-lists. An empty list means allow all."
        )
        .type(new GraphQLList(SelectInputType.INPUT_TYPE))
        .build()
    )
    .field(
      GraphQLInputObjectField
        .newInputObjectField()
        .name("not")
        .description(
          "Combination of exclude-lists for which lines/trips should be excluded. In order to be accepted " +
          "trip/lines cannot match with any exclude-list."
        )
        .type(new GraphQLList(SelectInputType.INPUT_TYPE))
        .build()
    )
    .build();
}
