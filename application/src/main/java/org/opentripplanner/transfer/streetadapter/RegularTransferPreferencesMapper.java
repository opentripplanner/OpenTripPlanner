package org.opentripplanner.transfer.streetadapter;

import java.util.function.Consumer;
import org.opentripplanner.core.model.basic.Ratio;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.BikePreferences;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.VehicleRoutingOptimizeType;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.BicyclePreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.BicycleWalk;
import org.opentripplanner.transit.transfer.regular.api.preferences.CarPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.TimeSlopeSafetyTrianglePreference;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WheelchairPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WithSpeed;
import org.opentripplanner.transit.transfer.regular.spi.TransferRequestMapper;

/**
 * The mapping glue between {@code application}'s {@link RouteRequest} and the concrete preferences
 * type the {@code transit-transfer} module's regular-transfer pipeline caches and normalizes paths
 * under - {@link AbstractUserPreferences}.
 * <p>
 * The profile type of a request is decided by the transfer street mode of the request, and for
 * walking by the wheelchair flag: a {@code WALK} request is of the type
 * {@link TransferProfileType#WHEELCHAIR} if the request is wheelchair accessible, and of the type
 * {@link TransferProfileType#WALK} otherwise. A {@code BIKE} request is of the type
 * {@link TransferProfileType#BICYCLE}, and a {@code CAR} request of the type
 * {@link TransferProfileType#CAR}. A request with another transfer street mode is not accepted.
 */
public final class RegularTransferPreferencesMapper implements TransferRequestMapper<RouteRequest> {

  @Override
  public AbstractUserPreferences<?> toUserPreferences(RouteRequest request) {
    return toUserPreferences(profileType(request), request);
  }

  @Override
  public AbstractUserPreferences<?> toUserPreferences(
    TransferProfileType type,
    RouteRequest request
  ) {
    var appWalk = request.preferences().walk();
    var speed = Speed.ofMetersPerSecond(appWalk.speed());
    var reluctance = Reluctance.of(appWalk.reluctance());
    var safetyFactor = Ratio.of(appWalk.safetyFactor()).normalize();
    var appBike = request.preferences().bike();
    var appCar = request.preferences().car();

    return switch (type) {
      case WALK -> WalkPreferences.of()
        .withSpeed(speed)
        .withReluctance(reluctance)
        .withSafetyFactor(safetyFactor)
        .build();
      case WHEELCHAIR -> WheelchairPreferences.of()
        .withSpeed(speed)
        .withReluctance(reluctance)
        .build();
      case BICYCLE -> BicyclePreferences.of()
        .withSpeed(Speed.ofMetersPerSecond(appBike.speed()))
        .withReluctance(Reluctance.of(appBike.reluctance()))
        .withOptimize(optimize(appBike))
        .withWalk(
          BicycleWalk.of(
            Speed.ofMetersPerSecond(appBike.walking().speed()),
            Reluctance.of(appBike.walking().reluctance())
          )
        )
        .build();
      case CAR -> CarPreferences.of().withReluctance(Reluctance.of(appCar.reluctance())).build();
    };
  }

  @Override
  public RouteRequest toRequest(AbstractUserPreferences<?> userPreferences, RouteRequest base) {
    return switch (userPreferences) {
      case WalkPreferences walk -> walkRequest(walk, base);
      case WheelchairPreferences wheelchair -> wheelchairRequest(wheelchair, base);
      case BicyclePreferences bicycle -> bicycleRequest(bicycle, base);
      case CarPreferences car -> carRequest(car, base);
    };
  }

  /**
   * The profile type of a request.
   *
   * @throws IllegalArgumentException if the transfer street mode of the request has no profile type
   */
  private static TransferProfileType profileType(RouteRequest request) {
    var mode = request.journey().transfer().mode();
    return switch (mode) {
      case WALK -> request.journey().wheelchair()
        ? TransferProfileType.WHEELCHAIR
        : TransferProfileType.WALK;
      case BIKE -> TransferProfileType.BICYCLE;
      case CAR -> TransferProfileType.CAR;
      default -> throw new IllegalArgumentException(
        "The transfer street mode " + mode + " is not supported by the regular transfers."
      );
    };
  }

