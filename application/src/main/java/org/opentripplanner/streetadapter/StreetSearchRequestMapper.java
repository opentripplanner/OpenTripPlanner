package org.opentripplanner.streetadapter;

import java.time.Instant;
import java.util.List;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.AccessibilityPreferences;
import org.opentripplanner.routing.api.request.preference.BikePreferences;
import org.opentripplanner.routing.api.request.preference.CarPreferences;
import org.opentripplanner.routing.api.request.preference.ElevatorPreferences;
import org.opentripplanner.routing.api.request.preference.EscalatorPreferences;
import org.opentripplanner.routing.api.request.preference.ScooterPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleParkingPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleRentalPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleWalkingPreferences;
import org.opentripplanner.routing.api.request.preference.WalkPreferences;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.routing.api.request.preference.filter.VehicleParkingFilter;
import org.opentripplanner.routing.api.request.preference.filter.VehicleParkingSelect;
import org.opentripplanner.street.search.intersection_model.IntersectionTraversalCalculator;
import org.opentripplanner.street.search.request.AccessibilityRequest;
import org.opentripplanner.street.search.request.BikeRequest;
import org.opentripplanner.street.search.request.CarRequest;
import org.opentripplanner.street.search.request.ElevatorRequest;
import org.opentripplanner.street.search.request.EscalatorRequest;
import org.opentripplanner.street.search.request.ParkingRequest;
import org.opentripplanner.street.search.request.RentalPeriod;
import org.opentripplanner.street.search.request.RentalRequest;
import org.opentripplanner.street.search.request.ScooterRequest;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.request.StreetSearchRequestBuilder;
import org.opentripplanner.street.search.request.TimeSlopeSafetyTriangle;
import org.opentripplanner.street.search.request.VehicleWalkingRequest;
import org.opentripplanner.street.search.request.WalkRequest;
import org.opentripplanner.street.search.request.WheelchairRequest;
import org.opentripplanner.street.search.request.filter.ParkingFilter;
import org.opentripplanner.street.search.request.filter.ParkingSelect;
import org.opentripplanner.street.search.request.filter.ParkingSelect.TagsSelect;

public class StreetSearchRequestMapper {

  /// Maps a [RouteRequest] to a [StreetSearchRequestBuilder] transferring all parameters
  /// relevant for street routing.
  public static StreetSearchRequestBuilder map(RouteRequest request) {
    var time = request.dateTime() == null ? RouteRequest.normalizeNow() : request.dateTime();
    var preferences = request.preferences();
    var street = preferences.street();
    var streetSearchRequestBuilder = StreetSearchRequest.of()
      .withStartTime(time)
      .withArriveBy(request.arriveBy())
      .withFrom(mapGenericLocation(request.from()))
      .withTo(mapGenericLocation(request.to()))
      .withWheelchairEnabled(request.journey().wheelchair())
      .withGeoidElevation(preferences.system().geoidElevation())
      .withTurnReluctance(street.turnReluctance())
      .withWheelchair(b -> mapWheelchair(b, preferences.wheelchair()))
      .withWalk(b -> mapWalk(b, preferences.walk()))
      .withBike(b -> mapBike(b, preferences.bike()))
      .withCar(b -> mapCar(b, preferences.car()))
      .withScooter(b -> mapScooter(b, preferences.scooter()))
      .withElevator(b -> mapElevator(b, street.elevator()))
      .withIntersectionTraversalCalculator(
        IntersectionTraversalCalculator.create(
          street.intersectionTraversalModel(),
          street.drivingDirection()
        )
      )
      .withTimeout(street.routingTimeout());

    var rentalDuration = request.journey().direct().rentalDuration();
    if (rentalDuration != null) {
      var rentalPeriod = request.arriveBy()
        ? RentalPeriod.createFromLatestArrivalTime(time, rentalDuration)
        : RentalPeriod.createFromEarliestDepartureTime(time, rentalDuration);
      streetSearchRequestBuilder.withRentalPeriod(rentalPeriod);
    }

    return streetSearchRequestBuilder;
  }

  /// Maps a [RouteRequest] to a [StreetSearchRequestBuilder] for transfer requests, where some
  /// special rules apply:
  ///
  ///  - they are always depart-at
  ///  - their start time is always the epoch (0)
  ///
  public static StreetSearchRequestBuilder mapToTransferRequest(RouteRequest request) {
    return map(request)
      .withFrom(null)
      .withTo(null)
      // transfer requests are always depart-at
      .withArriveBy(false)
      .withStartTime(Instant.ofEpochSecond(0))
      .withMode(request.journey().transfer().mode());
  }

  // private methods

  @Nullable
  private static Coordinate mapGenericLocation(@Nullable GenericLocation location) {
    if (location != null) {
      return location.getCoordinate();
    } else {
      return null;
    }
  }

  private static void mapWheelchair(WheelchairRequest.Builder b, WheelchairPreferences wheelchair) {
    b
      .withStop(mapAccessibility(wheelchair.stop()))
      .withElevator(mapAccessibility(wheelchair.elevator()))
      .withMaxSlope(wheelchair.maxSlope())
      .withSlopeExceededReluctance(wheelchair.slopeExceededReluctance())
      .withStairsReluctance(wheelchair.stairsReluctance())
      .withInaccessibleStreetReluctance(wheelchair.inaccessibleStreetReluctance());
  }

