package org.opentripplanner.ext.transmodelapi.model.plan;

import static org.opentripplanner.ext.transmodelapi.support.GqlUtil.newIdListInputField;

import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;

public class FilterInputType {

  static final GraphQLInputObjectType WHITELISTED_INPUT = GraphQLInputObjectType
    .newInputObject()
    .name("FilterWhitelist")
    .description(
      "Filter trips by only allowing lines involving certain " +
        "elements. If both lines and authorities are specified, only one must be valid " +
        "for each line to be used. If a line is both banned and whitelisted, it will " +
        "be counted as banned."
    )
    .field(newIdListInputField("lines", "Set of ids for lines that should be used"))
    .field(newIdListInputField("authorities", "Set of ids for authorities that should be used"))
    .field(
      newIdListInputField("groupsOfLines", "Set of ids for groups of lines that should be used")
    )
    .build();

  static final GraphQLInputObjectType BANNED_INPUT = GraphQLInputObjectType
    .newInputObject()
    .name("FilterBanned")
    .description(
      "Filter trips by disallowing lines involving certain elements. If both lines and " +
        "authorities are specified, only one must be valid for each line to be banned. If a " +
        "line is both banned and whitelisted, it will be counted as banned."
    )
    .field(GqlUtil.newIdListInputField("lines", "Set of ids for lines that should not be used"))
    .field(
      GqlUtil.newIdListInputField(
        "authorities",
        "Set of ids for authorities that should not be used"
      )
    )
    .field(
      GqlUtil.newIdListInputField(
        "groupsOfLines",
        "Set of ids of groups of lines that should not be used"
      )
    )
    .build();

  static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType
    .newInputObject()
    .name("Filters")
    .description("TODO")
    .field(
      GraphQLInputObjectField
        .newInputObjectField()
        .name("selector")
        .description("TODO")
        .type(FilterSelectorInputType.INPUT_TYPE)
        .build()
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
        .type(new GraphQLList(TransportModeInputType.INPUT_TYPE))
        .build()
    )
    .field(
      GraphQLInputObjectField
        .newInputObjectField()
        .name("banned")
        .description(
          "Parameters for indicating authorities, group of lines or lines not be used in the trip patterns"
        )
        .type(BANNED_INPUT)
        .build()
    )
    .field(
      GraphQLInputObjectField
        .newInputObjectField()
        .name("whiteListed")
        .description(
          "Parameters for indicating the only authorities, group of lines or lines to be used in the trip patterns"
        )
        .type(WHITELISTED_INPUT)
        .build()
    )
    .build();
}
