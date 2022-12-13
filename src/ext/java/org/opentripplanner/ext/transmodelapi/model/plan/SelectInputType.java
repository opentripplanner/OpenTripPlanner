package org.opentripplanner.ext.transmodelapi.model.plan;

import static org.opentripplanner.ext.transmodelapi.support.GqlUtil.newIdListInputField;

import graphql.Scalars;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import java.util.List;

public class SelectInputType {

  static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType
    .newInputObject()
    .name("TripFilterSelectInput")
    .description("A list of selectors for filter allow-list / exclude-list")
    .field(
      newIdListInputField(
        "lines",
        "Set of ids for lines that should be included in/excluded from search"
      )
    )
    .field(
      newIdListInputField(
        "authorities",
        "Set of ids for authorities that should be included in/excluded from search"
      )
    )
    .field(
      GraphQLInputObjectField
        .newInputObjectField()
        .name("feeds")
        .description(
          "Set of ids for feeds that should be should be included in/excluded from search"
        )
        .type(new GraphQLList(new GraphQLNonNull(Scalars.GraphQLString)))
        .defaultValue(List.of())
        .build()
    )
    .field(
      newIdListInputField(
        "serviceJourneys",
        "Set of ids for service journeys that should be included in/excluded from search"
      )
    )
    .field(
      GraphQLInputObjectField
        .newInputObjectField()
        .name("transportModes")
        .description(
          "The allowed modes for the transit part of the trip. Use an empty list to " +
          "disallow transit for this search. If the element is not present or null, it will " +
          "default to all transport modes."
        )
        .type(new GraphQLList(ModeAndSubModeInputType.INPUT_TYPE))
        .build()
    )
    // TODO: 2022-11-29 filters: planned cancellations?
    // TODO: 2022-11-29 filters: bikes ?
    // TODO: 2022-11-29 filters: wheelchair?
    // TODO: 2022-11-29 filters: groups of lines
    .build();
}
