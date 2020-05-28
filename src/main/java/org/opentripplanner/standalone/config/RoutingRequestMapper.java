package org.opentripplanner.standalone.config;

import org.opentripplanner.model.TransitMode;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.api.request.StreetMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;

public class RoutingRequestMapper {

    private static final Logger LOG = LoggerFactory.getLogger(RoutingRequestMapper.class);

    public static RoutingRequest mapRoutingRequest(NodeAdapter c) {
        RoutingRequest dft = new RoutingRequest();

        if (c.isEmpty()) { return dft; }

        LOG.debug("Loading default routing parameters from JSON.");
        RoutingRequest request = new RoutingRequest();

        // Keep this alphabetically sorted so it is easy to check if a parameter is missing from the
        // mapping or duplicate exist.
        request.alightSlack = c.asInt("alightSlack", dft.alightSlack);
        request.alightSlackForMode = c.asEnumMap("alightSlackForMode", TraverseMode.class, NodeAdapter::asInt);
        request.bikeRental = c.asBoolean("allowBikeRental", dft.bikeRental);
        request.arriveBy = c.asBoolean("arriveBy", dft.arriveBy);
        request.bikeBoardCost = c.asInt("bikeBoardCost", dft.bikeBoardCost);
        request.bikeParkAndRide = c.asBoolean("bikeParkAndRide", dft.bikeParkAndRide);
        request.bikeParkTime = c.asInt("bikeParkTime", dft.bikeParkTime);
        request.bikeParkCost = c.asInt("bikeParkCost", dft.bikeParkCost);
        request.bikeRentalDropoffCost = c.asInt("bikeRentalDropoffCost", dft.bikeRentalDropoffCost);
        request.bikeRentalDropoffTime = c.asInt("bikeRentalDropoffTime", dft.bikeRentalDropoffTime);
        request.bikeRentalPickupCost = c.asInt("bikeRentalPickupCost", dft.bikeRentalPickupCost);
        request.bikeRentalPickupTime = c.asInt("bikeRentalPickupTime", dft.bikeRentalPickupTime);
        request.bikeSpeed = c.asDouble("bikeSpeed", dft.bikeSpeed);
        request.bikeTriangleSafetyFactor = c.asDouble("bikeTriangleSafetyFactor", dft.bikeTriangleSafetyFactor);
        request.bikeTriangleSlopeFactor = c.asDouble("bikeTriangleSlopeFactor", dft.bikeTriangleSlopeFactor);
        request.bikeTriangleTimeFactor = c.asDouble("bikeTriangleTimeFactor", dft.bikeTriangleTimeFactor);
        request.bikeSwitchTime = c.asInt("bikeSwitchTime", dft.bikeSwitchTime);
        request.bikeSwitchCost = c.asInt("bikeSwitchCost", dft.bikeSwitchCost);
        request.boardSlack = c.asInt("boardSlack", dft.boardSlack);
        request.boardSlackForMode = c.asEnumMap("boardSlackForMode", TraverseMode.class, NodeAdapter::asInt);
        request.debugItineraryFilter = c.asBoolean("debugItineraryFilter", dft.debugItineraryFilter);
        request.carAccelerationSpeed = c.asDouble("carAccelerationSpeed", dft.carAccelerationSpeed);
        request.carDecelerationSpeed = c.asDouble("carDecelerationSpeed", dft.carDecelerationSpeed);
        request.carDropoffTime = c.asInt("carDropoffTime", dft.carDropoffTime);
        request.carSpeed = c.asDouble("carSpeed", dft.carSpeed);
        request.debugItineraryFilter = c.asBoolean("debugItineraryFilter", dft.debugItineraryFilter);
        request.disableAlertFiltering = c.asBoolean("disableAlertFiltering", dft.disableAlertFiltering);
        request.disableRemainingWeightHeuristic = c.asBoolean("disableRemainingWeightHeuristic", dft.disableRemainingWeightHeuristic);
        request.driveOnRight = c.asBoolean("driveOnRight", dft.driveOnRight);
        request.elevatorBoardCost = c.asInt("elevatorBoardCost", dft.elevatorBoardCost);
        request.elevatorBoardTime = c.asInt("elevatorBoardTime", dft.elevatorBoardTime);
        request.elevatorHopCost = c.asInt("elevatorHopCost", dft.elevatorHopCost);
        request.elevatorHopTime = c.asInt("elevatorHopTime", dft.elevatorHopTime);
        request.geoidElevation = c.asBoolean("geoidElevation", dft.geoidElevation);
        request.ignoreRealtimeUpdates = c.asBoolean("ignoreRealtimeUpdates", dft.ignoreRealtimeUpdates);
        request.carPickup = c.asBoolean("kissAndRide", dft.carPickup);
        request.locale = c.asLocale("locale", dft.locale);
        request.maxHours = c.asDouble("maxHours", dft.maxHours);
        request.maxPreTransitTime = c.asInt("maxPreTransitTime", dft.maxPreTransitTime);
        request.maxTransferWalkDistance = c.asDouble("maxTransferWalkDistance", dft.maxTransferWalkDistance);
        // 'maxTransfers' is configured in the Raptor tuning parameters, not here
        request.maxWalkDistance = c.asDouble("maxWalkDistance", dft.maxWalkDistance);
        request.maxWeight = c.asDouble("maxWeight", dft.maxWeight);
        request.maxWheelchairSlope = c.asDouble("maxWheelchairSlope", dft.maxWheelchairSlope); // ADA max wheelchair ramp slope is a good default.
        request.modes = new RequestModes(StreetMode.WALK, StreetMode.WALK, StreetMode.WALK, new HashSet<>(
            Arrays.asList(TransitMode.values()))); // TODO Map default modes from config
        request.nonpreferredTransferCost = c.asInt("nonpreferredTransferPenalty", dft.nonpreferredTransferCost);
        request.numItineraries = c.asInt("numItineraries", dft.numItineraries);
        request.onlyTransitTrips = c.asBoolean("onlyTransitTrips", dft.onlyTransitTrips);
        request.optimize = c.asEnum("optimize", dft.optimize);
        request.otherThanPreferredRoutesPenalty = c.asInt("otherThanPreferredRoutesPenalty", dft.otherThanPreferredRoutesPenalty);
        request.parkAndRide = c.asBoolean("parkAndRide", dft.parkAndRide);
        request.pathComparator = c.asText("pathComparator", dft.pathComparator);
        request.showIntermediateStops = c.asBoolean("showIntermediateStops", dft.showIntermediateStops);
        request.stairsReluctance = c.asDouble("stairsReluctance", dft.stairsReluctance);
        request.startingTransitTripId = c.asFeedScopedId("startingTransitTripId", dft.startingTransitTripId);
        request.transferCost = c.asInt("transferPenalty", dft.transferCost);
        request.transferSlack = c.asInt("transferSlack", dft.transferSlack);
        request.turnReluctance = c.asDouble("turnReluctance", dft.turnReluctance);
        request.useBikeRentalAvailabilityInformation = c.asBoolean("useBikeRentalAvailabilityInformation", dft.useBikeRentalAvailabilityInformation);
        request.useRequestedDateTimeInMaxHours = c.asBoolean("useRequestedDateTimeInMaxHours", dft.useRequestedDateTimeInMaxHours);
        request.useUnpreferredRoutesPenalty = c.asInt("useUnpreferredRoutesPenalty", dft.useUnpreferredRoutesPenalty);
        request.waitAtBeginningFactor = c.asDouble("waitAtBeginningFactor", dft.waitAtBeginningFactor);
        request.waitReluctance = c.asDouble("waitReluctance", dft.waitReluctance);
        request.walkBoardCost = c.asInt("walkBoardCost", dft.walkBoardCost);
        request.walkReluctance = c.asDouble("walkReluctance", dft.walkReluctance);
        request.walkSpeed = c.asDouble("walkSpeed", dft.walkSpeed);
        request.walkingBike = c.asBoolean("walkingBike", dft.walkingBike);
        request.wheelchairAccessible = c.asBoolean("wheelchairAccessible", dft.wheelchairAccessible);
        request.worstTime = c.asLong("worstTime", dft.worstTime);

        return request;
    }
}
