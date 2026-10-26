package org.opentripplanner.transfer.streetadapter;

import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.api.ways.StreetSegmentTypeUsage;
import org.opentripplanner.transit.transfer.regular.spi.TransferRequestMapper;

/**
 * The mapping glue between {@code application}'s {@link RouteRequest} and the concrete preferences
 * type the {@code transit-transfer} module's regular-transfer pipeline caches and normalizes paths
 * under - {@link AbstractUserPreferences}. Only {@code WALK} is supported today.
 * <p>
 * {@link #toUserPreferences} extracts a live request's walk preferences into the cache-friendly
 * {@link WalkPreferences} value, the key of the Raptor transfers cached per preferences. {@link
 * #toRequest} applies stored {@link AbstractUserPreferences} on a base request - the only place
 * {@link StreetTransferPathProvider} gets the real {@code RouteRequest}/{@code
 * StreetSearchRequest} it needs to actually traverse streets.
 */
public final class RegularTransferPreferencesMapper implements TransferRequestMapper<RouteRequest> {

  @Override
  public WalkPreferences toUserPreferences(RouteRequest request) {
    var appWalk = request.preferences().walk();
    return WalkPreferences.of()
      .withSpeed(Speed.ofMetersPerSecond(appWalk.speed()))
      .withReluctance(Reluctance.of(appWalk.reluctance()))
      .build();
  }

  @Override
  public RouteRequest toRequest(AbstractUserPreferences<?> userPreferences, RouteRequest base) {
    var builder = base.copyOf();
    builder.withJourney(jb -> jb.withTransfer(new StreetRequest(StreetMode.WALK)));
    builder.withPreferences(routingPrefs ->
      routingPrefs.withWalk(w -> {
        w.withSpeed(userPreferences.speed().toMetersPerSecond());
        w.withReluctance(userPreferences.reluctance().value());
        w.withStairsReluctance(stairsReluctance(userPreferences.stairs(), w.stairsReluctance()));
      })
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
