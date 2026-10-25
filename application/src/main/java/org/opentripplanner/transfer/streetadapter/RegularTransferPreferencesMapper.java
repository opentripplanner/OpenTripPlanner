package org.opentripplanner.transfer.streetadapter;

import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.BicyclePreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WheelchairPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WithStairs;
import org.opentripplanner.transit.transfer.regular.api.ways.StreetSegmentTypeUsage;
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
 * {@link TransferProfileType#BICYCLE}. A request with another transfer street mode is not
 * accepted.
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
    var appBike = request.preferences().bike();
    return switch (type) {
      case WALK -> WalkPreferences.of().withSpeed(speed).withReluctance(reluctance).build();
      case WHEELCHAIR -> WheelchairPreferences.of()
        .withSpeed(speed)
        .withReluctance(reluctance)
        .build();
      case BICYCLE -> BicyclePreferences.of()
        .withSpeed(Speed.ofMetersPerSecond(appBike.speed()))
        .withReluctance(Reluctance.of(appBike.reluctance()))
        .build();
    };
  }

  /**
   * The profile type of a request.
   *
   * @throws IllegalArgumentException if the transfer street mode of the request has no profile type
   */
  static TransferProfileType profileType(RouteRequest request) {
    var mode = request.journey().transfer().mode();
    if (mode == StreetMode.WALK) {
      return request.journey().wheelchair()
        ? TransferProfileType.WHEELCHAIR
        : TransferProfileType.WALK;
    }
    if (mode == StreetMode.BIKE) {
      return TransferProfileType.BICYCLE;
    }
    throw new IllegalArgumentException(
      "The transfer street mode " + mode + " is not supported by the regular transfers."
    );
  }

  @Override
  public RouteRequest toRequest(AbstractUserPreferences<?> userPreferences, RouteRequest base) {
    return switch (userPreferences) {
      case WalkPreferences walk -> walkRequest(walk, base, false);
      case WheelchairPreferences wheelchair -> walkRequest(wheelchair, base, true);
      case BicyclePreferences bicycle -> bicycleRequest(bicycle, base);
    };
  }

  /** A walking request, for a traveler in a wheelchair or not. */
  private static <T extends AbstractUserPreferences<T> & WithStairs> RouteRequest walkRequest(
    T userPreferences,
    RouteRequest base,
    boolean wheelchair
  ) {
    var builder = base.copyOf();
    builder.withJourney(jb ->
      jb.withTransfer(new StreetRequest(StreetMode.WALK)).withWheelchair(wheelchair)
    );
    builder.withPreferences(routingPrefs ->
      routingPrefs.withWalk(w -> {
        w.withSpeed(userPreferences.speed().toMetersPerSecond());
        w.withReluctance(userPreferences.reluctance().value());
        w.withStairsReluctance(stairsReluctance(userPreferences.stairs(), w.stairsReluctance()));
      })
    );
    return builder.buildDefault();
  }

  /** A request with a bicycle, that is pushed on stairs. */
  private static RouteRequest bicycleRequest(
    BicyclePreferences userPreferences,
    RouteRequest base
  ) {
    var builder = base.copyOf();
    builder.withJourney(jb ->
      jb.withTransfer(new StreetRequest(StreetMode.BIKE)).withWheelchair(false)
    );
    builder.withPreferences(routingPrefs ->
      routingPrefs
        .withBike(b -> {
          b.withSpeed(userPreferences.speed().toMetersPerSecond());
          b.withReluctance(userPreferences.reluctance().value());
        })
        .withWalk(w ->
          w.withStairsReluctance(stairsReluctance(userPreferences.stairs(), w.stairsReluctance()))
        )
    );
    return builder.buildDefault();
  }

  /** Interim mapping until the traveler-preferences design (stairs as a 3-way enum) is finalized. */
  private static double stairsReluctance(StreetSegmentTypeUsage stairs, double defaultReluctance) {
    return switch (stairs) {
      case ALLOWED -> defaultReluctance;
      case DISCOURAGED -> 10.0;
      case FORBIDDEN -> 1000.0;
    };
  }
}
