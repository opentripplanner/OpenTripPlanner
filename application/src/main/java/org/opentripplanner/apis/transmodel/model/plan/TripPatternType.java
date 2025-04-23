package org.opentripplanner.apis.transmodel.model.plan;

import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import org.opentripplanner.model.plan.Itinerary;

public class TripPatternType {

  public static GraphQLObjectType create(
    GraphQLOutputType systemNoticeType,
    GraphQLObjectType legType,
    GraphQLObjectType timePenaltyType,
    GraphQLObjectType emissionType,
    GraphQLScalarType dateTimeScalar
  ) {
    return GraphQLObjectType.newObject()
      .name("TripPattern")
      .description(
        "List of legs constituting a suggested sequence of rides and links for a specific trip."
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("startTime")
          .description("Time that the trip departs.")
          .type(dateTimeScalar)
          .deprecate("Replaced with expectedStartTime")
          .dataFetcher(env -> itinerary(env).startTime().toInstant().toEpochMilli())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("endTime")
          .description("Time that the trip arrives.")
          .type(dateTimeScalar)
          .deprecate("Replaced with expectedEndTime")
          .dataFetcher(env -> itinerary(env).endTime().toInstant().toEpochMilli())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("aimedStartTime")
          .description("The aimed date and time the trip starts.")
          .type(new GraphQLNonNull(dateTimeScalar))
          .dataFetcher(env ->
            // startTime is already adjusted for real-time - need to subtract delay to get aimed time
            itinerary(env)
              .startTime()
              .minusSeconds(itinerary(env).departureDelay())
              .toInstant()
              .toEpochMilli()
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("expectedStartTime")
          .description("The expected, real-time adjusted date and time the trip starts.")
          .type(new GraphQLNonNull(dateTimeScalar))
          .dataFetcher(env -> itinerary(env).startTime().toInstant().toEpochMilli())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("aimedEndTime")
          .description("The aimed date and time the trip ends.")
          .type(new GraphQLNonNull(dateTimeScalar))
          .dataFetcher(env ->
            // endTime is already adjusted for real-time - need to subtract delay to get aimed time
            itinerary(env)
              .endTime()
              .minusSeconds(itinerary(env).arrivalDelay())
              .toInstant()
              .toEpochMilli()
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("expectedEndTime")
          .description("The expected, real-time adjusted date and time the trip ends.")
          .type(new GraphQLNonNull(dateTimeScalar))
          .dataFetcher(env -> itinerary(env).endTime().toInstant().toEpochMilli())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("duration")
          .description("Duration of the trip, in seconds.")
          .type(ExtendedScalars.GraphQLLong)
          .dataFetcher(env -> itinerary(env).totalDuration().toSeconds())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("directDuration")
          .description("NOT IMPLEMENTED.")
          .type(ExtendedScalars.GraphQLLong)
          .dataFetcher(env -> itinerary(env).totalDuration().toSeconds())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("waitingTime")
          .description("How much time is spent waiting for transit to arrive, in seconds.")
          .type(ExtendedScalars.GraphQLLong)
          .dataFetcher(env -> itinerary(env).totalWaitingDuration().toSeconds())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("distance")
          .description("Total distance for the trip, in meters. NOT IMPLEMENTED")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(env -> itinerary(env).distanceMeters())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("walkTime")
          .description("How much time is spent walking, in seconds.")
          .type(ExtendedScalars.GraphQLLong)
          .dataFetcher(env -> itinerary(env).totalWalkDuration().toSeconds())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("streetDistance")
          .description(
            "How far the user has to walk, bike and/or drive in meters. It includes " +
            "all street(none transit) modes."
          )
          .type(Scalars.GraphQLFloat)
          .dataFetcher(env -> itinerary(env).totalStreetDistanceMeters())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("walkDistance")
          .deprecate("Replaced by `streetDistance`.")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(env -> itinerary(env).totalWalkDistanceMeters())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("legs")
          .description(
            "A list of legs. Each leg is either a walking (cycling, car) " +
            "portion of the trip, or a ride leg on a particular vehicle. So " +
            "a trip where the use walks to the Q train, transfers to the 6, " +
            "then walks to their destination, has four legs."
          )
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(legType))))
          .dataFetcher(env -> itinerary(env).legs())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("systemNotices")
          .description("Get all system notices.")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(systemNoticeType))))
          .dataFetcher(env -> itinerary(env).systemNotices())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("generalizedCost")
          .description("Generalized cost or weight of the itinerary. Used for debugging.")
          .type(Scalars.GraphQLInt)
          .dataFetcher(env -> itinerary(env).generalizedCostIncludingPenalty().toSeconds())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("generalizedCost2")
          .description(
            "A second cost or weight of the itinerary. Some use-cases like pass-through " +
            "and transit-priority-groups use a second cost during routing. This is used for debugging."
          )
          .type(Scalars.GraphQLInt)
          .dataFetcher(env -> itinerary(env).generalizedCost2().orElse(null))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("waitTimeOptimizedCost")
          .description(
            "A cost calculated to distribute wait-time and avoid very " +
            "short transfers. This field is meant for debugging only."
          )
          .type(Scalars.GraphQLInt)
          .dataFetcher(env -> itinerary(env).waitTimeOptimizedCost())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("transferPriorityCost")
          .description(
            "A cost calculated to favor transfer with higher priority. This " +
            "field is meant for debugging only."
          )
          .type(Scalars.GraphQLInt)
          .dataFetcher(env -> itinerary(env).transferPriorityCost())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("timePenalty")
          .description(
            """
            A time and cost penalty applied to access and egress to favor regular scheduled
            transit over potentially faster options with FLEX, Car, bike and scooter.

            Note! This field is meant for debugging only. The field can be removed without notice
            in the future.
            """
          )
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(timePenaltyType))))
          .dataFetcher(env -> TripPlanTimePenaltyDto.of(itinerary(env)))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("emission")
          .description(
            """
            The total emission per preson. The total emission is only available if all transit
            and car leg emissions can be calculated. If only a partial result is obtained, this
            will be null.
            """
          )
          .type(emissionType)
          .dataFetcher(env -> itinerary(env).emissionPerPerson())
          .build()
      )
      .build();
  }

  public static Itinerary itinerary(DataFetchingEnvironment env) {
    return env.getSource();
  }
}
