package org.opentripplanner.ext.transmodelapi.model.plan;

import static org.opentripplanner.ext.transmodelapi.support.GqlUtil.newIdListInputField;

import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;

public class FilterInputType {

  // TODO: 2022-11-29 filters: move to separate class
  static final GraphQLInputObjectType SELECT_INPUT_TYPE = GraphQLInputObjectType
    .newInputObject()
    .name("TripFilterSelectInput")
    .description("TODO")
    .field(newIdListInputField("lines", "Set of ids for lines that should be included in/excluded from search"))
    .field(newIdListInputField("authorities", "Set of ids for authorities that should be included in/excluded from search"))
    .field(newIdListInputField("feeds", "Set of ids for feeds that should be should be included in/excluded from search"))
    .field(newIdListInputField("serviceJourneys", "Set of ids for service journeys that should be included in /excluded from search"))
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

  static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType
    .newInputObject()
    .name("TripFilterInput")
    .description("TODO")
    .field(
      GraphQLInputObjectField
        .newInputObjectField()
        .name("include")
        .description("TODO")
        .type(SELECT_INPUT_TYPE)
        .build()
    )
    .field(
      GraphQLInputObjectField
        .newInputObjectField()
        .name("exclude")
        .description("TODO")
        .type(SELECT_INPUT_TYPE)
        .build()
    )
    .build();
}
