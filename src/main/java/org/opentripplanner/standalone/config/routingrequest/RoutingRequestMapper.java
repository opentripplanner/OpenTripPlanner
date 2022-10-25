package org.opentripplanner.standalone.config.routingrequest;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;
import static org.opentripplanner.standalone.config.routingrequest.ItineraryFiltersMapper.mapItineraryFilterParams;
import static org.opentripplanner.standalone.config.routingrequest.WheelchairAccessibilityRequestMapper.mapAccessibilityRequest;

import java.time.Duration;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.BikePreferences;
import org.opentripplanner.routing.api.request.preference.CarPreferences;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.preference.StreetPreferences;
import org.opentripplanner.routing.api.request.preference.SystemPreferences;
import org.opentripplanner.routing.api.request.preference.TransferOptimizationPreferences;
import org.opentripplanner.routing.api.request.preference.TransferPreferences;
import org.opentripplanner.routing.api.request.preference.TransitPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleParkingPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleRentalPreferences;
import org.opentripplanner.routing.api.request.preference.WalkPreferences;
import org.opentripplanner.routing.api.request.request.VehicleParkingRequest;
import org.opentripplanner.routing.api.request.request.VehicleRentalRequest;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.sandbox.DataOverlayParametersMapper;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.util.OTPFeature;
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
    VehicleRentalRequest vehicleRental = request.journey().rental();
    VehicleParkingRequest vehicleParking = request.journey().parking();

    // Keep this alphabetically sorted so it is easy to check if a parameter is missing from the
    // mapping or duplicate exist.

    vehicleRental.setAllowedNetworks(
      c
        .of("allowedVehicleRentalNetworks")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .asStringSet(vehicleRental.allowedNetworks())
    );
    request.setArriveBy(c.of("arriveBy").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(dft.arriveBy()));
    vehicleParking.setBannedTags(
      c
        .of("bannedVehicleParkingTags")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .asStringSet(vehicleParking.bannedTags())
    );
    vehicleRental.setBannedNetworks(
      c
        .of("bannedVehicleRentalNetworks")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .asStringSet(vehicleRental.bannedNetworks())
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

    request.setLocale(c.of("locale").withDoc(NA, /*TODO DOC*/"TODO").asLocale(dft.locale()));

    request
      .journey()
      .setModes(
        c
          .of("modes")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asCustomStringType(
            RequestModes.defaultRequestModes(),
            s -> new QualifiedModeSet(s).getRequestModes()
          )
      );

    request.setNumItineraries(
      c.of("numItineraries").withDoc(NA, /*TODO DOC*/"TODO").asInt(dft.numItineraries())
    );
    request.setSearchWindow(
      c.of("searchWindow").withDoc(NA, /*TODO DOC*/"TODO").asDuration(dft.searchWindow())
    );
    vehicleParking.setRequiredTags(
      c
        .of("requiredVehicleParkingTags")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .asStringSet(vehicleParking.requiredTags())
    );

    request.setWheelchair(
      c
        .of("wheelchairAccessibility")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withDescription(/*TODO DOC*/"TODO")
        .asObject()
        .of("enabled")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .asBoolean(false)
    );

    NodeAdapter unpreferred = c
      .of("unpreferred")
      .withDoc(NA, /*TODO DOC*/"TODO")
      .withDescription(/*TODO DOC*/"TODO")
      .asObject();
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
          .of("routes")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asFeedScopedIds(request.journey().transit().unpreferredRoutes())
      );

    // Map preferences
    request.withPreferences(preferences -> mapPreferences(c, preferences));

    return request;
  }

  private static void mapPreferences(NodeAdapter c, RoutingPreferences.Builder preferences) {
    preferences.withTransit(it -> mapTransitPreferences(c, it));
    preferences.withBike(it -> mapBikePreferences(c, it));
    preferences.withRental(it -> mapRentalPreferences(c, it));
    preferences.withStreet(it -> mapStreetPreferences(c, it));
    preferences.withCar(it -> mapCarPreferences(c, it));
    preferences.withSystem(it -> mapSystemPreferences(c, it));
    preferences.withTransfer(it -> mapTransferPreferences(c, it));
    preferences.withParking(mapParkingPreferences(c, preferences));
    preferences.withWalk(it -> mapWalkPreferences(c, it));
    preferences.withWheelchair(
      mapAccessibilityRequest(
        c
          .of("wheelchairAccessibility")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withDescription(/*TODO DOC*/"TODO")
          .asObject()
      )
    );
    preferences.withItineraryFilter(it -> {
      /*TODO DOC*/
      /*TODO DOC*/
      mapItineraryFilterParams(
        c
          .of("itineraryFilters")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withDescription(/*TODO DOC*/"TODO")
          .asObject(),
        it
      );
    });
  }

  private static void mapTransitPreferences(NodeAdapter c, TransitPreferences.Builder builder) {
    var dft = builder.original();
    builder
      .withAlightSlack(it ->
        it
          .withDefault(
            c
              .of("alightSlack")
              .withDoc(NA, /*TODO DOC*/"TODO")
              .asDuration2(dft.alightSlack().defaultValue(), SECONDS)
          )
          .withValues(
            c
              .of("alightSlackForMode")
              .withDoc(NA, /*TODO DOC*/"TODO")
              .asEnumMap(TransitMode.class, Duration.class)
          )
      )
      .withBoardSlack(it ->
        it
          .withDefault(
            c
              .of("boardSlack")
              .withDoc(NA, /*TODO DOC*/"TODO")
              .asDuration2(dft.boardSlack().defaultValue(), SECONDS)
          )
          .withValues(
            c
              .of("boardSlackForMode")
              .withDoc(NA, /*TODO DOC*/"TODO")
              .asEnumMap(TransitMode.class, Duration.class)
          )
      )
      .setIgnoreRealtimeUpdates(
        c
          .of("ignoreRealtimeUpdates")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asBoolean(dft.ignoreRealtimeUpdates())
      )
      .setOtherThanPreferredRoutesPenalty(
        c
          .of("otherThanPreferredRoutesPenalty")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asInt(dft.otherThanPreferredRoutesPenalty())
      )
      .setReluctanceForMode(
        c
          .of("transitReluctanceForMode")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asEnumMap(TransitMode.class, Double.class)
      )
      .setUnpreferredCost(
        c
          .of("unpreferredRouteCost")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asLinearFunction(dft.unpreferredCost())
      );
  }

  private static void mapBikePreferences(NodeAdapter c, BikePreferences.Builder builder) {
    var dft = builder.original();
    builder
      .withSpeed(c.of("bikeSpeed").withDoc(NA, /*TODO DOC*/"TODO").asDouble(dft.speed()))
      .withReluctance(
        c.of("bikeReluctance").withDoc(NA, /*TODO DOC*/"TODO").asDouble(dft.reluctance())
      )
      .withBoardCost(c.of("bikeBoardCost").withDoc(NA, /*TODO DOC*/"TODO").asInt(dft.boardCost()))
      .withParkTime(c.of("bikeParkTime").withDoc(NA, /*TODO DOC*/"TODO").asInt(dft.parkTime()))
      .withParkCost(c.of("bikeParkCost").withDoc(NA, /*TODO DOC*/"TODO").asInt(dft.parkCost()))
      .withWalkingSpeed(
        c.of("bikeWalkingSpeed").withDoc(NA, /*TODO DOC*/"TODO").asDouble(dft.walkingSpeed())
      )
      .withWalkingReluctance(
        c
          .of("bikeWalkingReluctance")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDouble(dft.walkingReluctance())
      )
      .withSwitchTime(
        c.of("bikeSwitchTime").withDoc(NA, /*TODO DOC*/"TODO").asInt(dft.switchTime())
      )
      .withSwitchCost(
        c.of("bikeSwitchCost").withDoc(NA, /*TODO DOC*/"TODO").asInt(dft.switchCost())
      )
      .withOptimizeType(c.of("optimize").withDoc(NA, /*TODO DOC*/"TODO").asEnum(dft.optimizeType()))
      .withOptimizeTriangle(it ->
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
  }

  private static void mapRentalPreferences(
    NodeAdapter c,
    VehicleRentalPreferences.Builder builder
  ) {
    var dft = builder.original();
    builder
      .withDropoffCost(
        c.of("bikeRentalDropoffCost").withDoc(NA, /*TODO DOC*/"TODO").asInt(dft.dropoffCost())
      )
      .withDropoffTime(
        c.of("bikeRentalDropoffTime").withDoc(NA, /*TODO DOC*/"TODO").asInt(dft.dropoffTime())
      )
      .withPickupCost(
        c.of("bikeRentalPickupCost").withDoc(NA, /*TODO DOC*/"TODO").asInt(dft.pickupCost())
      )
      .withPickupTime(
        c.of("bikeRentalPickupTime").withDoc(NA, /*TODO DOC*/"TODO").asInt(dft.pickupTime())
      )
      .withUseAvailabilityInformation(
        c
          .of("useBikeRentalAvailabilityInformation")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asBoolean(dft.useAvailabilityInformation())
      )
      .withArrivingInRentalVehicleAtDestinationCost(
        c
          .of("keepingRentedBicycleAtDestinationCost")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDouble(dft.arrivingInRentalVehicleAtDestinationCost())
      );
  }

  private static void mapStreetPreferences(NodeAdapter c, StreetPreferences.Builder builder) {
    var dft = builder.original();
    builder
      .withTurnReluctance(
        c.of("turnReluctance").withDoc(NA, /*TODO DOC*/"TODO").asDouble(dft.turnReluctance())
      )
      .withDrivingDirection(
        c.of("drivingDirection").withDoc(NA, /*TODO DOC*/"TODO").asEnum(dft.drivingDirection())
      )
      .withElevator(elevator -> {
        var dftElevator = dft.elevator();
        elevator
          .withBoardCost(
            c.of("elevatorBoardCost").withDoc(NA, /*TODO DOC*/"TODO").asInt(dftElevator.boardCost())
          )
          .withBoardTime(
            c.of("elevatorBoardTime").withDoc(NA, /*TODO DOC*/"TODO").asInt(dftElevator.boardTime())
          )
          .withHopCost(
            c.of("elevatorHopCost").withDoc(NA, /*TODO DOC*/"TODO").asInt(dftElevator.hopCost())
          )
          .withHopTime(
            c.of("elevatorHopTime").withDoc(NA, /*TODO DOC*/"TODO").asInt(dftElevator.hopTime())
          );
      })
      .withMaxAccessEgressDuration(
        c
          .of("maxAccessEgressDuration")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDuration(dft.maxAccessEgressDuration().defaultValue()),
        c
          .of("maxAccessEgressDurationForMode")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asEnumMap(StreetMode.class, Duration.class)
      )
      .withMaxDirectDuration(
        c
          .of("maxDirectStreetDuration")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDuration(dft.maxDirectDuration().defaultValue()),
        c
          .of("maxDirectStreetDurationForMode")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asEnumMap(StreetMode.class, Duration.class)
      )
      .withIntersectionTraversalModel(
        c
          .of("intersectionTraversalModel")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asEnum(dft.intersectionTraversalModel())
      );
  }

  private static void mapCarPreferences(NodeAdapter c, CarPreferences.Builder builder) {
    var dft = builder.original();
    builder
      .withSpeed(c.of("carSpeed").withDoc(NA, /*TODO DOC*/"TODO").asDouble(dft.speed()))
      .withReluctance(
        c.of("carReluctance").withDoc(NA, /*TODO DOC*/"TODO").asDouble(dft.reluctance())
      )
      .withDropoffTime(
        c.of("carDropoffTime").withDoc(NA, /*TODO DOC*/"TODO").asInt(dft.dropoffTime())
      )
      .withParkCost(c.of("carParkCost").withDoc(NA, /*TODO DOC*/"TODO").asInt(dft.parkCost()))
      .withParkTime(c.of("carParkTime").withDoc(NA, /*TODO DOC*/"TODO").asInt(dft.parkTime()))
      .withPickupCost(c.of("carPickupCost").withDoc(NA, /*TODO DOC*/"TODO").asInt(dft.pickupCost()))
      .withPickupTime(c.of("carPickupTime").withDoc(NA, /*TODO DOC*/"TODO").asInt(dft.pickupTime()))
      .withAccelerationSpeed(
        c
          .of("carAccelerationSpeed")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDouble(dft.accelerationSpeed())
      )
      .withDecelerationSpeed(
        c
          .of("carDecelerationSpeed")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDouble(dft.decelerationSpeed())
      );
  }

  private static void mapSystemPreferences(NodeAdapter c, SystemPreferences.Builder builder) {
    var dft = builder.original();
    builder
      .withGeoidElevation(
        c.of("geoidElevation").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(dft.geoidElevation())
      )
      .withMaxJourneyDuration(
        c
          .of("maxJourneyDuration")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDuration(dft.maxJourneyDuration())
      );
    if (OTPFeature.DataOverlay.isOn()) {
      /*TODO DOC*/
      /*TODO DOC*/
      builder.withDataOverlay(
        DataOverlayParametersMapper.map(
          c
            .of("dataOverlay")
            .withDoc(NA, /*TODO DOC*/"TODO")
            .withDescription(/*TODO DOC*/"TODO")
            .asObject()
        )
      );
    }
  }

  private static void mapTransferPreferences(NodeAdapter c, TransferPreferences.Builder tx) {
    var dft = tx.original();
    tx
      .withNonpreferredCost(
        c
          .of("nonpreferredTransferPenalty")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asInt(dft.nonpreferredCost())
      )
      .withCost(c.of("transferPenalty").withDoc(NA, /*TODO DOC*/"TODO").asInt(dft.cost()))
      .withSlack(c.of("transferSlack").withDoc(NA, /*TODO DOC*/"TODO").asInt(dft.slack()))
      .withWaitReluctance(
        c.of("waitReluctance").withDoc(NA, /*TODO DOC*/"TODO").asDouble(dft.waitReluctance())
      )
      .withOptimization(
        mapTransferOptimization(
          c
            .of("transferOptimization")
            .withDoc(NA, /*TODO DOC*/"TODO")
            .withDescription(/*TODO DOC*/"TODO")
            .asObject()
        )
      );
  }

  private static VehicleParkingPreferences mapParkingPreferences(
    NodeAdapter c,
    RoutingPreferences.Builder preferences
  ) {
    return VehicleParkingPreferences.of(
      c
        .of("useVehicleParkingAvailabilityInformation")
        .asBoolean(preferences.parking().useAvailabilityInformation())
    );
  }

  private static void mapWalkPreferences(NodeAdapter c, WalkPreferences.Builder walk) {
    var dft = walk.original();
    walk
      .withSpeed(c.of("walkSpeed").withDoc(NA, /*TODO DOC*/"TODO").asDouble(dft.speed()))
      .withReluctance(
        c.of("walkReluctance").withDoc(NA, /*TODO DOC*/"TODO").asDouble(dft.reluctance())
      )
      .withBoardCost(c.of("walkBoardCost").withDoc(NA, /*TODO DOC*/"TODO").asInt(dft.boardCost()))
      .withStairsReluctance(
        c.of("stairsReluctance").withDoc(NA, /*TODO DOC*/"TODO").asDouble(dft.stairsReluctance())
      )
      .withStairsTimeFactor(
        c.of("stairsTimeFactor").withDoc(NA, /*TODO DOC*/"TODO").asDouble(dft.stairsTimeFactor())
      )
      .withSafetyFactor(
        c.of("walkSafetyFactor").withDoc(NA, /*TODO DOC*/"TODO").asDouble(dft.safetyFactor())
      );
  }

  private static TransferOptimizationPreferences mapTransferOptimization(NodeAdapter c) {
    var dft = TransferOptimizationPreferences.DEFAULT;
    return TransferOptimizationPreferences
      .of()
      .withOptimizeTransferWaitTime(
        c
          .of("optimizeTransferWaitTime")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asBoolean(dft.optimizeTransferWaitTime())
      )
      .withMinSafeWaitTimeFactor(
        c
          .of("minSafeWaitTimeFactor")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDouble(dft.minSafeWaitTimeFactor())
      )
      .withBackTravelWaitTimeFactor(
        c
          .of("backTravelWaitTimeFactor")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDouble(dft.backTravelWaitTimeFactor())
      )
      .withExtraStopBoardAlightCostsFactor(
        c
          .of("extraStopBoardAlightCostsFactor")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDouble(dft.extraStopBoardAlightCostsFactor())
      )
      .build();
  }
}
