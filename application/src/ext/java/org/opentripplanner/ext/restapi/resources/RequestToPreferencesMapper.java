package org.opentripplanner.ext.restapi.resources;

import jakarta.validation.constraints.NotNull;
import java.util.function.Consumer;
import org.opentripplanner.ext.restapi.mapping.LegacyVehicleRoutingOptimizeType;
import org.opentripplanner.routing.algorithm.filterchain.api.TransitGeneralizedCostFilterParams;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterPreferences;
import org.opentripplanner.routing.api.request.preference.Relax;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleParkingPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleRentalPreferences;
import org.opentripplanner.utils.lang.ObjectUtils;

class RequestToPreferencesMapper {

  private final RoutingResource req;
  private final RoutingPreferences.Builder preferences;
  private final boolean isPlannedForNow;

  RequestToPreferencesMapper(
    RoutingResource req,
    RoutingPreferences.Builder preferences,
    boolean isPlannedForNow
  ) {
    this.req = req;
    this.preferences = preferences;
    this.isPlannedForNow = isPlannedForNow;
  }

  void map() {
    mapCar();
    mapWalk();
    mapBike();
    mapScooter();

    var boardAndAlightSlack = mapTransit();

    mapTransfer(boardAndAlightSlack);

    mapItineraryFilter();
    mapSystem();
  }

  private void mapCar() {
    preferences.withCar(car -> {
      setIfNotNull(req.carReluctance, car::withReluctance);
      car.withParking(parking -> {
        mapParking(parking);
        setIfNotNull(req.carParkCost, parking::withCost);
        setIfNotNull(req.carParkTime, parking::withTime);
      });
      car.withRental(this::mapRental);
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
      setIfNotNull(req.bikeSpeed, bike::withSpeed);
      setIfNotNull(req.bikeReluctance, bike::withReluctance);
      setIfNotNull(req.bikeBoardCost, bike::withBoardCost);
      setIfNotNull(req.bikeOptimizeType, optimizeType ->
        bike.withOptimizeType(LegacyVehicleRoutingOptimizeType.map(optimizeType))
      );

      if (req.bikeOptimizeType == LegacyVehicleRoutingOptimizeType.TRIANGLE) {
        bike.withOptimizeTriangle(triangle -> {
          setIfNotNull(req.triangleTimeFactor, triangle::withTime);
          setIfNotNull(req.triangleSlopeFactor, triangle::withSlope);
          setIfNotNull(req.triangleSafetyFactor, triangle::withSafety);
        });
      }

      bike.withParking(parking -> {
        mapParking(parking);
        setIfNotNull(req.bikeParkCost, parking::withCost);
        setIfNotNull(req.bikeParkTime, parking::withTime);
      });
      bike.withRental(this::mapRental);
      bike.withWalking(walk -> {
        setIfNotNull(req.bikeWalkingSpeed, walk::withSpeed);
        setIfNotNull(req.bikeWalkingReluctance, walk::withReluctance);
        setIfNotNull(req.bikeSwitchTime, walk::withMountDismountTime);
        setIfNotNull(req.bikeSwitchCost, walk::withMountDismountCost);
      });
    });
  }

  private void mapScooter() {
    preferences.withScooter(scooter -> {
      setIfNotNull(req.bikeSpeed, scooter::withSpeed);
      setIfNotNull(req.bikeReluctance, scooter::withReluctance);
      setIfNotNull(req.bikeOptimizeType, optimizeType ->
        scooter.withOptimizeType(LegacyVehicleRoutingOptimizeType.map(optimizeType))
      );

      if (req.bikeOptimizeType == LegacyVehicleRoutingOptimizeType.TRIANGLE) {
        scooter.withOptimizeTriangle(triangle -> {
          setIfNotNull(req.triangleTimeFactor, triangle::withTime);
          setIfNotNull(req.triangleSlopeFactor, triangle::withSlope);
          setIfNotNull(req.triangleSafetyFactor, triangle::withSafety);
        });
      }

      scooter.withRental(this::mapRental);
    });
  }

