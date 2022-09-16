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

    preferences.withBike(bike -> {
      bike.setSpeed(c.asDouble("bikeSpeed", bike.speed()));
      bike.setReluctance(c.asDouble("bikeReluctance", bike.reluctance()));
      bike.setBoardCost(c.asInt("bikeBoardCost", bike.boardCost()));
      bike.setParkTime(c.asInt("bikeParkTime", bike.parkTime()));
      bike.setParkCost(c.asInt("bikeParkCost", bike.parkCost()));
      bike.setWalkingSpeed(c.asDouble("bikeWalkingSpeed", bike.walkingSpeed()));
      bike.setWalkingReluctance(c.asDouble("bikeWalkingReluctance", bike.walkingReluctance()));
      bike.setSwitchTime(c.asInt("bikeSwitchTime", bike.switchTime()));
      bike.setSwitchCost(c.asInt("bikeSwitchCost", bike.switchCost()));
      bike.setOptimizeType(c.asEnum("optimize", bike.optimizeType()));

      bike.withOptimizeTriangle(it ->
        it
          .withTime(c.asDouble("bikeTriangleTimeFactor", it.time()))
          .withSlope(c.asDouble("bikeTriangleSlopeFactor", it.slope()))
          .withSafety(c.asDouble("bikeTriangleSafetyFactor", it.safety()))
      );
    });

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
    preferences.withCar(car -> {
      var cDft = preferences.car();
      car.withSpeed(c.asDouble("carSpeed", cDft.speed()));
      car.withReluctance(c.asDouble("carReluctance", cDft.reluctance()));
      car.withDropoffTime(c.asInt("carDropoffTime", cDft.dropoffTime()));
      car.withParkCost(c.asInt("carParkCost", cDft.parkCost()));
      car.withParkTime(c.asInt("carParkTime", cDft.parkTime()));
      car.withPickupCost(c.asInt("carPickupCost", cDft.pickupCost()));
      car.withPickupTime(c.asInt("carPickupTime", cDft.pickupTime()));
      car.withAccelerationSpeed(c.asDouble("carAccelerationSpeed", cDft.accelerationSpeed()));
      car.withDecelerationSpeed(c.asDouble("carDecelerationSpeed", cDft.decelerationSpeed()));
    });

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

    preferences.withTransfer(tx -> {
      var txDft = preferences.transfer();
      tx.withNonpreferredCost(c.asInt("nonpreferredTransferPenalty", txDft.nonpreferredCost()));
      tx.withCost(c.asInt("transferPenalty", txDft.cost()));
      tx.withSlack(c.asInt("transferSlack", txDft.slack()));
      tx.withWaitReluctance(c.asDouble("waitReluctance", txDft.waitReluctance()));
      tx.withOptimization(mapTransferOptimization(c.path("transferOptimization")));
    });

    request.setNumItineraries(c.asInt("numItineraries", dft.numItineraries()));
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
    preferences.withWalk(walk -> {
      walk.withSpeed(c.asDouble("walkSpeed", walk.speed()));
      walk.withReluctance(c.asDouble("walkReluctance", walk.reluctance()));
      walk.withBoardCost(c.asInt("walkBoardCost", walk.boardCost()));
      walk.withStairsReluctance(c.asDouble("stairsReluctance", walk.stairsReluctance()));
      walk.withStairsTimeFactor(c.asDouble("stairsTimeFactor", walk.stairsTimeFactor()));
      walk.withSafetyFactor(c.asDouble("walkSafetyFactor", walk.safetyFactor()));
    });

    preferences.setWheelchair(mapAccessibilityRequest(c.path("wheelchairAccessibility")));
    request.setWheelchair(c.path("wheelchairAccessibility").asBoolean("enabled", false));

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
    return TransferOptimizationPreferences
      .of()
      .withOptimizeTransferWaitTime(
        c.asBoolean("optimizeTransferWaitTime", dft.optimizeTransferWaitTime())
      )
      .withMinSafeWaitTimeFactor(c.asDouble("minSafeWaitTimeFactor", dft.minSafeWaitTimeFactor()))
      .withBackTravelWaitTimeFactor(
        c.asDouble("backTravelWaitTimeFactor", dft.backTravelWaitTimeFactor())
      )
      .withExtraStopBoardAlightCostsFactor(
        c.asDouble("extraStopBoardAlightCostsFactor", dft.extraStopBoardAlightCostsFactor())
      )
      .build();
  }
}
