package org.opentripplanner.ext.transmodelapi.model;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.routing.api.request.RoutingRequest;

public class DefaultRoutingRequest {
    public final RoutingRequest request;
    public final GraphQLObjectType graphQLType;

    public DefaultRoutingRequest(RoutingRequest request) {
        this.request = request;
        this.graphQLType = createGraphQLType();
    }

    private GraphQLObjectType createGraphQLType() {
        return GraphQLObjectType
                .newObject()
                .name("RoutingParameters")
                .description("The default parameters used in travel searches.")
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("walkSpeed")
                        .description("Max walk speed along streets, in meters per second")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(env -> request.walkSpeed)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("bikeSpeed")
                        .description("Max bike speed along streets, in meters per second")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(env -> request.bikeSpeed)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("carSpeed")
                        .description("Max car speed along streets, in meters per second")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(env -> request.carSpeed)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("maxWalkDistance")
                        .description(
                                "The maximum distance (in meters) the user is willing to walk for access/egress legs.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(env -> request.maxWalkDistance)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("maxTransferWalkDistance")
                        .description(
                                "The maximum distance (in meters) the user is willing to walk for transfer legs.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(env -> request.maxTransferWalkDistance)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("maxPreTransitTime")
                        .description(
                                "The maximum time (in seconds) of pre-transit travel when using drive-to-transit (park and ride or kiss and ride).")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(env -> request.maxPreTransitTime)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("wheelChairAccessible")
                        .description("Whether the trip must be wheelchair accessible.")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(env -> request.wheelchairAccessible)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("numItineraries")
                        .description("The maximum number of itineraries to return.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(env -> request.numItineraries)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("maxSlope")
                        .description("The maximum slope of streets for wheelchair trips.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(env -> request.maxWheelchairSlope)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("showIntermediateStops")
                        .description(
                                "Whether the planner should return intermediate stops lists for transit legs.")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(env -> request.showIntermediateStops)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("transferPenalty")
                        .description(
                                "An extra penalty added on transfers (i.e. all boardings except the first one).")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(env -> request.transferCost)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("walkReluctance")
                        .description(
                                "A multiplier for how bad walking is, compared to being in transit for equal lengths of time.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(env -> request.walkReluctance)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("stairsReluctance")
                        .description("Used instead of walkReluctance for stairs.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(env -> request.stairsReluctance)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("turnReluctance")
                        .description("Multiplicative factor on expected turning time.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(env -> request.turnReluctance)
                        .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("walkOnStreetReluctance")
                        .description("How much more reluctant is the user to walk on streets with car traffic allowed.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(env -> defaults.walkOnStreetReluctance)
                        .build())
                 */
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("elevatorBoardTime")
                        .description("How long does it take to get on an elevator, on average.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(env -> request.elevatorBoardTime)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("elevatorBoardCost")
                        .description("What is the cost of boarding a elevator?")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(env -> request.elevatorBoardCost)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("elevatorHopTime")
                        .description("How long does it take to advance one floor on an elevator?")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(env -> request.elevatorHopTime)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("elevatorHopCost")
                        .description("What is the cost of travelling one floor on an elevator?")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(env -> request.elevatorHopCost)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("bikeRentalPickupTime")
                        .description("Time to rent a bike.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(env -> request.bikeRentalPickupTime)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("bikeRentalPickupCost")
                        .description("Cost to rent a bike.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(env -> request.bikeRentalPickupCost)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("bikeRentalDropOffTime")
                        .description("Time to drop-off a rented bike.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(env -> request.bikeRentalDropoffTime)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("bikeRentalDropOffCost")
                        .description("Cost to drop-off a rented bike.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(env -> request.bikeRentalDropoffCost)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("bikeParkTime")
                        .description("Time to park a bike.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(env -> request.bikeParkTime)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("bikeParkCost")
                        .description("Cost to park a bike.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(env -> request.bikeParkCost)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("carDropOffTime")
                        .description(
                                "Time to park a car in a park and ride, w/o taking into account driving and walking cost.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(env -> request.carDropoffTime)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("waitReluctance")
                        .description(
                                "How much worse is waiting for a transit vehicle than being on a transit vehicle, as a multiplier.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(env -> request.waitReluctance)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("waitAtBeginningFactor")
                        .description(
                                "How much less bad is waiting at the beginning of the trip (replaces waitReluctance on the first boarding).")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(env -> request.waitAtBeginningFactor)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("walkBoardCost")
                        .description(
                                "This prevents unnecessary transfers by adding a cost for boarding a vehicle.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(env -> request.walkBoardCost)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("bikeBoardCost")
                        .description(
                                "Separate cost for boarding a vehicle with a bicycle, which is more difficult than on foot.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(env -> request.bikeBoardCost)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("otherThanPreferredRoutesPenalty")
                        .description(
                                "Penalty added for using every route that is not preferred if user set any route as preferred. We return number of seconds that we are willing to wait for preferred route.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(env -> request.otherThanPreferredRoutesPenalty)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("transferSlack")
                        .description(
                                "A global minimum transfer time (in seconds) that specifies the minimum amount of time that must pass between exiting one transit vehicle and boarding another.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(env -> request.transferSlack)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("boardSlackDefault")
                        .description(TransportModeSlack.boardSlackDescription("boardSlackList")).type(Scalars.GraphQLInt)
                        .dataFetcher(e -> ((RoutingRequest) e.getSource()).boardSlack).build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("boardSlackList")
                        .description(TransportModeSlack.slackByGroupDescription("boardSlack"))
                        .type(TransportModeSlack.SLACK_LIST_OUTPUT_TYPE)
                        .dataFetcher(e -> TransportModeSlack.mapToApiList(((RoutingRequest) e.getSource()).boardSlackForMode))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("alightSlackDefault")
                        .description(TransportModeSlack.alightSlackDescription("alightSlackList")).type(Scalars.GraphQLInt)
                        .dataFetcher(e -> ((RoutingRequest) e.getSource()).alightSlack).build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("alightSlackList")
                        .description(TransportModeSlack.slackByGroupDescription("alightSlack"))
                        .type(TransportModeSlack.SLACK_LIST_OUTPUT_TYPE)
                        .dataFetcher(e -> TransportModeSlack.mapToApiList(((RoutingRequest) e.getSource()).alightSlackForMode))
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("maxTransfers")
                        .description("Maximum number of transfers returned in a trip plan.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(env -> request.maxTransfers)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("reverseOptimizeOnTheFly")
                        .description("DEPRECATED - NOT IN USE IN OTP2.")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(e -> false).build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("compactLegsByReversedSearch")
                        .description("DEPRECATED - NOT IN USE IN OTP2.")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(e -> false).build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("carDecelerationSpeed")
                        .description(
                                "The deceleration speed of an automobile, in meters per second per second.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(env -> request.carDecelerationSpeed)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("carAccelerationSpeed")
                        .description(
                                "The acceleration speed of an automobile, in meters per second per second.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(env -> request.carAccelerationSpeed)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("ignoreRealTimeUpdates")
                        .description("When true, realtime updates are ignored during this search.")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(env -> request.ignoreRealtimeUpdates)
                        .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("includedPlannedCancellations")
                        .description("When true, service journeys cancelled in scheduled route data will be included during this search.")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(env -> defaults.includePlannedCancellations)
                        .build())
                 */
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("disableRemainingWeightHeuristic")
                        .description("If true, the remaining weight heuristic is disabled.")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(env -> request.disableRemainingWeightHeuristic)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("allowBikeRental")
                        .description("")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(env -> request.bikeRental)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("bikeParkAndRide")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(env -> request.bikeParkAndRide)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("parkAndRide")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(env -> request.parkAndRide)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("kissAndRide")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(env -> request.carPickup)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("debugItineraryFilter")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(env -> request.debugItineraryFilter)
                        .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("rideAndKiss")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(env -> defaults.rideAndKiss)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("useTraffic")
                        .description("Should traffic congestion be considered when driving?")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(env -> defaults.useTraffic)
                        .build())
                 */
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("onlyTransitTrips")
                        .description("Accept only paths that use transit (no street-only paths).")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(env -> request.onlyTransitTrips)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("disableAlertFiltering")
                        .description(
                                "Option to disable the default filtering of GTFS-RT alerts by time.")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(env -> request.disableAlertFiltering)
                        .build())
                .field(GraphQLFieldDefinition
                        .newFieldDefinition()
                        .name("geoIdElevation")
                        .description(
                                "Whether to apply the ellipsoid->geoid offset to all elevations in the response.")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(env -> request.geoidElevation)
                        .build())
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
