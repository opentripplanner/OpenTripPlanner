package org.opentripplanner.transfer.streetadapter;

import org.opentripplanner.core.model.basic.Ratio;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.BicyclePreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.BicycleWalk;
import org.opentripplanner.transit.transfer.regular.api.preferences.CarPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WheelchairPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WithSafetyFactor;
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
        .withSafetyFactor(safetyFactor)
        .build();
      case BICYCLE -> BicyclePreferences.of()
        .withSpeed(Speed.ofMetersPerSecond(appBike.speed()))
        .withReluctance(Reluctance.of(appBike.reluctance()))
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
      case WalkPreferences walk -> walkRequest(walk, base, false);
      case WheelchairPreferences wheelchair -> walkRequest(wheelchair, base, true);
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

  /** A walking request, for a traveler in a wheelchair or not. */
  private static <
    T extends AbstractUserPreferences<T> & WithSpeed & WithSafetyFactor
  > RouteRequest walkRequest(T userPreferences, RouteRequest base, boolean wheelchair) {
    return base
      .copyOf()
      .withJourney(jb -> jb.withTransfer(StreetMode.WALK).withWheelchair(wheelchair))
      .withPreferences(routingPrefs ->
        routingPrefs.withWalk(walk -> {
          walk.withSpeed(userPreferences.speed().toMetersPerSecond());
          walk.withReluctance(userPreferences.reluctance().value());
          walk.withSafetyFactor(userPreferences.safetyFactor().asDouble());
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
}
