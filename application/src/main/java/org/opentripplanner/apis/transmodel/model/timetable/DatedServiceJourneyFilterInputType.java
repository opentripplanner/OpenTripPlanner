package org.opentripplanner.apis.transmodel.model.timetable;

import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;

public class DatedServiceJourneyFilterInputType {

  public static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType.newInputObject()
    .name("DatedServiceJourneyFilterInput")
    .description(
      "A collection of selectors for which dated service journeys should be included / excluded. " +
        "At least one of `select` or `not` must be provided. " +
        "The `select` is always applied first, then `not`. " +
        "If only `not` is present, the exclusion is applied to all journeys."
    )
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name("select")
        .description(
          "A list of selectors for which journeys should be included. " +
            "A journey is included if it matches at least one selector. " +
            "Omit the field to include all journeys. An empty list is not allowed."
        )
        .type(new GraphQLList(new GraphQLNonNull(DatedServiceJourneySelectInputType.INPUT_TYPE)))
        .build()
    )
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name("not")
        .description(
          "A list of selectors for journeys that should be excluded. " +
            "If a journey matches at least one selector it will be excluded."
        )
        .type(new GraphQLList(new GraphQLNonNull(DatedServiceJourneySelectInputType.INPUT_TYPE)))
        .build()
    )
    .build();
}
