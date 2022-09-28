package org.opentripplanner.standalone.config;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.opentripplanner.standalone.config.WheelchairAccessibilityRequestMapper.mapAccessibilityRequest;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.time.Duration;
import java.util.List;
import java.util.Set;
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
        c
          .of("alightSlack")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDuration2(preferences.transit().alightSlack().defaultValue(), SECONDS),
        c
          .of("alightSlackForMode")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withExample(/*TODO DOC*/"TODO")
          .asEnumMap(TransitMode.class, Duration.class)
      );
    vehicleRental.setAllowedNetworks(
      Set.copyOf(
        c
          .of("allowedVehicleRentalNetworks")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withExample(/*TODO DOC*/"TODO")
          .asStringList(List.copyOf(vehicleRental.allowedNetworks()))
      )
    );
    request.setArriveBy(c.of("arriveBy").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(dft.arriveBy()));
    vehicleParking.setBannedTags(
      Set.copyOf(
        c
          .of("bannedVehicleParkingTags")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withExample(/*TODO DOC*/"TODO")
          .asStringList(List.copyOf(vehicleParking.bannedTags()))
      )
    );
    vehicleRental.setBannedNetworks(
      Set.copyOf(
        c
          .of("bannedVehicleRentalNetworks")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withExample(/*TODO DOC*/"TODO")
          .asStringList(List.copyOf(vehicleRental.bannedNetworks()))
      )
    );

    preferences.withBike(bike -> {
      bike.setSpeed(c.of("bikeSpeed").withDoc(NA, /*TODO DOC*/"TODO").asDouble(bike.speed()));
      bike.setReluctance(
        c.of("bikeReluctance").withDoc(NA, /*TODO DOC*/"TODO").asDouble(bike.reluctance())
      );
      bike.setBoardCost(
        c.of("bikeBoardCost").withDoc(NA, /*TODO DOC*/"TODO").asInt(bike.boardCost())
      );
      bike.setParkTime(c.of("bikeParkTime").withDoc(NA, /*TODO DOC*/"TODO").asInt(bike.parkTime()));
      bike.setParkCost(c.of("bikeParkCost").withDoc(NA, /*TODO DOC*/"TODO").asInt(bike.parkCost()));
      bike.setWalkingSpeed(
        c.of("bikeWalkingSpeed").withDoc(NA, /*TODO DOC*/"TODO").asDouble(bike.walkingSpeed())
      );
      bike.setWalkingReluctance(
        c
          .of("bikeWalkingReluctance")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDouble(bike.walkingReluctance())
      );
      bike.setSwitchTime(
        c.of("bikeSwitchTime").withDoc(NA, /*TODO DOC*/"TODO").asInt(bike.switchTime())
      );
      bike.setSwitchCost(
        c.of("bikeSwitchCost").withDoc(NA, /*TODO DOC*/"TODO").asInt(bike.switchCost())
      );
      bike.setOptimizeType(
        c.of("optimize").withDoc(NA, /*TODO DOC*/"TODO").asEnum(bike.optimizeType())
      );

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
      .setDropoffCost(
        c
          .of("bikeRentalDropoffCost")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asInt(preferences.rental().dropoffCost())
      );
    preferences
      .rental()
      .setDropoffTime(
        c
          .of("bikeRentalDropoffTime")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asInt(preferences.rental().dropoffTime())
      );
    preferences
      .rental()
      .setPickupCost(
        c
          .of("bikeRentalPickupCost")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asInt(preferences.rental().pickupCost())
      );
    preferences
      .rental()
      .setPickupTime(
        c
          .of("bikeRentalPickupTime")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asInt(preferences.rental().pickupTime())
      );
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
        c
          .of("boardSlack")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDuration2(preferences.transit().boardSlack().defaultValue(), SECONDS),
        c
          .of("boardSlackForMode")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withExample(/*TODO DOC*/"TODO")
          .asEnumMap(TransitMode.class, Duration.class)
      );

    preferences
      .street()
      .initMaxAccessEgressDuration(
        c
          .of("maxAccessEgressDuration")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDuration(preferences.street().maxAccessEgressDuration().defaultValue()),
        c
          .of("maxAccessEgressDurationForMode")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withExample(/*TODO DOC*/"TODO")
          .asEnumMap(StreetMode.class, Duration.class)
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
    preferences
      .car()
      .setDropoffTime(
        c
          .of("carDropoffTime")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asInt(preferences.car().dropoffTime())
      );
    preferences
      .car()
      .setParkCost(
        c.of("carParkCost").withDoc(NA, /*TODO DOC*/"TODO").asInt(preferences.car().parkCost())
      );
    preferences
      .car()
      .setParkTime(
        c.of("carParkTime").withDoc(NA, /*TODO DOC*/"TODO").asInt(preferences.car().parkTime())
      );
    preferences
      .car()
      .setPickupCost(
        c.of("carPickupCost").withDoc(NA, /*TODO DOC*/"TODO").asInt(preferences.car().pickupCost())
      );
    preferences
      .car()
      .setPickupTime(
        c.of("carPickupTime").withDoc(NA, /*TODO DOC*/"TODO").asInt(preferences.car().pickupTime())
      );
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
      .setElevatorBoardCost(
        c
          .of("elevatorBoardCost")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asInt(preferences.street().elevatorBoardCost())
      );
    preferences
      .street()
      .setElevatorBoardTime(
        c
          .of("elevatorBoardTime")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asInt(preferences.street().elevatorBoardTime())
      );
    preferences
      .street()
      .setElevatorHopCost(
        c
          .of("elevatorHopCost")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asInt(preferences.street().elevatorHopCost())
      );
    preferences
      .street()
      .setElevatorHopTime(
        c
          .of("elevatorHopTime")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asInt(preferences.street().elevatorHopTime())
      );
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
    request.setLocale(c.of("locale").withDoc(NA, /*TODO DOC*/"TODO").asLocale(dft.locale()));
    // 'maxTransfers' is configured in the Raptor tuning parameters, not here
    preferences
      .street()
      .initMaxDirectDuration(
        c
          .of("maxDirectStreetDuration")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDuration(preferences.street().maxDirectDuration().defaultValue()),
        c
          .of("maxDirectStreetDurationForMode")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withExample(/*TODO DOC*/"TODO")
          .asEnumMap(StreetMode.class, Duration.class)
      );

    preferences
      .system()
      .setMaxJourneyDuration(
        c
          .of("maxJourneyDuration")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDuration(preferences.system().maxJourneyDuration())
      );

    request
      .journey()
      .setModes(
        c
          .of("modes")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withExample(/*TODO DOC*/"TODO")
          .asCustomStingType(
            RequestModes.defaultRequestModes(),
            s -> new QualifiedModeSet(s).getRequestModes()
          )
      );

    preferences
      .transfer()
      .setNonpreferredCost(
        c
          .of("nonpreferredTransferPenalty")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asInt(preferences.transfer().nonpreferredCost())
      );
    request.setNumItineraries(
      c.of("numItineraries").withDoc(NA, /*TODO DOC*/"TODO").asInt(dft.numItineraries())
    );
    preferences
      .transit()
      .setOtherThanPreferredRoutesPenalty(
        c
          .of("otherThanPreferredRoutesPenalty")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asInt(preferences.transit().otherThanPreferredRoutesPenalty())
      );
    request.parkAndRide =
      c.of("parkAndRide").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(dft.parkAndRide);
    request.setSearchWindow(
      c.of("searchWindow").withDoc(NA, /*TODO DOC*/"TODO").asDuration(dft.searchWindow())
    );
    vehicleParking.setRequiredTags(
      Set.copyOf(
        c
          .of("requiredVehicleParkingTags")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withExample(/*TODO DOC*/"TODO")
          .asStringList(List.copyOf(vehicleParking.requiredTags()))
      )
    );

    preferences
      .transfer()
      .setCost(
        c.of("transferPenalty").withDoc(NA, /*TODO DOC*/"TODO").asInt(preferences.transfer().cost())
      );
    preferences
      .transfer()
      .setSlack(
        c.of("transferSlack").withDoc(NA, /*TODO DOC*/"TODO").asInt(preferences.transfer().slack())
      );
    preferences
      .transit()
      .setReluctanceForMode(
        c
          .of("transitReluctanceForMode")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withExample(/*TODO DOC*/"TODO")
          .asEnumMap(TransitMode.class, Double.class)
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
      walk.setBoardCost(
        c.of("walkBoardCost").withDoc(NA, /*TODO DOC*/"TODO").asInt(walk.boardCost())
      );
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
        c
          .of("drivingDirection")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asEnum(dft.preferences().street().drivingDirection())
      );
    preferences
      .street()
      .setIntersectionTraversalModel(
        c
          .of("intersectionTraversalModel")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asEnum(dft.preferences().street().intersectionTraversalModel())
      );

    NodeAdapter unpreferred = c.path("unpreferred");
    request
      .journey()
      .transit()
      .setUnpreferredRoutes(
        unpreferred
          .of("routes")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asFeedScopedIds(request.journey().transit().unpreferredRoutes())
      );

    request
      .journey()
      .transit()
      .setUnpreferredAgencies(
        unpreferred
          .of("agencies")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asFeedScopedIds(request.journey().transit().unpreferredRoutes())
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
