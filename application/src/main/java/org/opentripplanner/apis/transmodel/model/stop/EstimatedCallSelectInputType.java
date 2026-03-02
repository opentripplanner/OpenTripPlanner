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
      "A list of selectors for filter allow-list / exclude-list. " +
        "An empty list means that everything is allowed. " +
        "An estimated call will match with selectors if it matches with all non-empty lists. " +
        "The `select` is always applied first, then `not`. If only `not` is present, the exclude " +
        "is applied to the existing set of estimated calls. "
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
        .name("transportModes")
        .description(
          "The allowed modes of the journeys of the estimated call. " +
            "If the element is not present or null, it will default to all transport modes."
        )
        .type(new GraphQLList(new GraphQLNonNull(ModeAndSubModeInputType.INPUT_TYPE)))
        .build()
    )
    .build();
}
