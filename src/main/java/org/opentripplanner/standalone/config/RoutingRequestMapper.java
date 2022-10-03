package org.opentripplanner.standalone.config;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.opentripplanner.standalone.config.ItineraryFiltersMapper.mapItineraryFilterParams;
import static org.opentripplanner.standalone.config.WheelchairAccessibilityRequestMapper.mapAccessibilityRequest;

import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.preference.TransferOptimizationPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleParkingPreferences;
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
    RoutingPreferences preferences = request.preferences();
    VehicleRentalRequest vehicleRental = request.journey().rental();
    VehicleParkingRequest vehicleParking = request.journey().parking();

    // Keep this alphabetically sorted so it is easy to check if a parameter is missing from the
    // mapping or duplicate exist.

    preferences.withTransit(tr -> {
      var dftTr = preferences.transit();
      tr.initAlightSlack(
        c.asDuration2("alightSlack", dftTr.alightSlack().defaultValue(), SECONDS),
        c.asEnumMap("alightSlackForMode", TransitMode.class, (a, n) -> a.asDuration2(n, SECONDS))
      );
      tr.initBoardSlack(
        c.asDuration2("boardSlack", dftTr.boardSlack().defaultValue(), SECONDS),
        c.asEnumMap("boardSlackForMode", TransitMode.class, (a, n) -> a.asDuration2(n, SECONDS))
      );
      tr.setIgnoreRealtimeUpdates(
        c.asBoolean("ignoreRealtimeUpdates", dftTr.ignoreRealtimeUpdates())
      );
      tr.setOtherThanPreferredRoutesPenalty(
        c.asInt("otherThanPreferredRoutesPenalty", dftTr.otherThanPreferredRoutesPenalty())
      );
      tr.setReluctanceForMode(
        c.asEnumMap("transitReluctanceForMode", TransitMode.class, NodeAdapter::asDouble)
      );
      tr.setUnpreferredCost(c.asLinearFunction("unpreferredRouteCost", dftTr.unpreferredCost()));
    });

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

    preferences.withRental(rental -> {
      var rentalDft = preferences.rental();
      rental
        .withDropoffCost(c.asInt("bikeRentalDropoffCost", rentalDft.dropoffCost()))
        .withDropoffTime(c.asInt("bikeRentalDropoffTime", rentalDft.dropoffTime()))
        .withPickupCost(c.asInt("bikeRentalPickupCost", rentalDft.pickupCost()))
        .withPickupTime(c.asInt("bikeRentalPickupTime", rentalDft.pickupTime()))
        .withUseAvailabilityInformation(
          c.asBoolean(
            "useBikeRentalAvailabilityInformation",
            rentalDft.useAvailabilityInformation()
          )
        )
        .withArrivingInRentalVehicleAtDestinationCost(
          c.asDouble(
            "keepingRentedBicycleAtDestinationCost",
            rentalDft.arrivingInRentalVehicleAtDestinationCost()
          )
        );
    });
    request
      .journey()
      .rental()
      .setAllowArrivingInRentedVehicleAtDestination(
        c.asBoolean(
          "allowKeepingRentedBicycleAtDestination",
          request.journey().rental().allowArrivingInRentedVehicleAtDestination()
        )
      );

    preferences.withStreet(streetBuilder -> {
      var street = preferences.street();
      var dftStreet = dft.preferences().street();
      streetBuilder
        .withTurnReluctance(c.asDouble("turnReluctance", street.turnReluctance()))
        .withDrivingDirection(c.asEnum("drivingDirection", dftStreet.drivingDirection()))
        .withElevator(elevator -> {
          var dftElevator = dftStreet.elevator();
          elevator
            .withBoardCost(c.asInt("elevatorBoardCost", dftElevator.boardCost()))
            .withBoardTime(c.asInt("elevatorBoardTime", dftElevator.boardTime()))
            .withHopCost(c.asInt("elevatorHopCost", dftElevator.hopCost()))
            .withHopTime(c.asInt("elevatorHopTime", dftElevator.hopTime()));
        })
        .withMaxAccessEgressDuration(
          c.asDuration(
            "maxAccessEgressDuration",
            dftStreet.maxAccessEgressDuration().defaultValue()
          ),
          c.asEnumMap("maxAccessEgressDurationForMode", StreetMode.class, NodeAdapter::asDuration)
        )
        .withMaxDirectDuration(
          c.asDuration("maxDirectStreetDuration", dftStreet.maxDirectDuration().defaultValue()),
          c.asEnumMap("maxDirectStreetDurationForMode", StreetMode.class, NodeAdapter::asDuration)
        )
        .withIntersectionTraversalModel(
          c.asEnum("intersectionTraversalModel", dftStreet.intersectionTraversalModel())
        );
    });
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

    preferences.withItineraryFilter(it -> mapItineraryFilterParams(c.path("itineraryFilters"), it));

    preferences.withSystem(systemBuilder -> {
      var sysDft = preferences.system();
      systemBuilder
        .withGeoidElevation(c.asBoolean("geoidElevation", sysDft.geoidElevation()))
        .withMaxJourneyDuration(c.asDuration("maxJourneyDuration", sysDft.maxJourneyDuration()));
      if (OTPFeature.DataOverlay.isOn()) {
        systemBuilder.withDataOverlay(DataOverlayParametersMapper.map(c.path("dataOverlay")));
      }
    });

    request.carPickup = c.asBoolean("kissAndRide", dft.carPickup);
    request.setLocale(c.asLocale("locale", dft.locale()));

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
    request.parkAndRide = c.asBoolean("parkAndRide", dft.parkAndRide);
    request.setSearchWindow(c.asDuration("searchWindow", dft.searchWindow()));
    vehicleParking.setRequiredTags(
      c.asTextSet("requiredVehicleParkingTags", vehicleParking.requiredTags())
    );

    preferences.withParking(
      VehicleParkingPreferences.of(
        c.asBoolean(
          "useVehicleParkingAvailabilityInformation",
          preferences.parking().useAvailabilityInformation()
        )
      )
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

    var wheelchairNode = c.path("wheelchairAccessibility");
    preferences.setWheelchair(mapAccessibilityRequest(wheelchairNode));
    request.setWheelchair(wheelchairNode.asBoolean("enabled", false));

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
