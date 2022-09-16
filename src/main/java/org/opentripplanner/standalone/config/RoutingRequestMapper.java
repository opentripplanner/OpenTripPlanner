package org.opentripplanner.standalone.config;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.opentripplanner.standalone.config.WheelchairAccessibilityRequestMapper.mapAccessibilityRequest;

import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.preference.TransferOptimizationPreferences;
import org.opentripplanner.routing.api.request.request.VehicleParkingRequest;
import org.opentripplanner.routing.api.request.request.VehicleRentalRequest;
import org.opentripplanner.standalone.config.sandbox.DataOverlayParametersMapper;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutingRequestMapper {

  private static final Logger LOG = LoggerFactory.getLogger(RoutingRequestMapper.class);

  public static RouteRequest mapRoutingRequest(NodeAdapter c) {
    RouteRequest dft = new RouteRequest();

    if (c.isEmpty()) {
      return dft;
    }

    LOG.debug("Loading default routing parameters from JSON.");
    RouteRequest request = new RouteRequest();
    RoutingPreferences preferences = request.preferences();
    VehicleRentalRequest vehicleRental = request.journey().rental();
    VehicleParkingRequest vehicleParking = request.journey().parking();

    // Keep this alphabetically sorted so it is easy to check if a parameter is missing from the
    // mapping or duplicate exist.

    preferences
      .transit()
      .initAlightSlack(
        c.asDuration2("alightSlack", preferences.transit().alightSlack().defaultValue(), SECONDS),
        c.asEnumMap("alightSlackForMode", TransitMode.class, (a, n) -> a.asDuration2(n, SECONDS))
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
      .initOptimizeTriangle(
        c.asDouble("bikeTriangleTimeFactor", preferences.bike().optimizeTriangle().time()),
        c.asDouble("bikeTriangleSlopeFactor", preferences.bike().optimizeTriangle().slope()),
        c.asDouble("bikeTriangleSafetyFactor", preferences.bike().optimizeTriangle().safety())
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
    request
      .journey()
      .rental()
      .setAllowArrivingInRentedVehicleAtDestination(
        c.asBoolean(
          "allowKeepingRentedBicycleAtDestination",
          request.journey().rental().allowArrivingInRentedVehicleAtDestination()
        )
      );
    preferences
      .rental()
      .setArrivingInRentalVehicleAtDestinationCost(
        c.asDouble(
          "keepingRentedBicycleAtDestinationCost",
          preferences.rental().arrivingInRentalVehicleAtDestinationCost()
        )
      );
    preferences
      .transit()
      .initBoardSlack(
        c.asDuration2("boardSlack", preferences.transit().boardSlack().defaultValue(), SECONDS),
        c.asEnumMap("boardSlackForMode", TransitMode.class, (a, n) -> a.asDuration2(n, SECONDS))
      );

    preferences
      .street()
      .initMaxAccessEgressDuration(
        c.asDuration(
          "maxAccessEgressDuration",
          preferences.street().maxAccessEgressDuration().defaultValue()
        ),
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
    request.carPickup = c.asBoolean("kissAndRide", dft.carPickup);
    request.setLocale(c.asLocale("locale", dft.locale()));
    // 'maxTransfers' is configured in the Raptor tuning parameters, not here
    preferences
      .street()
      .initMaxDirectDuration(
        c.asDuration(
          "maxDirectStreetDuration",
          preferences.street().maxDirectDuration().defaultValue()
        ),
        c.asEnumMap("maxDirectStreetDurationForMode", StreetMode.class, NodeAdapter::asDuration)
      );

    preferences
      .system()
      .setMaxJourneyDuration(
        c.asDuration("maxJourneyDuration", preferences.system().maxJourneyDuration())
      );

    request.journey().setModes(c.asRequestModes("modes", RequestModes.defaultRequestModes()));

    preferences
      .transfer()
      .setNonpreferredCost(
        c.asInt("nonpreferredTransferPenalty", preferences.transfer().nonpreferredCost())
      );
    request.setNumItineraries(c.asInt("numItineraries", dft.numItineraries()));
    preferences.bike().setOptimizeType(c.asEnum("optimize", preferences.bike().optimizeType()));
    preferences
      .transit()
      .setOtherThanPreferredRoutesPenalty(
        c.asInt(
          "otherThanPreferredRoutesPenalty",
          preferences.transit().otherThanPreferredRoutesPenalty()
        )
      );
    request.parkAndRide = c.asBoolean("parkAndRide", dft.parkAndRide);
    request.setSearchWindow(c.asDuration("searchWindow", dft.searchWindow()));
    vehicleParking.setRequiredTags(
      c.asTextSet("requiredVehicleParkingTags", vehicleParking.requiredTags())
    );

    preferences
      .walk()
      .setStairsReluctance(c.asDouble("stairsReluctance", preferences.walk().stairsReluctance()));
    preferences
      .walk()
      .setStairsTimeFactor(c.asDouble("stairsTimeFactor", preferences.walk().stairsTimeFactor()));
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
      .setUnpreferredCost(
        c.asLinearFunction("unpreferredRouteCost", preferences.transit().unpreferredCost())
      );
    request.vehicleRental = c.asBoolean("allowBikeRental", dft.vehicleRental);
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

    preferences.setWheelchairAccessibility(
      mapAccessibilityRequest(c.path("wheelchairAccessibility"))
    );
    request.setWheelchair(c.path("wheelchairAccessibility").asBoolean("enabled", false));

    preferences.transfer().setOptimization(mapTransferOptimization(c.path("transferOptimization")));

    preferences.system().setDataOverlay(DataOverlayParametersMapper.map(c.path("dataOverlay")));

    preferences
      .street()
      .setDrivingDirection(
        c.asEnum("drivingDirection", dft.preferences().street().drivingDirection())
      );
    preferences
      .street()
      .setIntersectionTraversalModel(
        c.asEnum(
          "intersectionTraversalModel",
          dft.preferences().street().intersectionTraversalModel()
        )
      );

    NodeAdapter unpreferred = c.path("unpreferred");
    request
      .journey()
      .transit()
      .setUnpreferredRoutes(
        unpreferred.asFeedScopedIdList("routes", request.journey().transit().unpreferredRoutes())
      );

    request
      .journey()
      .transit()
      .setUnpreferredAgencies(
        unpreferred.asFeedScopedIdList("agencies", request.journey().transit().unpreferredRoutes())
      );

    return request;
  }

  private static TransferOptimizationPreferences mapTransferOptimization(NodeAdapter c) {
    var dft = TransferOptimizationPreferences.DEFAULT;
    return new TransferOptimizationPreferences(
      c.asBoolean("optimizeTransferWaitTime", dft.optimizeTransferWaitTime()),
      c.asDouble("minSafeWaitTimeFactor", dft.minSafeWaitTimeFactor()),
      c.asDouble("backTravelWaitTimeFactor", dft.backTravelWaitTimeFactor()),
      c.asDouble("extraStopBoardAlightCostsFactor", dft.extraStopBoardAlightCostsFactor())
    );
  }
}
