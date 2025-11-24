package org.opentripplanner.street.search.request;

import java.time.Instant;
import java.util.List;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.AccessibilityPreferences;
import org.opentripplanner.routing.api.request.preference.BikePreferences;
import org.opentripplanner.routing.api.request.preference.CarPreferences;
import org.opentripplanner.routing.api.request.preference.ElevatorPreferences;
import org.opentripplanner.routing.api.request.preference.EscalatorPreferences;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.preference.ScooterPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleParkingPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleRentalPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleWalkingPreferences;
import org.opentripplanner.routing.api.request.preference.WalkPreferences;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.routing.api.request.preference.filter.VehicleParkingFilter;
import org.opentripplanner.routing.api.request.preference.filter.VehicleParkingSelect;
import org.opentripplanner.street.search.request.filter.ParkingFilter;
import org.opentripplanner.street.search.request.filter.ParkingSelect;
import org.opentripplanner.street.search.request.filter.ParkingSelect.TagsSelect;

public class StreetSearchRequestMapper {

  public static StreetSearchRequestBuilder mapInternal(RouteRequest request) {
    var time = request.dateTime() == null ? RouteRequest.normalizeNow() : request.dateTime();
    final RoutingPreferences preferences = request.preferences();
    return StreetSearchRequest.of()
      .withStartTime(time)
      .withArriveBy(request.arriveBy())
      .withFrom(request.from())
      .withTo(request.to())
      .withWheelchairEnabled(request.journey().wheelchair())
      .withGeoidElevation(preferences.system().geoidElevation())
      .withTurnReluctance(preferences.street().turnReluctance())
      .withWheelchair(b -> mapWheelchair(b, request.preferences().wheelchair()))
      .withWalk(b -> mapWalk(b, preferences.walk()))
      .withBike(b -> mapBike(b, preferences.bike()))
      .withCar(b -> mapCar(b, preferences.car()))
      .withScooter(b -> mapScooter(b, preferences.scooter()))
      .withElevator(b -> mapElevator(b, preferences.street().elevator()));
  }

  public static StreetSearchRequestBuilder mapToTransferRequest(RouteRequest request) {
    return mapInternal(request)
      .withFrom(null)
      .withTo(null)
      // transfer requests are always depart-at
      .withArriveBy(false)
      .withStartTime(Instant.ofEpochSecond(0))
      .withMode(request.journey().transfer().mode());
  }

  // private methods

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
      .withBoardCost(preferences.boardCost())
      .withParking(b2 -> mapParking(b2, preferences.parking()))
      .withRental(b2 -> mapRental(b2, preferences.rental()))
      .withOptimizeType(preferences.optimizeType())
      .withOptimizeTriangle(preferences.optimizeTriangle())
      .withWalking(b2 -> mapVehicleWalking(b2, preferences.walking()));
  }

  private static void mapCar(CarRequest.Builder b, CarPreferences car) {
    b
      .withReluctance(car.reluctance())
      .withBoardCost(car.boardCost())
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
      .withOptimizeTriangle(scooter.optimizeTriangle())
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
      .withAllowedNetworks(rental.allowedNetworks());
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
      .withBoardTime(elevator.boardTime())
      .withHopCost(elevator.hopCost())
      .withHopTime(elevator.hopTime())
      .build();
  }
}
