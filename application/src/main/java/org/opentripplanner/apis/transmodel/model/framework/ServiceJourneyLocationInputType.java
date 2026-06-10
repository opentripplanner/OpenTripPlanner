package org.opentripplanner.apis.transmodel.model.framework;

import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLScalarType;

public class ServiceJourneyLocationInputType {

  public static GraphQLInputObjectType create(GraphQLScalarType dateTimeScalar) {
    return GraphQLInputObjectType.newInputObject()
      .name("ServiceJourneyLocationInput")
      .description("Identifies a specific dated service journey and a specific point within it.")
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
            "Identifies a specific point in the journey pattern. Note that cancelled stops " +
              "are not allowed unless includePlannedCancellations/includeRealtimeCancellations " +
              "are set accordingly."
          )
          .type(new GraphQLNonNull(PointInJourneyPatternReferenceInputType.create(dateTimeScalar)))
          .build()
      )
      .build();
  }
}
