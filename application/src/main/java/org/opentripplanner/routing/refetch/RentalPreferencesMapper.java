package org.opentripplanner.routing.refetch;

import org.opentripplanner.model.plan.itineraryreference.BikeRentalPreferences;
import org.opentripplanner.model.plan.itineraryreference.BikeWalkingPreferences;
import org.opentripplanner.model.plan.itineraryreference.CarRentalPreferences;
import org.opentripplanner.model.plan.itineraryreference.RentalDestinationPolicy;
import org.opentripplanner.model.plan.itineraryreference.RentalNetworks;
import org.opentripplanner.model.plan.itineraryreference.RidingPreferences;
import org.opentripplanner.model.plan.itineraryreference.ScooterRentalPreferences;
import org.opentripplanner.routing.api.request.preference.BikePreferences;
import org.opentripplanner.routing.api.request.preference.CarPreferences;
import org.opentripplanner.routing.api.request.preference.ScooterPreferences;
import org.opentripplanner.routing.api.request.preference.TimeSlopeSafetyTriangle;
import org.opentripplanner.routing.api.request.preference.VehicleRentalPreferences;

/**
 * Maps the bike, car and scooter preferences of a route request to the rental preferences stored
 * in an {@link org.opentripplanner.model.plan.itineraryreference.ItineraryReference}, and back
 * onto the preferences of a refetch request.
 * <p>
 * Only the preferences a client can set are stored. When mapped back, they replace those of the
 * refetch request; the others, like the rental pickup time, keep the server's current values.
 */
final class RentalPreferencesMapper {

  private RentalPreferencesMapper() {}

  static BikeRentalPreferences toBikeRental(BikePreferences bike) {
    var walking = bike.walking();
    return new BikeRentalPreferences(
      new RidingPreferences(
        bike.speed(),
        bike.reluctance(),
        bike.optimizeType(),
        bike.optimizeTriangle()
      ),
      new BikeWalkingPreferences(
        walking.speed(),
        walking.reluctance(),
        walking.mountDismountTime(),
        walking.mountDismountCost()
      ),
      toNetworks(bike.rental()),
      toDestinationPolicy(bike.rental())
    );
  }

  static void setBikeRental(BikePreferences.Builder bike, BikeRentalPreferences stored) {
    var riding = stored.riding();
    var walking = stored.walking();
    bike
      .withSpeed(riding.speed())
      .withReluctance(riding.reluctance())
      .withOptimizeType(riding.optimizeType())
      .withOptimizeTriangle(triangle -> setTriangle(triangle, riding.optimizeTriangle()))
      .withWalking(builder ->
        builder
          .withSpeed(walking.speed())
          .withReluctance(walking.reluctance())
          .withMountDismountTime(walking.mountDismountTime())
          .withMountDismountCost(walking.mountDismountCost().toSeconds())
      )
      .withRental(rental -> {
        setNetworks(rental, stored.networks());
        setDestinationPolicy(rental, stored.destinationPolicy());
      });
  }

  static CarRentalPreferences toCarRental(CarPreferences car) {
    return new CarRentalPreferences(car.reluctance(), toNetworks(car.rental()));
  }

  static void setCarRental(CarPreferences.Builder car, CarRentalPreferences stored) {
    car
      .withReluctance(stored.reluctance())
      .withRental(rental -> setNetworks(rental, stored.networks()));
  }

  static ScooterRentalPreferences toScooterRental(ScooterPreferences scooter) {
    return new ScooterRentalPreferences(
      new RidingPreferences(
        scooter.speed(),
        scooter.reluctance(),
        scooter.optimizeType(),
        scooter.optimizeTriangle()
      ),
      toNetworks(scooter.rental()),
      toDestinationPolicy(scooter.rental())
    );
  }

  static void setScooterRental(
    ScooterPreferences.Builder scooter,
    ScooterRentalPreferences stored
  ) {
    var riding = stored.riding();
    scooter
      .withSpeed(riding.speed())
      .withReluctance(riding.reluctance())
      .withOptimizeType(riding.optimizeType())
      .withOptimizeTriangle(triangle -> setTriangle(triangle, riding.optimizeTriangle()))
      .withRental(rental -> {
        setNetworks(rental, stored.networks());
        setDestinationPolicy(rental, stored.destinationPolicy());
      });
  }

  private static void setTriangle(
    TimeSlopeSafetyTriangle.Builder triangle,
    TimeSlopeSafetyTriangle stored
  ) {
    triangle.withTime(stored.time()).withSlope(stored.slope()).withSafety(stored.safety());
  }

  private static RentalNetworks toNetworks(VehicleRentalPreferences rental) {
    return new RentalNetworks(rental.allowedNetworks(), rental.bannedNetworks());
  }

  private static void setNetworks(VehicleRentalPreferences.Builder rental, RentalNetworks stored) {
    rental.withAllowedNetworks(stored.allowed()).withBannedNetworks(stored.banned());
  }

  private static RentalDestinationPolicy toDestinationPolicy(VehicleRentalPreferences rental) {
    return new RentalDestinationPolicy(
      rental.allowArrivingInRentedVehicleAtDestination(),
      rental.arrivingInRentalVehicleAtDestinationCost()
    );
  }

  private static void setDestinationPolicy(
    VehicleRentalPreferences.Builder rental,
    RentalDestinationPolicy stored
  ) {
    rental
      .withAllowArrivingInRentedVehicleAtDestination(stored.allowKeeping())
      .withArrivingInRentalVehicleAtDestinationCost(stored.keepingCost().toSeconds());
  }
}
