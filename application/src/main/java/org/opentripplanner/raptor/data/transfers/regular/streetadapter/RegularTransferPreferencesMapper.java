package org.opentripplanner.raptor.data.transfers.regular.streetadapter;

import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.api.ways.StreetSegmentTypeUsage;

/**
 * The mapping glue between {@code application}'s traveler-preference model and the concrete
 * preferences type the {@code transit-transfer} module's regular-transfer pipeline caches and
 * normalizes paths under - {@link AbstractUserPreferences}. Only {@code WALK} is supported today
 * (see {@code TransferProfileType}); this mapper is deliberately minimal until the traveler
 * preferences model is designed further.
 * <p>
 * {@link #fromRouteRequest} extracts a live request's walk preferences into the cache-friendly
 * {@link WalkPreferences} value (request time, before a {@link
 * org.opentripplanner.transit.transfer.regular.RegularTransferServiceFactory} lookup). {@link
 * #toRouteRequest} reconstructs a minimal {@link RouteRequest} from a stored {@link
 * AbstractUserPreferences} - the only place {@link StreetTransferPathProvider} needs a real
 * {@code RouteRequest}/{@code StreetSearchRequest} to actually traverse streets.
 */
public final class RegularTransferPreferencesMapper {

  private RegularTransferPreferencesMapper() {}

  public static WalkPreferences fromRouteRequest(RouteRequest request) {
    var appWalk = request.preferences().walk();
    return WalkPreferences.of()
      .withSpeed(Speed.ofMetersPerSecond(appWalk.speed()))
      .withReluctance(Reluctance.of(appWalk.reluctance()))
      .build();
  }

  public static RouteRequest toRouteRequest(AbstractUserPreferences<?> preferences) {
    var builder = RouteRequest.defaultValue().copyOf();
    builder.withJourney(jb -> jb.withTransfer(new StreetRequest(StreetMode.WALK)));
    builder.withPreferences(routingPrefs ->
      routingPrefs.withWalk(w -> {
        w.withSpeed(preferences.speed().toMetersPerSecond());
        w.withReluctance(preferences.reluctance().value());
        w.withStairsReluctance(stairsReluctance(preferences.stairs(), w.stairsReluctance()));
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
