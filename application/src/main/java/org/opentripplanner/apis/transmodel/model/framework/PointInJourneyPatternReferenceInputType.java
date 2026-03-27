package org.opentripplanner.apis.transmodel.model.framework;

import static graphql.Scalars.GraphQLString;

import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLScalarType;

public class PointInJourneyPatternReferenceInputType {

  public static final String FIELD_STOP_LOCATION_ID = "stopLocationId";
  public static final String FIELD_AIMED_DEPARTURE_TIME = "aimedDepartureTime";

  public static GraphQLInputObjectType create(GraphQLScalarType dateTimeScalar) {
    return GraphQLInputObjectType.newInputObject()
      .name("PointInJourneyPatternReference")
      .description(
        "Identifies a point in a journey pattern by stop location ID, " +
          "optionally with an aimed departure time for disambiguation."
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name(FIELD_STOP_LOCATION_ID)
          .description(
            "The stop location ID (quay or stop place). " +
              "Must not be the last stop in the journey pattern, " +
              "as boarding there would not allow further travel."
          )
          .type(new GraphQLNonNull(GraphQLString))
          .build()
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name(FIELD_AIMED_DEPARTURE_TIME)
          .description(
            "The exact aimed departure time at this stop, corresponding to the " +
              "aimedDepartureTime on EstimatedCall. Must match exactly. " +
              "Used for disambiguation when the stop location is visited more than once " +
              "in the journey pattern (e.g. ring lines). " +
              "If provided, it is always validated against the timetable."
          )
          .type(dateTimeScalar)
          .build()
      )
      .build();
  }
}
