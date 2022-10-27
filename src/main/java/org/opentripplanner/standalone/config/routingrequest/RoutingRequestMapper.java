package org.opentripplanner.standalone.config.routingrequest;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;
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
        .since(NA)
        .summary("TODO")
        .asStringSet(vehicleRental.allowedNetworks())
    );
    request.setArriveBy(c.of("arriveBy").since(NA).summary("TODO").asBoolean(dft.arriveBy()));
    vehicleParking.setBannedTags(
      c
        .of("bannedVehicleParkingTags")
        .since(NA)
        .summary("TODO")
        .asStringSet(vehicleParking.bannedTags())
    );
    vehicleRental.setBannedNetworks(
      c
        .of("bannedVehicleRentalNetworks")
        .since(NA)
        .summary("TODO")
        .asStringSet(vehicleRental.bannedNetworks())
    );

    request
      .journey()
      .rental()
      .setAllowArrivingInRentedVehicleAtDestination(
        c
          .of("allowKeepingRentedBicycleAtDestination")
          .since(NA)
          .summary("TODO")
          .asBoolean(request.journey().rental().allowArrivingInRentedVehicleAtDestination())
      );

    request.setLocale(c.of("locale").since(NA).summary("TODO").asLocale(dft.locale()));

    request
      .journey()
      .setModes(
        c
          .of("modes")
          .since(NA)
          .summary("TODO")
          .asCustomStringType(
            RequestModes.defaultRequestModes(),
            s -> new QualifiedModeSet(s).getRequestModes()
          )
      );

    request.setNumItineraries(
      c.of("numItineraries").since(NA).summary("TODO").asInt(dft.numItineraries())
    );
    request.setSearchWindow(
      c.of("searchWindow").since(NA).summary("TODO").asDuration(dft.searchWindow())
    );
    vehicleParking.setRequiredTags(
      c
        .of("requiredVehicleParkingTags")
        .since(NA)
        .summary("TODO")
        .asStringSet(vehicleParking.requiredTags())
    );

    request.setWheelchair(
      c
        .of("wheelchairAccessibility")
        .since(NA)
        .summary("TODO")
        .description(/*TODO DOC*/"TODO")
        .asObject()
        .of("enabled")
        .since(NA)
        .summary("TODO")
        .asBoolean(false)
    );

    NodeAdapter unpreferred = c
      .of("unpreferred")
      .since(NA)
      .summary("TODO")
      .description(/*TODO DOC*/"TODO")
      .asObject();
    request
      .journey()
      .transit()
      .setUnpreferredRoutes(
        unpreferred
          .of("routes")
          .since(NA)
          .summary("TODO")
          .asFeedScopedIds(request.journey().transit().unpreferredRoutes())
      );

    request
      .journey()
      .transit()
      .setUnpreferredAgencies(
        unpreferred
          .of("routes")
          .since(NA)
          .summary("TODO")
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
          .since(NA)
          .summary("TODO")
          .description(/*TODO DOC*/"TODO")
          .asObject()
      )
    );
    preferences.withItineraryFilter(it -> {
      mapItineraryFilterParams(
        c
          .of("itineraryFilters")
          .since(NA)
          .summary("TODO")
          .description(/*TODO DOC*/"TODO")
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
              .since(NA)
              .summary("TODO")
              .asDuration2(dft.alightSlack().defaultValue(), SECONDS)
          )
          .withValues(
            c
              .of("alightSlackForMode")
              .since(NA)
              .summary("TODO")
              .asEnumMap(TransitMode.class, Duration.class)
          )
      )
      .withBoardSlack(it ->
        it
          .withDefault(
            c
              .of("boardSlack")
              .since(NA)
              .summary("TODO")
              .asDuration2(dft.boardSlack().defaultValue(), SECONDS)
          )
          .withValues(
            c
              .of("boardSlackForMode")
              .since(NA)
              .summary("TODO")
              .asEnumMap(TransitMode.class, Duration.class)
          )
      )
      .setIgnoreRealtimeUpdates(
        c
          .of("ignoreRealtimeUpdates")
          .since(NA)
          .summary("TODO")
          .asBoolean(dft.ignoreRealtimeUpdates())
      )
      .setOtherThanPreferredRoutesPenalty(
        c
          .of("otherThanPreferredRoutesPenalty")
          .since(NA)
          .summary("TODO")
          .asInt(dft.otherThanPreferredRoutesPenalty())
      )
      .setReluctanceForMode(
        c
          .of("transitReluctanceForMode")
          .since(NA)
          .summary("TODO")
          .asEnumMap(TransitMode.class, Double.class)
      )
      .setUnpreferredCost(
        c
          .of("unpreferredCost")
          .since(V2_2)
          .summary("A cost function used to calculate penalty for an unpreferred route.")
          .description(
            """
            Function should return number of seconds that we are willing to wait for preferred route
            or for an unpreferred agency's departure. For example, 600 + 2.0 x
            """
          )
          .asLinearFunction(dft.unpreferredCost())
      );
  }

  private static void mapBikePreferences(NodeAdapter c, BikePreferences.Builder builder) {
    var dft = builder.original();
    builder
      .withSpeed(c.of("bikeSpeed").since(NA).summary("TODO").asDouble(dft.speed()))
      .withReluctance(c.of("bikeReluctance").since(NA).summary("TODO").asDouble(dft.reluctance()))
      .withBoardCost(c.of("bikeBoardCost").since(NA).summary("TODO").asInt(dft.boardCost()))
      .withParkTime(c.of("bikeParkTime").since(NA).summary("TODO").asInt(dft.parkTime()))
      .withParkCost(c.of("bikeParkCost").since(NA).summary("TODO").asInt(dft.parkCost()))
      .withWalkingSpeed(
        c.of("bikeWalkingSpeed").since(NA).summary("TODO").asDouble(dft.walkingSpeed())
      )
      .withWalkingReluctance(
        c.of("bikeWalkingReluctance").since(NA).summary("TODO").asDouble(dft.walkingReluctance())
      )
      .withSwitchTime(c.of("bikeSwitchTime").since(NA).summary("TODO").asInt(dft.switchTime()))
      .withSwitchCost(c.of("bikeSwitchCost").since(NA).summary("TODO").asInt(dft.switchCost()))
      .withOptimizeType(c.of("optimize").since(NA).summary("TODO").asEnum(dft.optimizeType()))
      .withOptimizeTriangle(it ->
        it
          .withTime(c.of("bikeTriangleTimeFactor").since(NA).summary("TODO").asDouble(it.time()))
          .withSlope(c.of("bikeTriangleSlopeFactor").since(NA).summary("TODO").asDouble(it.slope()))
          .withSafety(
            c.of("bikeTriangleSafetyFactor").since(NA).summary("TODO").asDouble(it.safety())
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
        c.of("bikeRentalDropoffCost").since(NA).summary("TODO").asInt(dft.dropoffCost())
      )
      .withDropoffTime(
        c.of("bikeRentalDropoffTime").since(NA).summary("TODO").asInt(dft.dropoffTime())
      )
      .withPickupCost(
        c.of("bikeRentalPickupCost").since(NA).summary("TODO").asInt(dft.pickupCost())
      )
      .withPickupTime(
        c.of("bikeRentalPickupTime").since(NA).summary("TODO").asInt(dft.pickupTime())
      )
      .withUseAvailabilityInformation(
        c
          .of("useBikeRentalAvailabilityInformation")
          .since(NA)
          .summary("TODO")
          .asBoolean(dft.useAvailabilityInformation())
      )
      .withArrivingInRentalVehicleAtDestinationCost(
        c
          .of("keepingRentedBicycleAtDestinationCost")
          .since(NA)
          .summary("TODO")
          .asDouble(dft.arrivingInRentalVehicleAtDestinationCost())
      );
  }

  private static void mapStreetPreferences(NodeAdapter c, StreetPreferences.Builder builder) {
    var dft = builder.original();
    builder
      .withTurnReluctance(
        c.of("turnReluctance").since(NA).summary("TODO").asDouble(dft.turnReluctance())
      )
      .withDrivingDirection(
        c.of("drivingDirection").since(NA).summary("TODO").asEnum(dft.drivingDirection())
      )
      .withElevator(elevator -> {
        var dftElevator = dft.elevator();
        elevator
          .withBoardCost(
            c.of("elevatorBoardCost").since(NA).summary("TODO").asInt(dftElevator.boardCost())
          )
          .withBoardTime(
            c.of("elevatorBoardTime").since(NA).summary("TODO").asInt(dftElevator.boardTime())
          )
          .withHopCost(
            c.of("elevatorHopCost").since(NA).summary("TODO").asInt(dftElevator.hopCost())
          )
          .withHopTime(
            c.of("elevatorHopTime").since(NA).summary("TODO").asInt(dftElevator.hopTime())
          );
      })
      .withMaxAccessEgressDuration(
        c
          .of("maxAccessEgressDuration")
          .since(NA)
          .summary("TODO")
          .asDuration(dft.maxAccessEgressDuration().defaultValue()),
        c
          .of("maxAccessEgressDurationForMode")
          .since(NA)
          .summary("TODO")
          .asEnumMap(StreetMode.class, Duration.class)
      )
      .withMaxDirectDuration(
        c
          .of("maxDirectStreetDuration")
          .since(NA)
          .summary("TODO")
          .asDuration(dft.maxDirectDuration().defaultValue()),
        c
          .of("maxDirectStreetDurationForMode")
          .since(NA)
          .summary("TODO")
          .asEnumMap(StreetMode.class, Duration.class)
      )
      .withIntersectionTraversalModel(
        c
          .of("intersectionTraversalModel")
          .since(NA)
          .summary("TODO")
          .asEnum(dft.intersectionTraversalModel())
      );
  }

  private static void mapCarPreferences(NodeAdapter c, CarPreferences.Builder builder) {
    var dft = builder.original();
    builder
      .withSpeed(c.of("carSpeed").since(NA).summary("TODO").asDouble(dft.speed()))
      .withReluctance(c.of("carReluctance").since(NA).summary("TODO").asDouble(dft.reluctance()))
      .withDropoffTime(c.of("carDropoffTime").since(NA).summary("TODO").asInt(dft.dropoffTime()))
      .withParkCost(c.of("carParkCost").since(NA).summary("TODO").asInt(dft.parkCost()))
      .withParkTime(c.of("carParkTime").since(NA).summary("TODO").asInt(dft.parkTime()))
      .withPickupCost(c.of("carPickupCost").since(NA).summary("TODO").asInt(dft.pickupCost()))
      .withPickupTime(c.of("carPickupTime").since(NA).summary("TODO").asInt(dft.pickupTime()))
      .withAccelerationSpeed(
        c.of("carAccelerationSpeed").since(NA).summary("TODO").asDouble(dft.accelerationSpeed())
      )
      .withDecelerationSpeed(
        c.of("carDecelerationSpeed").since(NA).summary("TODO").asDouble(dft.decelerationSpeed())
      );
  }

  private static void mapSystemPreferences(NodeAdapter c, SystemPreferences.Builder builder) {
    var dft = builder.original();
    builder
      .withGeoidElevation(
        c.of("geoidElevation").since(NA).summary("TODO").asBoolean(dft.geoidElevation())
      )
      .withMaxJourneyDuration(
        c.of("maxJourneyDuration").since(NA).summary("TODO").asDuration(dft.maxJourneyDuration())
      );
    if (OTPFeature.DataOverlay.isOn()) {
      builder.withDataOverlay(
        DataOverlayParametersMapper.map(
          c.of("dataOverlay").since(NA).summary("TODO").description(/*TODO DOC*/"TODO").asObject()
        )
      );
    }
  }

  private static void mapTransferPreferences(NodeAdapter c, TransferPreferences.Builder tx) {
    var dft = tx.original();
    tx
      .withNonpreferredCost(
        c.of("nonpreferredTransferPenalty").since(NA).summary("TODO").asInt(dft.nonpreferredCost())
      )
      .withCost(c.of("transferPenalty").since(NA).summary("TODO").asInt(dft.cost()))
      .withSlack(c.of("transferSlack").since(NA).summary("TODO").asInt(dft.slack()))
      .withWaitReluctance(
        c.of("waitReluctance").since(NA).summary("TODO").asDouble(dft.waitReluctance())
      )
      .withOptimization(
        mapTransferOptimization(
          c
            .of("transferOptimization")
            .since(NA)
            .summary("TODO")
            .description(/*TODO DOC*/"TODO")
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
      .withSpeed(c.of("walkSpeed").since(NA).summary("TODO").asDouble(dft.speed()))
      .withReluctance(c.of("walkReluctance").since(NA).summary("TODO").asDouble(dft.reluctance()))
      .withBoardCost(c.of("walkBoardCost").since(NA).summary("TODO").asInt(dft.boardCost()))
      .withStairsReluctance(
        c.of("stairsReluctance").since(NA).summary("TODO").asDouble(dft.stairsReluctance())
      )
      .withStairsTimeFactor(
        c.of("stairsTimeFactor").since(NA).summary("TODO").asDouble(dft.stairsTimeFactor())
      )
      .withSafetyFactor(
        c.of("walkSafetyFactor").since(NA).summary("TODO").asDouble(dft.safetyFactor())
      );
  }

  private static TransferOptimizationPreferences mapTransferOptimization(NodeAdapter c) {
    var dft = TransferOptimizationPreferences.DEFAULT;
    return TransferOptimizationPreferences
      .of()
      .withOptimizeTransferWaitTime(
        c
          .of("optimizeTransferWaitTime")
          .since(NA)
          .summary("TODO")
          .asBoolean(dft.optimizeTransferWaitTime())
      )
      .withMinSafeWaitTimeFactor(
        c
          .of("minSafeWaitTimeFactor")
          .since(NA)
          .summary("TODO")
          .asDouble(dft.minSafeWaitTimeFactor())
      )
      .withBackTravelWaitTimeFactor(
        c
          .of("backTravelWaitTimeFactor")
          .since(NA)
          .summary("TODO")
          .asDouble(dft.backTravelWaitTimeFactor())
      )
      .withExtraStopBoardAlightCostsFactor(
        c
          .of("extraStopBoardAlightCostsFactor")
          .since(NA)
          .summary("TODO")
          .asDouble(dft.extraStopBoardAlightCostsFactor())
      )
      .build();
  }
}
