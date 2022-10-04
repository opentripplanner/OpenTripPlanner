package org.opentripplanner.api.common;

import java.util.function.Consumer;
import javax.validation.constraints.NotNull;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleParkingPreferences;
import org.opentripplanner.routing.core.BicycleOptimizeType;

class RequestToPreferencesMapper {

  private final RoutingResource req;
  private final RoutingPreferences.Builder preferences;
  private final boolean vehicleRental;
  private final boolean isPlannedForNow;

  RequestToPreferencesMapper(
    RoutingResource req,
    RoutingPreferences.Builder preferences,
    boolean vehicleRental,
    boolean isPlannedForNow
  ) {
    this.req = req;
    this.preferences = preferences;
    this.vehicleRental = vehicleRental;
    this.isPlannedForNow = isPlannedForNow;
  }

  void map() {
    mapCar();
    mapWalk();
    mapBike();

    var boardAndAlightSlack = mapTransit();

    mapTransfer(boardAndAlightSlack);

    maptRental();
    mapItineraryFilter();
    mapParking();
    mapSystem();
  }

  private void mapCar() {
    preferences.withCar(car -> {
      setIfNotNull(req.carReluctance, car::withReluctance);
      setIfNotNull(req.carParkCost, car::withParkCost);
      setIfNotNull(req.carParkTime, car::withParkTime);
    });
  }

  private void mapWalk() {
    preferences.withWalk(walk -> {
      setIfNotNull(req.walkReluctance, walk::withReluctance);
      setIfNotNull(req.walkSpeed, walk::withSpeed);
      setIfNotNull(req.walkBoardCost, walk::withBoardCost);
      setIfNotNull(req.walkSafetyFactor, walk::withSafetyFactor);
    });
  }

  private void mapBike() {
    preferences.withBike(bike -> {
      setIfNotNull(req.bikeSpeed, bike::setSpeed);
      setIfNotNull(req.bikeReluctance, bike::setReluctance);
      setIfNotNull(req.bikeBoardCost, bike::setBoardCost);
      setIfNotNull(req.bikeWalkingSpeed, bike::setWalkingSpeed);
      setIfNotNull(req.bikeWalkingReluctance, bike::setWalkingReluctance);
      setIfNotNull(req.bikeParkCost, bike::setParkCost);
      setIfNotNull(req.bikeParkTime, bike::setParkTime);
      setIfNotNull(req.bikeSwitchTime, bike::setSwitchTime);
      setIfNotNull(req.bikeSwitchCost, bike::setSwitchCost);
      setIfNotNull(req.bikeOptimizeType, bike::setOptimizeType);

      if (req.bikeOptimizeType == BicycleOptimizeType.TRIANGLE) {
        bike.withOptimizeTriangle(triangle -> {
          setIfNotNull(req.triangleTimeFactor, triangle::withTime);
          setIfNotNull(req.triangleSlopeFactor, triangle::withSlope);
          setIfNotNull(req.triangleSafetyFactor, triangle::withSafety);
        });
      }
      if (vehicleRental && req.bikeSpeed == null) {
        //slower bike speed for bike sharing, based on empirical evidence from DC.
        bike.setSpeed(4.3);
      }
    });
  }

  private BoardAndAlightSlack mapTransit() {
    preferences.withTransit(tr -> {
      setIfNotNull(req.boardSlack, tr::withDefaultBoardSlackSec);
      setIfNotNull(req.alightSlack, tr::withDefaultAlightSlackSec);
      setIfNotNull(req.otherThanPreferredRoutesPenalty, tr::setOtherThanPreferredRoutesPenalty);
      setIfNotNull(req.ignoreRealtimeUpdates, tr::setIgnoreRealtimeUpdates);
    });

    return new BoardAndAlightSlack(
      preferences.transit().boardSlack().defaultValueSeconds() +
      preferences.transit().alightSlack().defaultValueSeconds()
    );
  }

  private void mapTransfer(BoardAndAlightSlack boardAndAlightSlack) {
    preferences.withTransfer(transfer -> {
      setIfNotNull(req.waitReluctance, transfer::withWaitReluctance);
      setIfNotNull(req.transferPenalty, transfer::withCost);

      if (req.minTransferTime != null) {
        if (boardAndAlightSlack.value > req.minTransferTime) {
          throw new IllegalArgumentException(
            "Invalid parameters: 'minTransferTime' must be greater than or equal to board slack plus alight slack"
          );
        }
        transfer.withSlack(req.minTransferTime - boardAndAlightSlack.value);
      }

      setIfNotNull(req.nonpreferredTransferPenalty, transfer::withNonpreferredCost);
      setIfNotNull(req.maxTransfers, transfer::withMaxTransfers);
    });
  }

  private void maptRental() {
    preferences.withRental(rental -> {
      setIfNotNull(
        req.keepingRentedBicycleAtDestinationCost,
        rental::withArrivingInRentalVehicleAtDestinationCost
      );
      rental.withUseAvailabilityInformation(isPlannedForNow);
    });
  }

  private void mapItineraryFilter() {
    setIfNotNull(
      req.debugItineraryFilter,
      value -> preferences.withItineraryFilter(it -> it.withDebug(value))
    );
  }

  private void mapParking() {
    setIfNotNull(
      req.useVehicleParkingAvailabilityInformation,
      value -> preferences.withParking(VehicleParkingPreferences.of(value))
    );
  }

  private void mapSystem() {
    preferences.withSystem(system -> {
      setIfNotNull(req.geoidElevation, system::withGeoidElevation);
    });
  }

  static <T> void setIfNotNull(T value, @NotNull Consumer<T> body) {
    if (value != null) {
      body.accept(value);
    }
  }

  /**
   * The combined value of board and alight slack is used in the initialization of transfer
   * preferences.
   * <p>
   * This class is used to pass a type-safe value from one phase of the mapping to another. This
   * enforces the mapping order: 'transit' before 'transfer'. */
  private record BoardAndAlightSlack(int value) {}
}
