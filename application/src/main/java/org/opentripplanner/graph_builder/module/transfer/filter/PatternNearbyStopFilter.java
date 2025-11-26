package org.opentripplanner.graph_builder.module.transfer.filter;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitService;

/**
 * Filters nearby stops based on trip pattern availability.
 * <p>
 * This filter ensures that transfers are only generated between stops that are served by trip
 * patterns. For each trip pattern passing nearby, it keeps only the closest stop where boarding
 * or alighting is possible (depending on direction).
 * <p>
 * Stops without patterns may still be included if they are marked as sometimes-used by real-time
 * updates (when the IncludeStopsUsedRealTimeInTransfers feature is enabled).
 */
class PatternNearbyStopFilter implements NearbyStopFilter {

  private final TransitService transitService;

  PatternNearbyStopFilter(TransitService transitService) {
    this.transitService = transitService;
  }

  @Override
  public boolean includeFromStop(FeedScopedId id, boolean reverseDirection) {
    var stop = transitService.getRegularStop(id);
    boolean hasPatterns = !findPatternsForStop(stop, !reverseDirection).isEmpty();
    return hasPatterns || includeStopUsedRealtime(stop);
  }

  @Override
  public Collection<NearbyStop> filterToStops(
    Collection<NearbyStop> nearbyStops,
    boolean reverseDirection
  ) {
    // Track the closest stop on each pattern passing nearby.
    MinMap<FeedScopedId, NearbyStop> closestStopForPattern = new MinMap<>();

    // The end result
    Set<NearbyStop> uniqueStopsResult = new HashSet<>();

    for (var it : nearbyStops) {
      StopLocation stop = it.stop;

      if (stop instanceof RegularStop regularStop) {
        var patternsForStop = findPatternsForStop(regularStop, reverseDirection);

        if (patternsForStop.isEmpty() && includeStopUsedRealtime(regularStop)) {
          uniqueStopsResult.add(it);
        }

        for (var pattern : patternsForStop) {
          closestStopForPattern.putMin(pattern, it);
        }
      }
    }
    uniqueStopsResult.addAll(closestStopForPattern.values());

    return uniqueStopsResult;
  }

  /**
   * Find all candidate patterns for the given destination {@code stop}. Only return patterns
   * where we can board(forward direction) or alight(reverse direction) at the given stop.
   */
  private List<FeedScopedId> findPatternsForStop(RegularStop stop, boolean reverseDirection) {
    return transitService
      .findPatterns(stop)
      .stream()
      .filter(reverseDirection ? p -> p.alightingExist(stop) : p -> p.boardingExist(stop))
      .map(TripPattern::getId)
      .toList();
  }

  private boolean includeStopUsedRealtime(RegularStop stop) {
    return OTPFeature.IncludeStopsUsedRealTimeInTransfers.isOn() && stop.isSometimesUsedRealtime();
  }
}
