package org.opentripplanner.apis.transmodel.model.plan.legacyvia;

import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import org.opentripplanner.apis.transmodel.model.EnumTypes;
import org.opentripplanner.apis.transmodel.model.plan.FilterInputType;

public class ViaSegmentInputType {

  public static GraphQLInputObjectType create() {
    final GraphQLInputObjectType streetModes = GraphQLInputObjectType.newInputObject()
      .name("StreetModes")
      .description(
        "Input format for specifying which modes will be allowed for this search. " +
        "If this element is not present, it will default to all to foot."
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name("accessMode")
          .description(
            "The mode used to get from the origin to the access stops in the transit " +
            "network the transit network (first-mile). If the element is not present or null," +
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
      .build();

    return GraphQLInputObjectType.newInputObject()
      .name("ViaSegmentInput")
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name("modes")
          .description("The set of access/egress/direct modes to be used for this search.")
          .type(streetModes)
          .build()
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name("filters")
          .description(
            "A list of filters for which trips should be included. A trip will be included if it " +
            "matches with at least one filter. An empty list of filters means that all trips " +
            "should be included."
          )
          .type(new GraphQLList(new GraphQLNonNull(FilterInputType.INPUT_TYPE)))
          .build()
      )
      .build();
  }
}
