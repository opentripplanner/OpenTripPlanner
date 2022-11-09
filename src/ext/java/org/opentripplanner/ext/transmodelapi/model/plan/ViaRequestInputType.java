package org.opentripplanner.ext.transmodelapi.model.plan;

import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;

public class ViaRequestInputType {

  public static GraphQLInputObjectType create() {
    return GraphQLInputObjectType
      .newInputObject()
      .name("ViaRequest")
      .field(
        GraphQLInputObjectField
          .newInputObjectField()
          .name("modes")
          .description(
            "The set of access/egress/direct/transit modes to be used for this search. " +
            "Note that this only works at the Line level. If individual ServiceJourneys have " +
            "modes that differ from the Line mode, this will NOT be accounted for."
          )
          .type(ModeInputType.INPUT_TYPE)
          .build()
      )
      .build();
  }
}