  private static AccessibilityRequest mapAccessibility(AccessibilityPreferences stop) {
    var b = AccessibilityRequest.of()
      .withInaccessibleCost(stop.inaccessibleCost())
      .withUnknownCost(stop.unknownCost());
    if (stop.onlyConsiderAccessible()) {
      b.withAccessibleOnly();
    }
    return b.build();
  }

  private static void mapWalk(WalkRequest.Builder b, WalkPreferences pref) {
    b
      .withSpeed(pref.speed())
      .withReluctance(pref.reluctance())
      .withStairsReluctance(pref.stairsReluctance())
      .withStairsTimeFactor(pref.stairsTimeFactor())
      .withSafetyFactor(pref.safetyFactor())
      .withEscalator(b2 -> mapEscalator(b2, pref.escalator()));
  }

  private static void mapEscalator(EscalatorRequest.Builder b, EscalatorPreferences escalator) {
    b.withReluctance(escalator.reluctance()).withSpeed(escalator.speed());
  }

  private static void mapBike(BikeRequest.Builder b, BikePreferences preferences) {
    b
      .withReluctance(preferences.reluctance())
      .withSpeed(preferences.speed())
      .withParking(b2 -> mapParking(b2, preferences.parking()))
      .withRental(b2 -> mapRental(b2, preferences.rental()))
      .withOptimizeType(preferences.optimizeType())
      .withOptimizeTriangle(mapTriangle(preferences.optimizeTriangle()))
      .withWalking(b2 -> mapVehicleWalking(b2, preferences.walking()));
  }

  private static TimeSlopeSafetyTriangle mapTriangle(
    org.opentripplanner.routing.api.request.preference.TimeSlopeSafetyTriangle original
  ) {
    return TimeSlopeSafetyTriangle.of()
      .withTime(original.time())
      .withSlope(original.slope())
      .withSafety(original.safety())
      .build();
  }

  private static void mapCar(CarRequest.Builder b, CarPreferences car) {
    b
      .withReluctance(car.reluctance())
      .withParking(b2 -> mapParking(b2, car.parking()))
      .withRental(b2 -> mapRental(b2, car.rental()))
      .withPickupTime(car.pickupTime())
      .withPickupCost(car.pickupCost())
      .withAccelerationSpeed(car.accelerationSpeed())
      .withDecelerationSpeed(car.decelerationSpeed());
  }

  private static void mapScooter(ScooterRequest.Builder b, ScooterPreferences scooter) {
    b
      .withSpeed(scooter.speed())
      .withReluctance(scooter.reluctance())
      .withRental(b2 -> mapRental(b2, scooter.rental()))
      .withOptimizeType(scooter.optimizeType())
      .withOptimizeTriangle(mapTriangle(scooter.optimizeTriangle()))
      .build();
  }

  private static void mapVehicleWalking(
    VehicleWalkingRequest.Builder b,
    VehicleWalkingPreferences walking
  ) {
    b
      .withSpeed(walking.speed())
      .withReluctance(walking.reluctance())
      .withStairsReluctance(walking.stairsReluctance())
      .withMountDismountCost(walking.mountDismountCost())
      .withMountDismountTime(walking.mountDismountTime());
  }

  private static void mapRental(RentalRequest.Builder b, VehicleRentalPreferences rental) {
    b
      .withPickupTime(rental.pickupTime())
      .withPickupCost(rental.pickupCost())
      .withDropOffTime(rental.dropOffTime())
      .withDropOffCost(rental.dropOffCost())
      .withUseAvailabilityInformation(rental.useAvailabilityInformation())
      .withAllowArrivingInRentedVehicleAtDestination(
        rental.allowArrivingInRentedVehicleAtDestination()
      )
      .withArrivingInRentalVehicleAtDestinationCost(
        rental.arrivingInRentalVehicleAtDestinationCost()
      )
      .withBannedNetworks(rental.bannedNetworks())
      .withAllowedNetworks(rental.allowedNetworks())
      .withElectricAssistSlopeSensitivity(rental.electricAssistSlopeSensitivity());
  }

  private static void mapParking(ParkingRequest.Builder b, VehicleParkingPreferences pref) {
    b
      .withUnpreferredTagCost(pref.unpreferredVehicleParkingTagCost())
      .withFilter(mapParkingFilter(pref.filter()))
      .withPreferred(mapParkingFilter(pref.preferred()))
      .withTime(pref.time())
      .withCost(pref.cost());
  }

  private static ParkingFilter mapParkingFilter(VehicleParkingFilter filter) {
    return new ParkingFilter(mapTagSelect(filter.not()), mapTagSelect(filter.select()));
  }

  private static List<ParkingSelect> mapTagSelect(List<VehicleParkingSelect> selects) {
    return selects
      .stream()
      .map(s -> new TagsSelect(s.tags()))
      .map(ParkingSelect.class::cast)
      .toList();
  }

  private static void mapElevator(ElevatorRequest.Builder b, ElevatorPreferences elevator) {
    b
      .withBoardCost(elevator.boardCost())
      .withBoardSlack(elevator.boardSlack())
      .withHopTime(elevator.hopTime())
      .withReluctance(elevator.reluctance())
      .build();
  }
}
