package org.opentripplanner.apis.transmodel.model.plan;

import static org.opentripplanner.apis.transmodel.model.EnumTypes.TRANSPORT_MODE;

import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import org.opentripplanner.apis.transmodel.model.EnumTypes;

public class ModeAndSubModeInputType {

  static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType.newInputObject()
    .name("TransportModes")
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name("transportMode")
        .description(
          "A transportMode that should be allowed for this search. You can further" +
          "narrow it down by specifying a list of transportSubModes"
        )
        .type(TRANSPORT_MODE)
        .build()
    )
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name("transportSubModes")
        .description(
          "The allowed transportSubModes for this search. If this element is not" +
          "present or null, it will default to all transportSubModes for the specified" +
          "TransportMode. Be aware that all transportSubModes have an associated " +
          "TransportMode, which must match what is specified in the transportMode field."
        )
        .type(new GraphQLList(EnumTypes.TRANSPORT_SUBMODE))
        .build()
    )
    .build();
}
