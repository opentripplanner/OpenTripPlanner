package org.opentripplanner.standalone.config;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.opentripplanner.standalone.config.WheelchairAccessibilityRequestMapper.mapAccessibilityRequest;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.time.Duration;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.preference.TransferOptimizationPreferences;
import org.opentripplanner.routing.api.request.request.VehicleParkingRequest;
import org.opentripplanner.routing.api.request.request.VehicleRentalRequest;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
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
        c.asEnumMap("alightSlackForMode", TransitMode.class, Duration.class)
      );
    vehicleRental.setAllowedNetworks(
      c.asTextSet("allowedVehicleRentalNetworks", vehicleRental.allowedNetworks())
    );
    request.setArriveBy(c.of("arriveBy").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(dft.arriveBy()));
    vehicleParking.setBannedTags(
      c.asTextSet("bannedVehicleParkingTags", vehicleParking.bannedTags())
    );
    vehicleRental.setBannedNetworks(
      c.asTextSet("bannedVehicleRentalNetworks", vehicleRental.bannedNetworks())
    );

    preferences.withBike(bike -> {
      bike.setSpeed(c.of("bikeSpeed").withDoc(NA, /*TODO DOC*/"TODO").asDouble(bike.speed()));
      bike.setReluctance(
        c.of("bikeReluctance").withDoc(NA, /*TODO DOC*/"TODO").asDouble(bike.reluctance())
      );
      bike.setBoardCost(c.asInt("bikeBoardCost", bike.boardCost()));
      bike.setParkTime(c.asInt("bikeParkTime", bike.parkTime()));
      bike.setParkCost(c.asInt("bikeParkCost", bike.parkCost()));
      bike.setWalkingSpeed(
        c.of("bikeWalkingSpeed").withDoc(NA, /*TODO DOC*/"TODO").asDouble(bike.walkingSpeed())
      );
      bike.setWalkingReluctance(
        c
          .of("bikeWalkingReluctance")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDouble(bike.walkingReluctance())
      );
      bike.setSwitchTime(c.asInt("bikeSwitchTime", bike.switchTime()));
      bike.setSwitchCost(c.asInt("bikeSwitchCost", bike.switchCost()));
      bike.setOptimizeType(c.asEnum("optimize", bike.optimizeType()));

      bike.withOptimizeTriangle(it ->
        it
          .withTime(
            c.of("bikeTriangleTimeFactor").withDoc(NA, /*TODO DOC*/"TODO").asDouble(it.time())
          )
          .withSlope(
            c.of("bikeTriangleSlopeFactor").withDoc(NA, /*TODO DOC*/"TODO").asDouble(it.slope())
          )
          .withSafety(
            c.of("bikeTriangleSafetyFactor").withDoc(NA, /*TODO DOC*/"TODO").asDouble(it.safety())
          )
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
        c
          .of("allowKeepingRentedBicycleAtDestination")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asBoolean(request.journey().rental().allowArrivingInRentedVehicleAtDestination())
      );
    preferences
      .rental()
      .setArrivingInRentalVehicleAtDestinationCost(
        c
          .of("keepingRentedBicycleAtDestinationCost")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDouble(preferences.rental().arrivingInRentalVehicleAtDestinationCost())
      );
    preferences
      .transit()
      .initBoardSlack(
        c.asDuration2("boardSlack", preferences.transit().boardSlack().defaultValue(), SECONDS),
        c.asEnumMap("boardSlackForMode", TransitMode.class, Duration.class)
      );

    preferences
      .street()
      .initMaxAccessEgressDuration(
        c.asDuration(
          "maxAccessEgressDuration",
          preferences.street().maxAccessEgressDuration().defaultValue()
        ),
        c.asEnumMap("maxAccessEgressDurationForMode", StreetMode.class, Duration.class)
      );
    preferences
      .car()
      .setAccelerationSpeed(
        c
          .of("carAccelerationSpeed")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDouble(preferences.car().accelerationSpeed())
      );
    preferences
      .car()
      .setDecelerationSpeed(
        c
          .of("carDecelerationSpeed")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDouble(preferences.car().decelerationSpeed())
      );
    preferences.car().setDropoffTime(c.asInt("carDropoffTime", preferences.car().dropoffTime()));
    preferences.car().setParkCost(c.asInt("carParkCost", preferences.car().parkCost()));
    preferences.car().setParkTime(c.asInt("carParkTime", preferences.car().parkTime()));
    preferences.car().setPickupCost(c.asInt("carPickupCost", preferences.car().pickupCost()));
    preferences.car().setPickupTime(c.asInt("carPickupTime", preferences.car().pickupTime()));
    preferences
      .car()
      .setReluctance(
        c
          .of("carReluctance")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDouble(preferences.car().reluctance())
      );
    preferences
      .car()
      .setSpeed(
        c.of("carSpeed").withDoc(NA, /*TODO DOC*/"TODO").asDouble(preferences.car().speed())
      );

    preferences
      .system()
      .setItineraryFilters(ItineraryFiltersMapper.map(c.path("itineraryFilters")));
    preferences
      .system()
      .setDisableAlertFiltering(
        c
          .of("disableAlertFiltering")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asBoolean(preferences.system().disableAlertFiltering())
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
      .setGeoidElevation(
        c
          .of("geoidElevation")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asBoolean(preferences.system().geoidElevation())
      );
    preferences
      .transit()
      .setIgnoreRealtimeUpdates(
        c
          .of("ignoreRealtimeUpdates")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asBoolean(preferences.transit().ignoreRealtimeUpdates())
      );
    request.carPickup =
      c.of("kissAndRide").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(dft.carPickup);
    request.setLocale(c.asLocale("locale", dft.locale()));
    // 'maxTransfers' is configured in the Raptor tuning parameters, not here
    preferences
      .street()
      .initMaxDirectDuration(
        c.asDuration(
          "maxDirectStreetDuration",
          preferences.street().maxDirectDuration().defaultValue()
        ),
        c.asEnumMap("maxDirectStreetDurationForMode", StreetMode.class, Duration.class)
      );

    preferences
      .system()
      .setMaxJourneyDuration(
        c.asDuration("maxJourneyDuration", preferences.system().maxJourneyDuration())
      );

    request
      .journey()
      .setModes(
        c.asCustomStingType(
          "modes",
          RequestModes.defaultRequestModes(),
          s -> new QualifiedModeSet(s).getRequestModes()
        )
      );

    preferences
      .transfer()
      .setNonpreferredCost(
        c.asInt("nonpreferredTransferPenalty", preferences.transfer().nonpreferredCost())
      );
    request.setNumItineraries(c.asInt("numItineraries", dft.numItineraries()));
    preferences
      .transit()
      .setOtherThanPreferredRoutesPenalty(
        c.asInt(
          "otherThanPreferredRoutesPenalty",
          preferences.transit().otherThanPreferredRoutesPenalty()
        )
      );
    request.parkAndRide =
      c.of("parkAndRide").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(dft.parkAndRide);
    request.setSearchWindow(c.asDuration("searchWindow", dft.searchWindow()));
    vehicleParking.setRequiredTags(
      c.asTextSet("requiredVehicleParkingTags", vehicleParking.requiredTags())
    );

    preferences.transfer().setCost(c.asInt("transferPenalty", preferences.transfer().cost()));
    preferences.transfer().setSlack(c.asInt("transferSlack", preferences.transfer().slack()));
    preferences
      .transit()
      .setReluctanceForMode(
        c.asEnumMap("transitReluctanceForMode", TransitMode.class, Double.class)
      );
    preferences
      .street()
      .setTurnReluctance(
        c
          .of("turnReluctance")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDouble(preferences.street().turnReluctance())
      );
    preferences
      .rental()
      .setUseAvailabilityInformation(
        c
          .of("useBikeRentalAvailabilityInformation")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asBoolean(preferences.rental().useAvailabilityInformation())
      );
    preferences
      .parking()
      .setUseAvailabilityInformation(
        c
          .of("useVehicleParkingAvailabilityInformation")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asBoolean(preferences.parking().useAvailabilityInformation())
      );
    preferences
      .transit()
      .setUnpreferredCost(
        c.asLinearFunction("unpreferredRouteCost", preferences.transit().unpreferredCost())
      );
    request.vehicleRental =
      c.of("allowBikeRental").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(dft.vehicleRental);
    preferences
      .transfer()
      .setWaitAtBeginningFactor(
        c
          .of("waitAtBeginningFactor")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDouble(preferences.transfer().waitAtBeginningFactor())
      );
    preferences
      .transfer()
      .setWaitReluctance(
        c
          .of("waitReluctance")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDouble(preferences.transfer().waitAtBeginningFactor())
      );
    preferences.withWalk(walk -> {
      walk.setSpeed(c.of("walkSpeed").withDoc(NA, /*TODO DOC*/"TODO").asDouble(walk.speed()));
      walk.setReluctance(
        c.of("walkReluctance").withDoc(NA, /*TODO DOC*/"TODO").asDouble(walk.reluctance())
      );
      walk.setBoardCost(c.asInt("walkBoardCost", walk.boardCost()));
      walk.setStairsReluctance(
        c.of("stairsReluctance").withDoc(NA, /*TODO DOC*/"TODO").asDouble(walk.stairsReluctance())
      );
      walk.setStairsTimeFactor(
        c.of("stairsTimeFactor").withDoc(NA, /*TODO DOC*/"TODO").asDouble(walk.stairsTimeFactor())
      );
      walk.setSafetyFactor(
        c.of("walkSafetyFactor").withDoc(NA, /*TODO DOC*/"TODO").asDouble(walk.safetyFactor())
      );
    });

    preferences.setWheelchair(mapAccessibilityRequest(c.path("wheelchairAccessibility")));
    request.setWheelchair(
      c
        .path("wheelchairAccessibility")
        .of("enabled")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .asBoolean(false)
    );

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
        unpreferred.asFeedScopedIds("routes", request.journey().transit().unpreferredRoutes())
      );

    request
      .journey()
      .transit()
      .setUnpreferredAgencies(
        unpreferred.asFeedScopedIds("agencies", request.journey().transit().unpreferredRoutes())
      );

    return request;
  }

  private static TransferOptimizationPreferences mapTransferOptimization(NodeAdapter c) {
    var dft = TransferOptimizationPreferences.DEFAULT;
    return new TransferOptimizationPreferences(
      c
        .of("optimizeTransferWaitTime")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .asBoolean(dft.optimizeTransferWaitTime()),
      c
        .of("minSafeWaitTimeFactor")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .asDouble(dft.minSafeWaitTimeFactor()),
      c
        .of("backTravelWaitTimeFactor")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .asDouble(dft.backTravelWaitTimeFactor()),
      c
        .of("extraStopBoardAlightCostsFactor")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .asDouble(dft.extraStopBoardAlightCostsFactor())
    );
  }
}