  private BoardAndAlightSlack mapTransit() {
    preferences.withTransit(tr -> {
      setIfNotNull(req.boardSlack, tr::withDefaultBoardSlackSec);
      setIfNotNull(req.alightSlack, tr::withDefaultAlightSlackSec);
      setIfNotNull(req.otherThanPreferredRoutesPenalty, tr::setOtherThanPreferredRoutesPenalty);
      setIfNotNull(req.ignoreRealtimeUpdates, tr::setIgnoreRealtimeUpdates);

      if (req.relaxTransitGroupPriority != null) {
        tr.withRelaxTransitGroupPriority(CostLinearFunction.of(req.relaxTransitGroupPriority));
      } else {
        setIfNotNull(req.relaxTransitSearchGeneralizedCostAtDestination, v ->
          tr.withRaptor(r -> r.withRelaxGeneralizedCostAtDestination(v))
        );
      }
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
        transfer.withSlackSec(req.minTransferTime - boardAndAlightSlack.value);
      }

      setIfNotNull(req.nonpreferredTransferPenalty, transfer::withNonpreferredCost);
      setIfNotNull(req.maxTransfers, transfer::withMaxTransfers);
      setIfNotNull(req.maxAdditionalTransfers, transfer::withMaxAdditionalTransfers);
    });
  }

  private void mapRental(VehicleRentalPreferences.Builder rental) {
    setIfNotNull(
      req.allowKeepingRentedBicycleAtDestination,
      rental::withAllowArrivingInRentedVehicleAtDestination
    );
    setIfNotNull(req.allowedVehicleRentalNetworks, rental::withAllowedNetworks);
    setIfNotNull(req.bannedVehicleRentalNetworks, rental::withBannedNetworks);

    setIfNotNull(req.keepingRentedBicycleAtDestinationCost, cost ->
      rental.withArrivingInRentalVehicleAtDestinationCost((int) Math.round(cost))
    );
    rental.withUseAvailabilityInformation(isPlannedForNow);
  }

  private void mapItineraryFilter() {
    preferences.withItineraryFilter(filter -> {
      setIfNotNull(req.debugItineraryFilter, filter::withDebug);
      setIfNotNull(req.groupSimilarityKeepOne, filter::withGroupSimilarityKeepOne);
      setIfNotNull(req.groupSimilarityKeepThree, filter::withGroupSimilarityKeepThree);
      setIfNotNull(
        req.groupedOtherThanSameLegsMaxCostMultiplier,
        filter::withGroupedOtherThanSameLegsMaxCostMultiplier
      );
      filter.withTransitGeneralizedCostLimit(mapTransitGeneralizedCostFilterParams(filter));
      setIfNotNull(req.nonTransitGeneralizedCostLimitFunction, it ->
        filter.withNonTransitGeneralizedCostLimit(CostLinearFunction.of(it))
      );
    });
  }

  private TransitGeneralizedCostFilterParams mapTransitGeneralizedCostFilterParams(
    ItineraryFilterPreferences.Builder filter
  ) {
    var costLimitFunction = (req.transitGeneralizedCostLimitFunction == null)
      ? filter.original().transitGeneralizedCostLimit().costLimitFunction()
      : CostLinearFunction.of(req.transitGeneralizedCostLimitFunction);

    var intervalRelaxFactor = ObjectUtils.ifNotNull(
      req.transitGeneralizedCostLimitIntervalRelaxFactor,
      filter.original().transitGeneralizedCostLimit().intervalRelaxFactor()
    );

    return new TransitGeneralizedCostFilterParams(costLimitFunction, intervalRelaxFactor);
  }

  private void mapParking(VehicleParkingPreferences.Builder builder) {
    builder.withRequiredVehicleParkingTags(req.requiredVehicleParkingTags);
    builder.withBannedVehicleParkingTags(req.bannedVehicleParkingTags);
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

  static <T> void mapRelaxIfNotNull(String fx, @NotNull Consumer<Relax> body) {
    if (fx == null) {
      return;
    }
    var a = fx.split("[\\sxXuUvVtT*+]+");
    if (a.length != 2) {
      return;
    }
    body.accept(new Relax(Double.parseDouble(a[0]), Integer.parseInt(a[1])));
  }

  /**
   * The combined value of board and alight slack is used in the initialization of transfer
   * preferences.
   * <p>
   * This class is used to pass a type-safe value from one phase of the mapping to another. This
   * enforces the mapping order: 'transit' before 'transfer'. */
  private record BoardAndAlightSlack(int value) {}
}
