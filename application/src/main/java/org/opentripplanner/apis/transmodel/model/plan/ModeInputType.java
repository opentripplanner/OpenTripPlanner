package org.opentripplanner.apis.transmodel.model.plan;

import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import org.opentripplanner.apis.transmodel.model.EnumTypes;

class ModeInputType {

  static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType.newInputObject()
    .name("Modes")
    .description(
      "Input format for specifying which modes will be allowed for this search. " +
      "If this element is not present, it will default to accessMode/egressMode/directMode " +
      "of foot and all transport modes will be allowed."
    )
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name("accessMode")
        .description(
          "The mode used to get from the origin to the access stops in the transit " +
          "network (first-mile). If the element is not present or null," +
          "only transit that can be immediately boarded from the origin will be used."
        )
        .type(EnumTypes.STREET_MODE)
        .build()
    )
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name("egressMode")
        .description(
          "The mode used to get from the egress stops in the transit network to" +
          "the destination (last-mile). If the element is not present or null," +
          "only transit that can immediately arrive at the origin will be used."
        )
        .type(EnumTypes.STREET_MODE)
        .build()
    )
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name("directMode")
        .description(
          "The mode used to get from the origin to the destination directly, " +
          "without using the transit network. If the element is not present or null," +
          "direct travel without using transit will be disallowed."
        )
        .type(EnumTypes.STREET_MODE)
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
        .type(new GraphQLList(ModeAndSubModeInputType.INPUT_TYPE))
        .build()
    )
    .build();
}
