package org.opentripplanner.standalone.config;

import org.opentripplanner.model.TransitMode;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.TransferOptimizationRequest;
import org.opentripplanner.standalone.config.sandbox.DataOverlayParametersMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        request.alightSlackForMode = c.asEnumMap("alightSlackForMode", TransitMode.class, NodeAdapter::asInt);
        request.allowedVehicleRentalNetworks = c.asTextSet("allowedVehicleRentalNetworks", dft.allowedVehicleRentalNetworks);
        request.arriveBy = c.asBoolean("arriveBy", dft.arriveBy);
        request.bannedVehicleParkingTags = c.asTextSet("bannedVehicleParkingTags", dft.bannedVehicleParkingTags);
        request.bannedVehicleRentalNetworks = c.asTextSet("bannedVehicleRentalNetworks", dft.bannedVehicleRentalNetworks);
        request.bikeBoardCost = c.asInt("bikeBoardCost", dft.bikeBoardCost);
        request.bikeParkTime = c.asInt("bikeParkTime", dft.bikeParkTime);
        request.bikeParkCost = c.asInt("bikeParkCost", dft.bikeParkCost);
        request.bikeReluctance = c.asDouble("bikeReluctance", dft.bikeReluctance);
        request.vehicleRentalDropoffCost = c.asInt("bikeRentalDropoffCost", dft.vehicleRentalDropoffCost);
        request.vehicleRentalDropoffTime = c.asInt("bikeRentalDropoffTime", dft.vehicleRentalDropoffTime);
        request.vehicleRentalPickupCost = c.asInt("bikeRentalPickupCost", dft.vehicleRentalPickupCost);
        request.vehicleRentalPickupTime = c.asInt("bikeRentalPickupTime", dft.vehicleRentalPickupTime);
        request.bikeSpeed = c.asDouble("bikeSpeed", dft.bikeSpeed);
        request.bikeTriangleSafetyFactor = c.asDouble("bikeTriangleSafetyFactor", dft.bikeTriangleSafetyFactor);
        request.bikeTriangleSlopeFactor = c.asDouble("bikeTriangleSlopeFactor", dft.bikeTriangleSlopeFactor);
        request.bikeTriangleTimeFactor = c.asDouble("bikeTriangleTimeFactor", dft.bikeTriangleTimeFactor);
        request.bikeSwitchTime = c.asInt("bikeSwitchTime", dft.bikeSwitchTime);
        request.bikeSwitchCost = c.asInt("bikeSwitchCost", dft.bikeSwitchCost);
        request.bikeWalkingReluctance = c.asDouble("bikeWalkingReluctance", dft.bikeWalkingReluctance);
        request.bikeWalkingSpeed = c.asDouble("bikeWalkingSpeed", dft.bikeWalkingSpeed);
        request.allowKeepingRentedVehicleAtDestination = c.asBoolean("allowKeepingRentedBicycleAtDestination", dft.allowKeepingRentedVehicleAtDestination);
        request.keepingRentedVehicleAtDestinationCost = c.asDouble("keepingRentedBicycleAtDestinationCost", dft.keepingRentedVehicleAtDestinationCost);
        request.boardSlack = c.asInt("boardSlack", dft.boardSlack);
        request.boardSlackForMode = c.asEnumMap("boardSlackForMode", TransitMode.class, NodeAdapter::asInt);
        request.maxAccessEgressDurationSecondsForMode = c.asEnumMap("maxAccessEgressDurationSecondsForMode", StreetMode.class, NodeAdapter::asDouble);
        request.carAccelerationSpeed = c.asDouble("carAccelerationSpeed", dft.carAccelerationSpeed);
        request.carDecelerationSpeed = c.asDouble("carDecelerationSpeed", dft.carDecelerationSpeed);
        request.carDropoffTime = c.asInt("carDropoffTime", dft.carDropoffTime);
        request.carParkCost = c.asInt("carParkCost", dft.carParkCost);
        request.carParkTime = c.asInt("carParkTime", dft.carParkTime);
        request.carPickupCost = c.asInt("carPickupCost", dft.carPickupCost);
        request.carPickupTime = c.asInt("carPickupTime", dft.carPickupTime);
        request.carReluctance = c.asDouble("carReluctance", dft.carReluctance);
        request.carSpeed = c.asDouble("carSpeed", dft.carSpeed);
        request.itineraryFilters = ItineraryFiltersMapper.map(c.path("itineraryFilters"));
        request.disableAlertFiltering = c.asBoolean("disableAlertFiltering", dft.disableAlertFiltering);
        request.disableRemainingWeightHeuristic = c.asBoolean("disableRemainingWeightHeuristic", dft.disableRemainingWeightHeuristic);
        request.elevatorBoardCost = c.asInt("elevatorBoardCost", dft.elevatorBoardCost);
        request.elevatorBoardTime = c.asInt("elevatorBoardTime", dft.elevatorBoardTime);
        request.elevatorHopCost = c.asInt("elevatorHopCost", dft.elevatorHopCost);
        request.elevatorHopTime = c.asInt("elevatorHopTime", dft.elevatorHopTime);
        request.geoidElevation = c.asBoolean("geoidElevation", dft.geoidElevation);
        request.ignoreRealtimeUpdates = c.asBoolean("ignoreRealtimeUpdates", dft.ignoreRealtimeUpdates);
        request.carPickup = c.asBoolean("kissAndRide", dft.carPickup);
        request.locale = c.asLocale("locale", dft.locale);
        // 'maxTransfers' is configured in the Raptor tuning parameters, not here
        request.maxDirectStreetDurationSeconds = c.asDouble("maxDirectStreetDurationSeconds", dft.maxDirectStreetDurationSeconds);
        request.maxJourneyDuration = c.asDuration("maxJourneyDuration", dft.maxJourneyDuration);
        request.maxWheelchairSlope = c.asDouble("maxWheelchairSlope", dft.maxWheelchairSlope); // ADA max wheelchair ramp slope is a good default.
        request.modes = c.asRequestModes("modes", RequestModes.defaultRequestModes);
        request.nonpreferredTransferCost = c.asInt("nonpreferredTransferPenalty", dft.nonpreferredTransferCost);
        request.numItineraries = c.asInt("numItineraries", dft.numItineraries);
        request.onlyTransitTrips = c.asBoolean("onlyTransitTrips", dft.onlyTransitTrips);
        request.bicycleOptimizeType = c.asEnum("optimize", dft.bicycleOptimizeType);
        request.otherThanPreferredRoutesPenalty = c.asInt("otherThanPreferredRoutesPenalty", dft.otherThanPreferredRoutesPenalty);
        request.parkAndRide = c.asBoolean("parkAndRide", dft.parkAndRide);
        request.pathComparator = c.asText("pathComparator", dft.pathComparator);
        request.requiredVehicleParkingTags = c.asTextSet("requiredVehicleParkingTags", dft.requiredVehicleParkingTags);
        request.showIntermediateStops = c.asBoolean("showIntermediateStops", dft.showIntermediateStops);
        request.stairsReluctance = c.asDouble("stairsReluctance", dft.stairsReluctance);
        request.stairsTimeFactor = c.asDouble("stairsTimeFactor", dft.stairsTimeFactor);
        request.startingTransitTripId = c.asFeedScopedId("startingTransitTripId", dft.startingTransitTripId);
        request.transferCost = c.asInt("transferPenalty", dft.transferCost);
        request.transferSlack = c.asInt("transferSlack", dft.transferSlack);
        request.setTransitReluctanceForMode(c.asEnumMap("transitReluctanceForMode", TransitMode.class, NodeAdapter::asDouble));
        request.turnReluctance = c.asDouble("turnReluctance", dft.turnReluctance);
        request.useVehicleRentalAvailabilityInformation = c.asBoolean("useBikeRentalAvailabilityInformation", dft.useVehicleRentalAvailabilityInformation);
        request.useVehicleParkingAvailabilityInformation = c.asBoolean("useVehicleParkingAvailabilityInformation", dft.useVehicleParkingAvailabilityInformation);
        request.useUnpreferredRoutesPenalty = c.asInt("useUnpreferredRoutesPenalty", dft.useUnpreferredRoutesPenalty);
        request.vehicleRental = c.asBoolean("allowBikeRental", dft.vehicleRental);
        request.waitAtBeginningFactor = c.asDouble("waitAtBeginningFactor", dft.waitAtBeginningFactor);
        request.waitReluctance = c.asDouble("waitReluctance", dft.waitReluctance);
        request.walkBoardCost = c.asInt("walkBoardCost", dft.walkBoardCost);
        request.walkReluctance = c.asDouble("walkReluctance", dft.walkReluctance);
        request.walkSpeed = c.asDouble("walkSpeed", dft.walkSpeed);
        request.wheelchairAccessible = c.asBoolean("wheelchairAccessible", dft.wheelchairAccessible);

        mapTransferOptimization(
            (TransferOptimizationRequest)request.transferOptimization,
            c.path("transferOptimization")
        );

        request.dataOverlay = DataOverlayParametersMapper.map(c.path("dataOverlay"));

        return request;
    }

    private static void mapTransferOptimization(TransferOptimizationRequest p, NodeAdapter c) {
        p.optimizeTransferWaitTime = c.asBoolean(
                "optimizeTransferWaitTime", p.optimizeTransferWaitTime
        );
        p.minSafeWaitTimeFactor = c.asDouble("minSafeWaitTimeFactor", p.minSafeWaitTimeFactor);
        p.backTravelWaitTimeFactor = c.asDouble(
                "backTravelWaitTimeFactor", p.backTravelWaitTimeFactor
        );
        p.extraStopBoardAlightCostsFactor = c.asDouble(
                "extraStopBoardAlightCostsFactor", p.extraStopBoardAlightCostsFactor
        );
    }
}
