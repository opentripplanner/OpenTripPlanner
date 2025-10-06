package org.opentripplanner.apis.transmodel.model;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.routing.api.request.RouteRequest;

public class DefaultRouteRequestType {

  public final RouteRequest request;
  public final GraphQLObjectType graphQLType;

  public DefaultRouteRequestType(RouteRequest request) {
    this.request = request;
    this.graphQLType = createGraphQLType();
  }

  private GraphQLObjectType createGraphQLType() {
    var preferences = request.preferences();

    return GraphQLObjectType.newObject()
      .name("RoutingParameters")
      .description("The default parameters used in travel searches.")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("walkSpeed")
          .description("Max walk speed along streets, in meters per second")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(env -> preferences.walk().speed())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("bikeSpeed")
          .description("Max bike speed along streets, in meters per second")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(env -> preferences.bike().speed())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("carSpeed")
          .deprecate("This parameter is no longer configurable.")
          .description("Max car speed along streets, in meters per second")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(env -> 0)
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("maxDirectStreetDuration")
          .description(
            "This is the maximum duration in seconds for a direct street search. " +
            "This is a performance limit and should therefore be set high. " +
            "Use filters to limit what is presented to the client."
          )
          .type(Scalars.GraphQLInt)
          .dataFetcher(env -> preferences.street().maxDirectDuration().defaultValueSeconds())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("wheelChairAccessible")
          .description("Whether the trip must be wheelchair accessible.")
          .type(Scalars.GraphQLBoolean)
          .dataFetcher(env -> request.journey().wheelchair())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("numItineraries")
          .description("The maximum number of itineraries to return.")
          .type(Scalars.GraphQLInt)
          .dataFetcher(env -> request.numItineraries())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("maxSlope")
          .description("The maximum slope of streets for wheelchair trips.")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(env -> preferences.wheelchair().maxSlope())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("showIntermediateStops")
          .deprecate("Deprecated. This parameter is always enabled")
          .description(
            "Whether the planner should return intermediate stops lists for transit legs."
          )
          .deprecate("This parameter is always enabled")
          .type(Scalars.GraphQLBoolean)
          .dataFetcher(env -> true)
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("transferPenalty")
          .description(
            "An extra penalty added on transfers (i.e. all boardings except the first one)."
          )
          .type(Scalars.GraphQLInt)
          .dataFetcher(env -> preferences.transfer().cost())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("walkReluctance")
          .description(
            "A multiplier for how bad walking is, compared to being in transit for equal lengths of time."
          )
          .type(Scalars.GraphQLFloat)
          .dataFetcher(env -> preferences.walk().reluctance())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("stairsReluctance")
          .description(
            "A multiplier to specify how bad walking on stairs is, compared to walking on flat ground, on top of the walk reluctance."
          )
          .type(Scalars.GraphQLFloat)
          .dataFetcher(env -> preferences.walk().stairsReluctance())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("turnReluctance")
          .description("Multiplicative factor on expected turning time.")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(env -> preferences.street().turnReluctance())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("elevatorBoardTime")
          .description("How long does it take to get on an elevator, on average.")
          .type(Scalars.GraphQLInt)
          .dataFetcher(env -> preferences.street().elevator().boardTime())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("elevatorBoardCost")
          .description("What is the cost of boarding a elevator?")
          .type(Scalars.GraphQLInt)
          .dataFetcher(env -> preferences.street().elevator().boardCost())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("elevatorHopTime")
          .description("How long does it take to advance one floor on an elevator?")
          .type(Scalars.GraphQLInt)
          .dataFetcher(env -> preferences.street().elevator().hopTime())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("elevatorHopCost")
          .description("What is the cost of travelling one floor on an elevator?")
          .type(Scalars.GraphQLInt)
          .dataFetcher(env -> preferences.street().elevator().hopCost())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("bikeRentalPickupTime")
          .description("Time to rent a bike.")
          .type(Scalars.GraphQLInt)
          .dataFetcher(env -> (int) preferences.bike().rental().pickupTime().toSeconds())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("bikeRentalPickupCost")
          .description("Cost to rent a bike.")
          .type(Scalars.GraphQLInt)
          .dataFetcher(env -> preferences.bike().rental().pickupCost().toSeconds())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("bikeRentalDropOffTime")
          .description("Time to drop-off a rented bike.")
          .type(Scalars.GraphQLInt)
          .dataFetcher(env -> (int) preferences.bike().rental().dropOffTime().toSeconds())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("bikeRentalDropOffCost")
          .description("Cost to drop-off a rented bike.")
          .type(Scalars.GraphQLInt)
          .dataFetcher(env -> preferences.bike().rental().dropOffCost().toSeconds())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("bikeParkTime")
          .description("Time to park a bike.")
          .type(Scalars.GraphQLInt)
          .dataFetcher(env -> (int) preferences.bike().parking().time().toSeconds())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("bikeParkCost")
          .description("Cost to park a bike.")
          .type(Scalars.GraphQLInt)
          .dataFetcher(env -> preferences.bike().parking().cost().toSeconds())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("carDropOffTime")
          .description(
            "Time to park a car in a park and ride, w/o taking into account driving and walking cost."
          )
          .type(Scalars.GraphQLInt)
          .dataFetcher(env -> 0)
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("waitReluctance")
          .description(
            "How much worse is waiting for a transit vehicle than being on a transit vehicle, as a multiplier."
          )
          .type(Scalars.GraphQLFloat)
          .dataFetcher(env -> preferences.transfer().waitReluctance())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("walkBoardCost")
          .description(
            "This prevents unnecessary transfers by adding a cost for boarding a vehicle."
          )
          .type(Scalars.GraphQLInt)
          .dataFetcher(env -> preferences.walk().boardCost())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("bikeBoardCost")
          .description(
            "Separate cost for boarding a vehicle with a bicycle, which is more difficult than on foot."
          )
          .type(Scalars.GraphQLInt)
          .dataFetcher(env -> preferences.bike().boardCost())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("otherThanPreferredRoutesPenalty")
          .description(
            "Penalty added for using every route that is not preferred if user set any route as preferred. We return number of seconds that we are willing to wait for preferred route."
          )
          .type(Scalars.GraphQLInt)
          .dataFetcher(env -> preferences.transit().otherThanPreferredRoutesPenalty())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("transferSlack")
          .description(
            "A global minimum transfer time (in seconds) that specifies the minimum amount of time that must pass between exiting one transit vehicle and boarding another."
          )
          .type(Scalars.GraphQLInt)
          .dataFetcher(env -> preferences.transfer().slack().toSeconds())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("boardSlackDefault")
          .description(TransportModeSlack.boardSlackDescription("boardSlackList"))
          .type(Scalars.GraphQLInt)
          .dataFetcher(e -> preferences.transit().boardSlack().defaultValueSeconds())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("boardSlackList")
          .description(TransportModeSlack.slackByGroupDescription("boardSlack"))
          .type(TransportModeSlack.SLACK_LIST_OUTPUT_TYPE)
          .dataFetcher(e -> TransportModeSlack.mapToApiList(preferences.transit().boardSlack()))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("alightSlackDefault")
          .description(TransportModeSlack.alightSlackDescription("alightSlackList"))
          .type(Scalars.GraphQLInt)
          .dataFetcher(e -> preferences.transit().alightSlack().defaultValueSeconds())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("alightSlackList")
          .description(TransportModeSlack.slackByGroupDescription("alightSlack"))
          .type(TransportModeSlack.SLACK_LIST_OUTPUT_TYPE)
          .dataFetcher(e -> TransportModeSlack.mapToApiList(preferences.transit().alightSlack()))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("maxTransfers")
          .description("Maximum number of transfers returned in a trip plan.")
          .type(Scalars.GraphQLInt)
          .dataFetcher(env -> preferences.transfer().maxTransfers())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("maxAdditionalTransfers")
          .description(
            "Maximum number of transfers allowed in addition to the result with least number of transfers"
          )
          .type(Scalars.GraphQLInt)
          .dataFetcher(env -> preferences.transfer().maxAdditionalTransfers())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("reverseOptimizeOnTheFly")
          .deprecate("NOT IN USE IN OTP2.")
          .type(Scalars.GraphQLBoolean)
          .dataFetcher(e -> false)
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("compactLegsByReversedSearch")
          .deprecate("NOT IN USE IN OTP2.")
          .type(Scalars.GraphQLBoolean)
          .dataFetcher(e -> false)
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("carDecelerationSpeed")
          .description("The deceleration speed of an automobile, in meters per second per second.")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(env -> preferences.car().decelerationSpeed())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("carAccelerationSpeed")
          .description("The acceleration speed of an automobile, in meters per second per second.")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(env -> preferences.car().accelerationSpeed())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("ignoreRealTimeUpdates")
          .description("When true, real-time updates are ignored during this search.")
          .type(Scalars.GraphQLBoolean)
          .dataFetcher(env -> preferences.transit().ignoreRealtimeUpdates())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("includedPlannedCancellations")
          .description(
            "When true, service journeys cancelled in scheduled route data will be included during this search."
          )
          .type(Scalars.GraphQLBoolean)
          .dataFetcher(env -> preferences.transit().includePlannedCancellations())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("disableRemainingWeightHeuristic")
          .description("If true, the remaining weight heuristic is disabled.")
          .type(Scalars.GraphQLBoolean)
          .dataFetcher(env -> false)
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("allowBikeRental")
          .description("")
          .type(Scalars.GraphQLBoolean)
          .deprecate("Rental is specified by modes")
          .dataFetcher(env -> false)
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("parkAndRide")
          .type(Scalars.GraphQLBoolean)
          .deprecate("Parking is specified by modes")
          .dataFetcher(env -> false)
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("kissAndRide")
          .type(Scalars.GraphQLBoolean)
          .deprecate("Parking is specified by modes")
          .dataFetcher(env -> false)
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("debugItineraryFilter")
          .type(Scalars.GraphQLBoolean)
          .deprecate("Use `itineraryFilter.debug` instead.")
          .dataFetcher(env -> preferences.itineraryFilter().debug().debugEnabled())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("onlyTransitTrips")
          .description("Accept only paths that use transit (no street-only paths).")
          .deprecate("This is replaced by modes input object")
          .type(Scalars.GraphQLBoolean)
          .dataFetcher(env -> false)
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("disableAlertFiltering")
          .description("Option to disable the default filtering of GTFS-RT alerts by time.")
          .type(Scalars.GraphQLBoolean)
          .deprecate("This is not supported!")
          .dataFetcher(env -> false)
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("geoIdElevation")
          .description(
            "Whether to apply the ellipsoid->geoid offset to all elevations in the response."
          )
          .type(Scalars.GraphQLBoolean)
          .dataFetcher(env -> preferences.system().geoidElevation())
          .build()
      )
      /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("preferredInterchangePenalty")
                        .description("Whether to apply the ellipsoid->geoid offset to all elevations in the response.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(env -> defaults.preferredInterchangePenalty)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("recommendedInterchangePenalty")
                        .description("Whether to apply the ellipsoid->geoid offset to all elevations in the response.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(env -> defaults.recommendedInterchangePenalty)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("interchangeAllowedPenalty")
                        .description("Whether to apply the ellipsoid->geoid offset to all elevations in the response.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(env -> defaults.interchangeAllowedPenalty)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("noInterchangePenalty")
                        .description("Whether to apply the ellipsoid->geoid offset to all elevations in the response.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(env -> defaults.noInterchangePenalty)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("preTransitReluctance")
                        .description("How much worse driving before and after transit is than riding on transit. Applies to ride and kiss, kiss and ride and park and ride.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(env -> defaults.preTransitReluctance)
                        .build())
                 */
      .build();
  }
}
