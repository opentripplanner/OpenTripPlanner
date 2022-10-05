package org.opentripplanner.standalone.config;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.opentripplanner.standalone.config.ItineraryFiltersMapper.mapItineraryFilterParams;
import static org.opentripplanner.standalone.config.WheelchairAccessibilityRequestMapper.mapAccessibilityRequest;

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
      c.asTextSet("allowedVehicleRentalNetworks", vehicleRental.allowedNetworks())
    );
    request.setArriveBy(c.asBoolean("arriveBy", dft.arriveBy()));
    vehicleParking.setBannedTags(
      c.asTextSet("bannedVehicleParkingTags", vehicleParking.bannedTags())
    );
    vehicleRental.setBannedNetworks(
      c.asTextSet("bannedVehicleRentalNetworks", vehicleRental.bannedNetworks())
    );

    request
      .journey()
      .rental()
      .setAllowArrivingInRentedVehicleAtDestination(
        c.asBoolean(
          "allowKeepingRentedBicycleAtDestination",
          request.journey().rental().allowArrivingInRentedVehicleAtDestination()
        )
      );

    request.setLocale(c.asLocale("locale", dft.locale()));

    request.journey().setModes(c.asRequestModes("modes", RequestModes.defaultRequestModes()));

    request.setNumItineraries(c.asInt("numItineraries", dft.numItineraries()));
    request.setSearchWindow(c.asDuration("searchWindow", dft.searchWindow()));
    vehicleParking.setRequiredTags(
      c.asTextSet("requiredVehicleParkingTags", vehicleParking.requiredTags())
    );

    request.setWheelchair(c.path("wheelchairAccessibility").asBoolean("enabled", false));

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
    preferences.withWheelchair(mapAccessibilityRequest(c.path("wheelchairAccessibility")));
    preferences.withItineraryFilter(it -> mapItineraryFilterParams(c.path("itineraryFilters"), it));
  }

  private static void mapTransitPreferences(NodeAdapter c, TransitPreferences.Builder builder) {
    var dft = builder.original();
    builder
      .withAlightSlack(it ->
        it
          .withDefault(c.asDuration2("alightSlack", dft.alightSlack().defaultValue(), SECONDS))
          .withValues(
            c.asEnumMap(
              "alightSlackForMode",
              TransitMode.class,
              (a, n) -> a.asDuration2(n, SECONDS)
            )
          )
      )
      .withBoardSlack(it ->
        it
          .withDefault(c.asDuration2("boardSlack", dft.boardSlack().defaultValue(), SECONDS))
          .withValues(
            c.asEnumMap("boardSlackForMode", TransitMode.class, (a, n) -> a.asDuration2(n, SECONDS))
          )
      )
      .setIgnoreRealtimeUpdates(c.asBoolean("ignoreRealtimeUpdates", dft.ignoreRealtimeUpdates()))
      .setOtherThanPreferredRoutesPenalty(
        c.asInt("otherThanPreferredRoutesPenalty", dft.otherThanPreferredRoutesPenalty())
      )
      .setReluctanceForMode(
        c.asEnumMap("transitReluctanceForMode", TransitMode.class, NodeAdapter::asDouble)
      )
      .setUnpreferredCost(c.asLinearFunction("unpreferredRouteCost", dft.unpreferredCost()));
  }

  private static void mapBikePreferences(NodeAdapter c, BikePreferences.Builder builder) {
    var dft = builder.original();
    builder
      .withSpeed(c.asDouble("bikeSpeed", dft.speed()))
      .withReluctance(c.asDouble("bikeReluctance", dft.reluctance()))
      .withBoardCost(c.asInt("bikeBoardCost", dft.boardCost()))
      .withParkTime(c.asInt("bikeParkTime", dft.parkTime()))
      .withParkCost(c.asInt("bikeParkCost", dft.parkCost()))
      .withWalkingSpeed(c.asDouble("bikeWalkingSpeed", dft.walkingSpeed()))
      .withWalkingReluctance(c.asDouble("bikeWalkingReluctance", dft.walkingReluctance()))
      .withSwitchTime(c.asInt("bikeSwitchTime", dft.switchTime()))
      .withSwitchCost(c.asInt("bikeSwitchCost", dft.switchCost()))
      .withOptimizeType(c.asEnum("optimize", dft.optimizeType()))
      .withOptimizeTriangle(it ->
        it
          .withTime(c.asDouble("bikeTriangleTimeFactor", it.time()))
          .withSlope(c.asDouble("bikeTriangleSlopeFactor", it.slope()))
          .withSafety(c.asDouble("bikeTriangleSafetyFactor", it.safety()))
      );
  }

  private static void mapRentalPreferences(
    NodeAdapter c,
    VehicleRentalPreferences.Builder builder
  ) {
    var dft = builder.original();
    builder
      .withDropoffCost(c.asInt("bikeRentalDropoffCost", dft.dropoffCost()))
      .withDropoffTime(c.asInt("bikeRentalDropoffTime", dft.dropoffTime()))
      .withPickupCost(c.asInt("bikeRentalPickupCost", dft.pickupCost()))
      .withPickupTime(c.asInt("bikeRentalPickupTime", dft.pickupTime()))
      .withUseAvailabilityInformation(
        c.asBoolean("useBikeRentalAvailabilityInformation", dft.useAvailabilityInformation())
      )
      .withArrivingInRentalVehicleAtDestinationCost(
        c.asDouble(
          "keepingRentedBicycleAtDestinationCost",
          dft.arrivingInRentalVehicleAtDestinationCost()
        )
      );
  }

  private static void mapStreetPreferences(NodeAdapter c, StreetPreferences.Builder builder) {
    var dft = builder.original();
    builder
      .withTurnReluctance(c.asDouble("turnReluctance", dft.turnReluctance()))
      .withDrivingDirection(c.asEnum("drivingDirection", dft.drivingDirection()))
      .withElevator(elevator -> {
        var dftElevator = dft.elevator();
        elevator
          .withBoardCost(c.asInt("elevatorBoardCost", dftElevator.boardCost()))
          .withBoardTime(c.asInt("elevatorBoardTime", dftElevator.boardTime()))
          .withHopCost(c.asInt("elevatorHopCost", dftElevator.hopCost()))
          .withHopTime(c.asInt("elevatorHopTime", dftElevator.hopTime()));
      })
      .withMaxAccessEgressDuration(
        c.asDuration("maxAccessEgressDuration", dft.maxAccessEgressDuration().defaultValue()),
        c.asEnumMap("maxAccessEgressDurationForMode", StreetMode.class, NodeAdapter::asDuration)
      )
      .withMaxDirectDuration(
        c.asDuration("maxDirectStreetDuration", dft.maxDirectDuration().defaultValue()),
        c.asEnumMap("maxDirectStreetDurationForMode", StreetMode.class, NodeAdapter::asDuration)
      )
      .withIntersectionTraversalModel(
        c.asEnum("intersectionTraversalModel", dft.intersectionTraversalModel())
      );
  }

  private static void mapCarPreferences(NodeAdapter c, CarPreferences.Builder builder) {
    var dft = builder.original();
    builder
      .withSpeed(c.asDouble("carSpeed", dft.speed()))
      .withReluctance(c.asDouble("carReluctance", dft.reluctance()))
      .withDropoffTime(c.asInt("carDropoffTime", dft.dropoffTime()))
      .withParkCost(c.asInt("carParkCost", dft.parkCost()))
      .withParkTime(c.asInt("carParkTime", dft.parkTime()))
      .withPickupCost(c.asInt("carPickupCost", dft.pickupCost()))
      .withPickupTime(c.asInt("carPickupTime", dft.pickupTime()))
      .withAccelerationSpeed(c.asDouble("carAccelerationSpeed", dft.accelerationSpeed()))
      .withDecelerationSpeed(c.asDouble("carDecelerationSpeed", dft.decelerationSpeed()));
  }

  private static void mapSystemPreferences(NodeAdapter c, SystemPreferences.Builder builder) {
    var dft = builder.original();
    builder
      .withGeoidElevation(c.asBoolean("geoidElevation", dft.geoidElevation()))
      .withMaxJourneyDuration(c.asDuration("maxJourneyDuration", dft.maxJourneyDuration()));
    if (OTPFeature.DataOverlay.isOn()) {
      builder.withDataOverlay(DataOverlayParametersMapper.map(c.path("dataOverlay")));
    }
  }

  private static void mapTransferPreferences(NodeAdapter c, TransferPreferences.Builder tx) {
    var dft = tx.original();
    tx
      .withNonpreferredCost(c.asInt("nonpreferredTransferPenalty", dft.nonpreferredCost()))
      .withCost(c.asInt("transferPenalty", dft.cost()))
      .withSlack(c.asInt("transferSlack", dft.slack()))
      .withWaitReluctance(c.asDouble("waitReluctance", dft.waitReluctance()))
      .withOptimization(mapTransferOptimization(c.path("transferOptimization")));
  }

  private static VehicleParkingPreferences mapParkingPreferences(
    NodeAdapter c,
    RoutingPreferences.Builder preferences
  ) {
    return VehicleParkingPreferences.of(
      c.asBoolean(
        "useVehicleParkingAvailabilityInformation",
        preferences.parking().useAvailabilityInformation()
      )
    );
  }

  private static void mapWalkPreferences(NodeAdapter c, WalkPreferences.Builder walk) {
    var dft = walk.original();
    walk
      .withSpeed(c.asDouble("walkSpeed", dft.speed()))
      .withReluctance(c.asDouble("walkReluctance", dft.reluctance()))
      .withBoardCost(c.asInt("walkBoardCost", dft.boardCost()))
      .withStairsReluctance(c.asDouble("stairsReluctance", dft.stairsReluctance()))
      .withStairsTimeFactor(c.asDouble("stairsTimeFactor", dft.stairsTimeFactor()))
      .withSafetyFactor(c.asDouble("walkSafetyFactor", dft.safetyFactor()));
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
