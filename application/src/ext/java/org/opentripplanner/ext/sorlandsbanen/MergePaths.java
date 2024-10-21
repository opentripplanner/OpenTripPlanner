package org.opentripplanner.ext.sorlandsbanen;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.PathLeg;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TripScheduleWithOffset;
import org.opentripplanner.transit.model.basic.TransitMode;

/**
 * Strategy for merging the main results and the extra rail results from Sorlandsbanen.
 * Everything from the main result is kept, and any additional rail results from the alternative
 * search are added.
 */
class MergePaths<T extends RaptorTripSchedule> implements BiFunction<Collection<RaptorPath<T>>, Collection<RaptorPath<T>>, Collection<RaptorPath<T>>> {

  @Override
  public Collection<RaptorPath<T>> apply(Collection<RaptorPath<T>> main, Collection<RaptorPath<T>> railAlternatives) {
    Map<PathKey, RaptorPath<T>> result = new HashMap<>();
    addAllToMap(result, main);
    addRailToMap(result, railAlternatives);
    return result.values();
  }

  private void addAllToMap(Map<PathKey, RaptorPath<T>> map, Collection<RaptorPath<T>> paths) {
    for (var it : paths) {
        map.put(new PathKey(it), it);
    }
  }

  private void addRailToMap(Map<PathKey, RaptorPath<T>> map, Collection<RaptorPath<T>> paths) {
    for (var it : paths) {
      if (hasRail(it)) {
        map.put(new PathKey(it), it);
      }
    }
  }

  private static boolean hasRail(RaptorPath<?> path) {
    return path
      .legStream()
      .filter(PathLeg::isTransitLeg)
      .anyMatch(leg -> {
        var trip = (TripScheduleWithOffset) leg.asTransitLeg().trip();
        var mode = trip.getOriginalTripPattern().getMode();
        return mode == TransitMode.RAIL;
      });
  }
}
