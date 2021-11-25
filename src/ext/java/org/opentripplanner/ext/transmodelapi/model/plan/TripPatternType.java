package org.opentripplanner.ext.transmodelapi.model.plan;

import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.DataFetchingEnvironment;
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
                        .dataFetcher(env -> itinerary(env).startTime().getTime().getTime())
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("endTime")
                        .description("Time that the trip arrives.")
                        .type(gqlUtil.dateTimeScalar)
                        .deprecate("Replaced with expectedEndTime")
                        .dataFetcher(env -> itinerary(env).endTime().getTime().getTime())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("aimedStartTime")
                        .description("The aimed date and time the trip starts.")
                        .type(gqlUtil.dateTimeScalar)
                        .dataFetcher(
                                // startTime is already adjusted for realtime - need to subtract delay to get aimed time
                                env -> itinerary(env).startTime().getTime().getTime()
                                       - 1000L * itinerary(env).departureDelay())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("expectedStartTime")
                        .description(
                                "The expected, realtime adjusted date and time the trip starts.")
                        .type(gqlUtil.dateTimeScalar)
                        .dataFetcher(env -> itinerary(env).startTime().getTime().getTime())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("aimedEndTime")
                        .description("The aimed date and time the trip ends.")
                        .type(gqlUtil.dateTimeScalar)
                        .dataFetcher(
                                // endTime is already adjusted for realtime - need to subtract delay to get aimed time
                                env -> itinerary(env).endTime().getTime().getTime() 
                                        - 1000L * itinerary(env).arrivalDelay()
                        )
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("expectedEndTime")
                        .description("The expected, realtime adjusted date and time the trip ends.")
                        .type(gqlUtil.dateTimeScalar)
                        .dataFetcher(env -> itinerary(env).endTime().getTime().getTime())
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("duration")
                        .description("Duration of the trip, in seconds.")
                        .type(ExtendedScalars.GraphQLLong)
                        .dataFetcher(env -> itinerary(env).durationSeconds)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("directDuration")
                        .description("NOT IMPLEMENTED.")
                        .type(ExtendedScalars.GraphQLLong)
                        .dataFetcher(env -> itinerary(env).durationSeconds)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("waitingTime")
                        .description("How much time is spent waiting for transit to arrive, in seconds.")
                        .type(ExtendedScalars.GraphQLLong)
                        .dataFetcher(env -> itinerary(env).waitingTimeSeconds)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("distance")
                        .description("Total distance for the trip, in meters. NOT IMPLEMENTED")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(env -> itinerary(env).distanceMeters())
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("walkTime")
                        .description("How much time is spent walking, in seconds.")
                        .type(ExtendedScalars.GraphQLLong)
                        // TODO This unfortunately include BIKE and CAR
                        .dataFetcher(env -> itinerary(env).nonTransitTimeSeconds)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("walkDistance")
                        // TODO This unfortunately include BIKE and CAR
                        .description("How far the user has to walk, in meters.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(env -> itinerary(env).nonTransitDistanceMeters)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("legs")
                        .description(
                                "A list of legs. Each leg is either a walking (cycling, car) " 
                                + "portion of the trip, or a ride leg on a particular vehicle. So " 
                                + "a trip where the use walks to the Q train, transfers to the 6, " 
                                + "then walks to their destination, has four legs."
                        )
                        .type(new GraphQLNonNull(new GraphQLList(legType)))
                        .dataFetcher(env -> itinerary(env).legs)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("systemNotices")
                        .description("Get all system notices.")
                        .type(new GraphQLNonNull(new GraphQLList(systemNoticeType)))
                        .dataFetcher(env -> itinerary(env).systemNotices)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("generalizedCost")
                        .description(
                                "Generalized cost or weight of the itinerary. Used for debugging."
                        )
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(env -> itinerary(env).generalizedCost)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("waitTimeOptimizedCost")
                        .description(
                                "A cost calculated to distribute wait-time and avoid very "
                                + "short transfers. This field is meant for debugging only."
                        )
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(
                                env -> itinerary(env).waitTimeOptimizedCost)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("transferPriorityCost")
                        .description(
                                "A cost calculated to favor transfer with higher priority. This "
                                        + "field is meant for debugging only."
                        )
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(
                                env -> itinerary(env).transferPriorityCost)
                        .build())
                .build();
    }

    public static Itinerary itinerary(DataFetchingEnvironment env) {
        return env.getSource();
    }
}
