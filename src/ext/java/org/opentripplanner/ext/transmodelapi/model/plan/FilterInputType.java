package org.opentripplanner.ext.transmodelapi.model.plan;

import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.TRANSPORT_MODE;
import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.TRANSPORT_SUBMODE;
import static org.opentripplanner.ext.transmodelapi.support.GqlUtil.newIdListInputField;

import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;

public class FilterInputType {

  // TODO: 2022-11-29 move to separate class
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
        .name("modes")
        .description("Set of transport modes that should be included in/excluded from search")
        .type(new GraphQLList(TRANSPORT_MODE))
        .build()
    )
    .field(
      GraphQLInputObjectField
        .newInputObjectField()
        .name("subModes")
        .description("Set of transport sub modes that should be included in/excluded from search")
        .type(new GraphQLList(TRANSPORT_SUBMODE))
        .build()
    )
    // TODO: 2022-11-29 planned cancellations?
    // TODO: 2022-11-29 bikes ?
    // TODO: 2022-11-29 wheelchair?
    // TODO: 2022-11-29 groups of lines
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