  /** A walking request for a traveler that is not in a wheelchair. */
  private static RouteRequest walkRequest(WalkPreferences userPreferences, RouteRequest base) {
    return walkingRequest(userPreferences, base, false, walk ->
      walk.withSafetyFactor(userPreferences.safetyFactor().asDouble())
    );
  }

  /**
   * A walking request for a traveler in a wheelchair. The street search does not use the safety
   * factor for a wheelchair, so it is the one of the base request.
   */
  private static RouteRequest wheelchairRequest(
    WheelchairPreferences userPreferences,
    RouteRequest base
  ) {
    return walkingRequest(userPreferences, base, true, walk -> {});
  }

  private static <T extends AbstractUserPreferences<T> & WithSpeed> RouteRequest walkingRequest(
    T userPreferences,
    RouteRequest base,
    boolean wheelchair,
    Consumer<org.opentripplanner.routing.api.request.preference.WalkPreferences.Builder> other
  ) {
    return base
      .copyOf()
      .withJourney(jb -> jb.withTransfer(StreetMode.WALK).withWheelchair(wheelchair))
      .withPreferences(routingPrefs ->
        routingPrefs.withWalk(walk -> {
          walk.withSpeed(userPreferences.speed().toMetersPerSecond());
          walk.withReluctance(userPreferences.reluctance().value());
          other.accept(walk);
        })
      )
      .buildDefault();
  }

  /** A request with a bicycle, that is pushed on stairs. */
  private static RouteRequest bicycleRequest(
    BicyclePreferences userPreferences,
    RouteRequest base
  ) {
    return base
      .copyOf()
      .withJourney(jb -> jb.withTransfer(StreetMode.BIKE))
      .withPreferences(rb ->
        rb.withBike(bike -> {
          bike.withSpeed(userPreferences.speed().toMetersPerSecond());
          bike.withReluctance(userPreferences.reluctance().value());
          // The triangle is the same as the shortest, flattest or safest type with one value of 1.0
          bike.withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE);
          bike.withOptimizeTriangle(triangle -> {
            triangle.withTime(userPreferences.optimize().time().asDouble());
            triangle.withSlope(userPreferences.optimize().slope().asDouble());
            triangle.withSafety(userPreferences.optimize().safety().asDouble());
          });
          bike.withWalking(walking -> {
            walking.withSpeed(userPreferences.walk().speed().toMetersPerSecond());
            walking.withReluctance(userPreferences.walk().reluctance().value());
          });
        })
      )
      .buildDefault();
  }

  /** A request with a car, the speed of the car is the speed of the streets. */
  private static RouteRequest carRequest(CarPreferences userPreferences, RouteRequest base) {
    return base
      .copyOf()
      .withJourney(jb -> jb.withTransfer(StreetMode.CAR))
      .withPreferences(pb ->
        pb.withCar(c -> c.withReluctance(userPreferences.reluctance().value()))
      )
      .buildDefault();
  }

  /** The optimization of a bike request as a triangle: the other types have one value of 1.0. */
  private static TimeSlopeSafetyTrianglePreference optimize(BikePreferences bike) {
    return switch (bike.optimizeType()) {
      case SHORTEST_DURATION -> TimeSlopeSafetyTrianglePreference.of(1.0, 0.0, 0.0);
      case FLAT_STREETS -> TimeSlopeSafetyTrianglePreference.of(0.0, 1.0, 0.0);
      case SAFE_STREETS -> TimeSlopeSafetyTrianglePreference.of(0.0, 0.0, 1.0);
      case TRIANGLE -> {
        var triangle = bike.optimizeTriangle();
        yield triangle.time() + triangle.slope() + triangle.safety() > 0.0
          ? TimeSlopeSafetyTrianglePreference.of(
              triangle.time(),
              triangle.slope(),
              triangle.safety()
            )
          : TimeSlopeSafetyTrianglePreference.of(0.0, 0.0, 1.0);
      }
    };
  }
}
