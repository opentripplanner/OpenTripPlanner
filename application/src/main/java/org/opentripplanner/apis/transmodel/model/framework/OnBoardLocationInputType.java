package org.opentripplanner.apis.transmodel.model.framework;

import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLScalarType;

public class OnBoardLocationInputType {

  public static GraphQLInputObjectType create(GraphQLScalarType dateTimeScalar) {
    return GraphQLInputObjectType.newInputObject()
      .name("OnBoardLocationInput")
      .description(
        "Identifies a position on-board a specific service journey. " +
          "Used to start a trip planning search from on-board a vehicle."
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name("datedServiceJourneyReference")
          .description(
            "Identifies the service journey and service date, either by service journey ID " +
              "and service date, or by a dated service journey ID."
          )
          .type(new GraphQLNonNull(DatedServiceJourneyReferenceInputType.INPUT_TYPE))
          .build()
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name("pointInJourneyPatternReference")
          .description(
            "Identifies the point in the journey pattern where the traveler is " +
              "considered to be boarding, or the last stop passed."
          )
          .type(new GraphQLNonNull(PointInJourneyPatternReferenceInputType.create(dateTimeScalar)))
          .build()
      )
      .build();
  }
}
