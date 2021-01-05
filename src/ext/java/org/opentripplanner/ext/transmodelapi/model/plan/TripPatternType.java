package org.opentripplanner.ext.transmodelapi.model.plan;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.model.plan.Itinerary;

public class TripPatternType {
  public static GraphQLObjectType create(
      GraphQLOutputType systemNoticeType,
      GraphQLObjectType legType,
      GqlUtil gqlUtil
  ) {
    return GraphQLObjectType
        .newObject()
        .name("TripPattern")
        .description(
            "List of legs constituting a suggested sequence of rides and links for a specific trip.")
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("startTime")
            .description("Time that the trip departs.")
            .type(gqlUtil.dateTimeScalar)
            .deprecate("Replaced with expectedStartTime")
            .dataFetcher(environment -> ((Itinerary) environment.getSource())
                .startTime()
                .getTime()
                .getTime())
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("endTime")
            .description("Time that the trip arrives.")
            .type(gqlUtil.dateTimeScalar)
            .deprecate("Replaced with expectedEndTime")
            .dataFetcher(environment -> ((Itinerary) environment.getSource())
                .endTime()
                .getTime()
                .getTime())
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("aimedStartTime")
            .description("The aimed date and time the trip starts.")
            .type(gqlUtil.dateTimeScalar)
            .dataFetcher(
                // startTime is already adjusted for realtime - need to subtract delay to get aimed time
                environment -> ((Itinerary) environment.getSource())
                    .startTime()
                    .getTime()
                    .getTime() -
                    (1000 * ((Itinerary) environment.getSource())
                        .departureDelay()))
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("expectedStartTime")
            .description("The expected, realtime adjusted date and time the trip starts.")
            .type(gqlUtil.dateTimeScalar)
            .dataFetcher(
                environment -> ((Itinerary) environment.getSource())
                    .startTime()
                    .getTime()
                    .getTime())
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("aimedEndTime")
            .description("The aimed date and time the trip ends.")
            .type(gqlUtil.dateTimeScalar)
            .dataFetcher(
                // endTime is already adjusted for realtime - need to subtract delay to get aimed time
                environment -> ((Itinerary) environment.getSource())
                    .endTime()
                    .getTime()
                    .getTime() -
                    (1000 * ((Itinerary) environment.getSource())
                        .arrivalDelay()))
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("expectedEndTime")
            .description("The expected, realtime adjusted date and time the trip ends.")
            .type(gqlUtil.dateTimeScalar)
            .dataFetcher(environment -> ((Itinerary) environment.getSource())
                .endTime()
                .getTime()
                .getTime())
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("duration")
            .description("Duration of the trip, in seconds.")
            .type(Scalars.GraphQLLong)
            .dataFetcher(environment -> ((Itinerary) environment.getSource()).durationSeconds)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("directDuration")
            .description("NOT IMPLEMENTED.")
            .type(Scalars.GraphQLLong)
            .dataFetcher(environment -> ((Itinerary) environment.getSource()).durationSeconds)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("waitingTime")
            .description("How much time is spent waiting for transit to arrive, in seconds.")
            .type(Scalars.GraphQLLong)
            .dataFetcher(environment -> ((Itinerary) environment.getSource()).waitingTimeSeconds)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("distance")
            .description("Total distance for the trip, in meters. NOT IMPLEMENTED")
            .type(Scalars.GraphQLFloat)
            .dataFetcher(environment -> ((Itinerary) environment.getSource()).distanceMeters())
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("walkTime")
            .description("How much time is spent walking, in seconds.")
            .type(Scalars.GraphQLLong)
            // TODO This unfortunately include BIKE and CAR
            .dataFetcher(environment -> ((Itinerary) environment.getSource()).nonTransitTimeSeconds)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("walkDistance")
            // TODO This unfortunately include BIKE and CAR
            .description("How far the user has to walk, in meters.")
            .type(Scalars.GraphQLFloat)
            .dataFetcher(environment -> ((Itinerary) environment.getSource()).nonTransitDistanceMeters)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("legs")
            .description(
                "A list of legs. Each leg is either a walking (cycling, car) portion of the trip, or a ride leg on a particular vehicle. So a trip where the use walks to the Q train, transfers to the 6, then walks to their destination, has four legs.")
            .type(new GraphQLNonNull(new GraphQLList(legType)))
            .dataFetcher(environment -> ((Itinerary) environment.getSource()).legs)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("systemNotices")
            .description("Get all system notices.")
            .type(new GraphQLNonNull(new GraphQLList(systemNoticeType)))
            .dataFetcher(env -> ((Itinerary) env.getSource()).systemNotices)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("generalizedCost")
            .description("Generalized cost or weight of the itinerary. Used for debugging.")
            .type(Scalars.GraphQLInt)
            .dataFetcher(env -> ((Itinerary) env.getSource()).generalizedCost)
            .build())
        .build();
  }

}
