package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.corridor.CarpoolCorridor.CorridorStop;
import org.opentripplanner.ext.carpooling.corridor.CarpoolStopIndex;
import org.opentripplanner.ext.carpooling.corridor.RoutableCarpoolTrip;
import org.opentripplanner.ext.carpooling.street.CarReachableVertexSnapper.SnapResult;
import org.opentripplanner.ext.carpooling.street.StreetPathUtils;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.street.search.request.StreetSearchRequest;

/**
 * Finds the transit stops the passenger of an access or egress request can use with a trip. An
 * access passenger is dropped off for a stop and walks to it; an egress passenger walks from a stop
 * and is picked up.
 * <p>
 * Not thread-safe; one instance per request.
 */
public class UsableStops {

  private final CarpoolStopIndex stopIndex;
  private final boolean access;
  private final StreetSearchRequest streetSearchRequest;
  private final Duration maxWalk;
  private final Map<FeedScopedId, SnapResult> snaps = new HashMap<>();

  /** A stop the passenger can use on one leg of a trip, with the vertex the car stops at for it. */
  public record UsableStop(CorridorStop stop, SnapResult snap) {}

  public UsableStops(
    CarpoolStopIndex stopIndex,
    AccessEgressType accessOrEgress,
    StreetSearchRequest streetSearchRequest,
    Duration maxWalk
  ) {
    this.stopIndex = stopIndex;
    this.access = accessOrEgress.isAccess();
    this.streetSearchRequest = streetSearchRequest;
    this.maxWalk = maxWalk;
  }

  /** The stops of the trip's corridor that the passenger can use within the walk budget. */
  public List<UsableStop> of(RoutableCarpoolTrip trip) {
    var usable = new ArrayList<UsableStop>();
    for (var stop : trip.corridor().stops()) {
      boolean served = access ? stop.servesDropoff() : stop.servesPickup();
      if (!served) {
        continue;
      }
      var snap = snap(stop.stopId());
      if (snap == null || StreetPathUtils.durationOrZero(snap.walkPath()).compareTo(maxWalk) > 0) {
        continue;
      }
      usable.add(new UsableStop(stop, snap));
    }
    return usable;
  }

  /** The stop's snap, found the first time the request asks for it. */
  @Nullable
  private SnapResult snap(FeedScopedId stopId) {
    if (!snaps.containsKey(stopId)) {
      snaps.put(stopId, findSnap(stopId));
    }
    return snaps.get(stopId);
  }

  /**
   * The stop's snap with its walk replayed under the request's street preferences, or {@code null}
   * when the stop has no snap or the passenger cannot take its walk, for instance by wheelchair.
   * The snap itself is chosen with the default preferences, so only that walk is checked.
   */
  @Nullable
  private SnapResult findSnap(FeedScopedId stopId) {
    var snap = access ? stopIndex.dropoffSnap(stopId) : stopIndex.pickupSnap(stopId);
    if (snap == null || snap.walkPath() == null) {
      return snap;
    }
    var walk = StreetPathUtils.replay(snap.walkPath(), streetSearchRequest);
    return walk == null ? null : new SnapResult(snap.vertex(), walk);
  }
}
