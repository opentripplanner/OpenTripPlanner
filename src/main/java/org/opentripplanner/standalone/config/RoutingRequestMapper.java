package org.opentripplanner.standalone.config;

import static org.opentripplanner.standalone.config.WheelchairAccessibilityRequestMapper.mapAccessibilityRequest;

import org.apache.commons.lang3.tuple.Pair;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.TransferOptimizationRequest;
import org.opentripplanner.routing.api.request.refactor.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.refactor.request.NewRouteRequest;
import org.opentripplanner.standalone.config.sandbox.DataOverlayParametersMapper;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutingRequestMapper {

  private static final Logger LOG = LoggerFactory.getLogger(RoutingRequestMapper.class);

  public static Pair<NewRouteRequest, RoutingPreferences> mapRoutingRequest(NodeAdapter c) {
    var dft = new NewRouteRequest();
    var pref = new RoutingPreferences();

    if (c.isEmpty()) {
      return Pair.of(dft, pref);
    }

    LOG.debug("Loading default routing parameters from JSON.");
    var request = new NewRouteRequest();
    var preferences = new RoutingPreferences();
    var vehicleRental = request.journey().rental();
    var vehicleParking = request.journey().parking();

    // Keep this alphabetically sorted so it is easy to check if a parameter is missing from the
    // mapping or duplicate exist.
    preferences.transfer().setSlack(c.asInt("alightSlack", preferences.transfer().slack()));
    preferences
      .transit()
      .setAlightSlackForMode(
        c.asEnumMap("alightSlackForMode", TransitMode.class, NodeAdapter::asInt)
      );
    vehicleRental.setAllowedNetworks(
      c.asTextSet("allowedVehicleRentalNetworks", vehicleRental.allowedNetworks())
    );
    request.setArriveBy(c.asBoolean("arriveBy", dft.arriveBy()));
    vehicleParking.setBannedTags(
      c.asTextSet("bannedVehicleParkingTags", vehicleParking.bannedTags())
    );
    vehicleRental.setBannedNetworks(
      c.asTextSet("bannedVehicleRentalNetworks", vehicleRental.bannedNetworks())
    );
    preferences.bike().setBoardCost(c.asInt("bikeBoardCost", preferences.bike().boardCost()));
    preferences.bike().setParkTime(c.asInt("bikeParkTime", preferences.bike().parkTime()));
    preferences.bike().setParkCost(c.asInt("bikeParkCost", preferences.bike().parkCost()));
    preferences.bike().setReluctance(c.asDouble("bikeReluctance", preferences.bike().reluctance()));
    preferences
      .rental()
      .setDropoffCost(c.asInt("bikeRentalDropoffCost", preferences.rental().dropoffCost()));
    preferences
      .rental()
      .setDropoffTime(c.asInt("bikeRentalDropoffTime", preferences.rental().dropoffTime()));
    preferences
      .rental()
      .setPickupCost(c.asInt("bikeRentalPickupCost", preferences.rental().pickupCost()));
    preferences
      .rental()
      .setPickupTime(c.asInt("bikeRentalPickupTime", preferences.rental().pickupTime()));
    preferences.bike().setSpeed(c.asDouble("bikeSpeed", preferences.bike().speed()));
    preferences
      .bike()
      .setTriangleSafetyFactor(
        c.asDouble("bikeTriangleSafetyFactor", preferences.bike().triangleSafetyFactor())
      );
    preferences
      .bike()
      .setTriangleSlopeFactor(
        c.asDouble("bikeTriangleSlopeFactor", preferences.bike().triangleSlopeFactor())
      );
    preferences
      .bike()
      .setTriangleTimeFactor(
        c.asDouble("bikeTriangleTimeFactor", preferences.bike().triangleTimeFactor())
      );
    preferences.bike().setSwitchTime(c.asInt("bikeSwitchTime", preferences.bike().switchTime()));
    preferences.bike().setSwitchCost(c.asInt("bikeSwitchCost", preferences.bike().switchCost()));
    preferences
      .bike()
      .setWalkingReluctance(
        c.asDouble("bikeWalkingReluctance", preferences.bike().walkingReluctance())
      );
    preferences
      .bike()
      .setWalkingSpeed(c.asDouble("bikeWalkingSpeed", preferences.bike().walkingSpeed()));
    preferences
      .rental()
      .setAllowKeepingRentedVehicleAtDestination(
        c.asBoolean(
          "allowKeepingRentedBicycleAtDestination",
          preferences.rental().allowKeepingRentedVehicleAtDestination()
        )
      );
    preferences
      .rental()
      .setKeepingVehicleAtDestinationCost(
        c.asDouble(
          "keepingRentedBicycleAtDestinationCost",
          preferences.rental().keepingVehicleAtDestinationCost()
        )
      );
    preferences.transit().setBoardSlack(c.asInt("boardSlack", preferences.transit().boardSlack()));
    preferences
      .transit()
      .setBoardSlackForMode(
        c.asEnumMap("boardSlackForMode", TransitMode.class, NodeAdapter::asInt)
      );
    preferences
      .street()
      .setMaxAccessEgressDurationForMode(
        c.asEnumMap("maxAccessEgressDurationForMode", StreetMode.class, NodeAdapter::asDuration)
      );
    preferences
      .car()
      .setAccelerationSpeed(
        c.asDouble("carAccelerationSpeed", preferences.car().accelerationSpeed())
      );
    preferences
      .car()
      .setDecelerationSpeed(
        c.asDouble("carDecelerationSpeed", preferences.car().decelerationSpeed())
      );
    preferences.car().setDropoffTime(c.asInt("carDropoffTime", preferences.car().dropoffTime()));
    preferences.car().setParkCost(c.asInt("carParkCost", preferences.car().parkCost()));
    preferences.car().setParkTime(c.asInt("carParkTime", preferences.car().parkTime()));
    preferences.car().setPickupCost(c.asInt("carPickupCost", preferences.car().pickupCost()));
    preferences.car().setPickupTime(c.asInt("carPickupTime", preferences.car().pickupTime()));
    preferences.car().setReluctance(c.asDouble("carReluctance", preferences.car().reluctance()));
    preferences.car().setSpeed(c.asDouble("carSpeed", preferences.car().speed()));

    preferences
      .system()
      .setItineraryFilters(ItineraryFiltersMapper.map(c.path("itineraryFilters")));
    preferences
      .system()
      .setDisableAlertFiltering(
        c.asBoolean("disableAlertFiltering", preferences.system().disableAlertFiltering())
      );
    preferences
      .street()
      .setElevatorBoardCost(c.asInt("elevatorBoardCost", preferences.street().elevatorBoardCost()));
    preferences
      .street()
      .setElevatorBoardTime(c.asInt("elevatorBoardTime", preferences.street().elevatorBoardTime()));
    preferences
      .street()
      .setElevatorHopCost(c.asInt("elevatorHopCost", preferences.street().elevatorHopCost()));
    preferences
      .street()
      .setElevatorHopTime(c.asInt("elevatorHopTime", preferences.street().elevatorHopTime()));
    preferences
      .system()
      .setGeoidElevation(c.asBoolean("geoidElevation", preferences.system().geoidElevation()));
    preferences
      .transit()
      .setIgnoreRealtimeUpdates(
        c.asBoolean("ignoreRealtimeUpdates", preferences.transit().ignoreRealtimeUpdates())
      );
    preferences.car().setAllowPickup(c.asBoolean("kissAndRide", preferences.car().allowPickup()));

    request.setLocale(c.asLocale("locale", dft.locale()));
    // 'maxTransfers' is configured in the Raptor tuning parameters, not here
    preferences
      .street()
      .setMaxDirectDuration(
        c.asDuration("maxDirectStreetDuration", preferences.street().maxDirectDuration())
      );
    preferences
      .street()
      .setMaxDirectDurationForMode(
        c.asEnumMap("maxDirectStreetDurationForMode", StreetMode.class, NodeAdapter::asDuration)
      );

    preferences
      .system()
      .setMaxJourneyDuration(
        c.asDuration("maxJourneyDuration", preferences.system().maxJourneyDuration())
      );

    // TODO: 2022-08-23 Verify that this is right
    var journeyRequest = c.asJourneyRequest("modes", dft.journey());
    request.journey().access().setMode(journeyRequest.access().mode());
    request.journey().direct().setMode(journeyRequest.direct().mode());
    request.journey().egress().setMode(journeyRequest.egress().mode());
    request.journey().setStreetSubRequestModes(journeyRequest.streetSubRequestModes());
    request.journey().transfer().setMode(journeyRequest.transfer().mode());
    request.journey().transit().setModes(journeyRequest.transit().modes());

    preferences
      .transfer()
      .setNonpreferredCost(
        c.asInt("nonpreferredTransferPenalty", preferences.transfer().nonpreferredCost())
      );
    request.setNumItineraries(c.asInt("numItineraries", dft.numItineraries()));
    request
      .journey()
      .setOnlyTransitTrips(c.asBoolean("onlyTransitTrips", request.journey().onlyTransitTrips()));
    preferences.bike().setOptimizeType(c.asEnum("optimize", preferences.bike().optimizeType()));
    preferences
      .transit()
      .setOtherThanPreferredRoutesPenalty(
        c.asInt(
          "otherThanPreferredRoutesPenalty",
          preferences.transit().otherThanPreferredRoutesPenalty()
        )
      );
    preferences.bike().setParkAndRide(c.asBoolean("parkAndRide", preferences.bike().parkAndRide()));
    preferences
      .street()
      .setPathComparator(c.asText("pathComparator", preferences.street().pathComparator()));
    vehicleParking.setRequiredTags(
      c.asTextSet("requiredVehicleParkingTags", vehicleParking.requiredTags())
    );

    preferences
      .system()
      .setShowIntermediateStops(
        c.asBoolean("showIntermediateStops", preferences.system().showIntermediateStops())
      );
    preferences
      .walk()
      .setStairsReluctance(c.asDouble("stairsReluctance", preferences.walk().stairsReluctance()));
    preferences
      .walk()
      .setStairsTimeFactor(c.asDouble("stairsTimeFactor", preferences.walk().stairsTimeFactor()));
    request.setStartingTransitTripId(
      c.asFeedScopedId("startingTransitTripId", request.startingTransitTripId())
    );
    preferences.transfer().setCost(c.asInt("transferPenalty", preferences.transfer().cost()));
    preferences.transfer().setSlack(c.asInt("transferSlack", preferences.transfer().slack()));
    preferences
      .transit()
      .setReluctanceForMode(
        c.asEnumMap("transitReluctanceForMode", TransitMode.class, NodeAdapter::asDouble)
      );
    preferences
      .street()
      .setTurnReluctance(c.asDouble("turnReluctance", preferences.street().turnReluctance()));
    preferences
      .rental()
      .setUseAvailabilityInformation(
        c.asBoolean(
          "useBikeRentalAvailabilityInformation",
          preferences.rental().useAvailabilityInformation()
        )
      );
    preferences
      .parking()
      .setUseAvailabilityInformation(
        c.asBoolean(
          "useVehicleParkingAvailabilityInformation",
          preferences.parking().useAvailabilityInformation()
        )
      );
    preferences
      .transit()
      .setUnpreferredRouteCost(
        c.asLinearFunction("unpreferredRouteCost", preferences.transit().unpreferredRouteCost())
      );
    preferences.rental().setAllow(c.asBoolean("allowBikeRental", preferences.rental().allow()));
    preferences
      .transfer()
      .setWaitAtBeginningFactor(
        c.asDouble("waitAtBeginningFactor", preferences.transfer().waitAtBeginningFactor())
      );
    preferences
      .transfer()
      .setWaitReluctance(
        c.asDouble("waitReluctance", preferences.transfer().waitAtBeginningFactor())
      );
    preferences.walk().setBoardCost(c.asInt("walkBoardCost", preferences.walk().boardCost()));
    preferences.walk().setReluctance(c.asDouble("walkReluctance", preferences.walk().reluctance()));
    preferences.walk().setSpeed(c.asDouble("walkSpeed", preferences.walk().speed()));
    preferences
      .walk()
      .setSafetyFactor(c.asDouble("walkSafetyFactor", preferences.walk().safetyFactor()));

    preferences
      .wheelchair()
      .setAccessibility(mapAccessibilityRequest(c.path("wheelchairAccessibility")));

    mapTransferOptimization(
      (TransferOptimizationRequest) preferences.transfer().optimization(),
      c.path("transferOptimization")
    );

    preferences.system().setDataOverlay(DataOverlayParametersMapper.map(c.path("dataOverlay")));

    request
      .journey()
      .transit()
      .setUnpreferredRoutes(
        c
          .path("unpreferred")
          .asRouteMatcher("routes", request.journey().transit().unpreferredRoutes())
      );

    request
      .journeyRequest()
      .transit()
      .setUnpreferredAgencies(
        c
          .path("unpreferred")
          .asRouteMatcher("agencies", request.journeyRequest().transit().unpreferredRoutes())
      );

    return Pair.of(request, preferences);
  }

  private static void mapTransferOptimization(TransferOptimizationRequest p, NodeAdapter c) {
    p.optimizeTransferWaitTime =
      c.asBoolean("optimizeTransferWaitTime", p.optimizeTransferWaitTime);
    p.minSafeWaitTimeFactor = c.asDouble("minSafeWaitTimeFactor", p.minSafeWaitTimeFactor);
    p.backTravelWaitTimeFactor = c.asDouble("backTravelWaitTimeFactor", p.backTravelWaitTimeFactor);
    p.extraStopBoardAlightCostsFactor =
      c.asDouble("extraStopBoardAlightCostsFactor", p.extraStopBoardAlightCostsFactor);
  }
}
