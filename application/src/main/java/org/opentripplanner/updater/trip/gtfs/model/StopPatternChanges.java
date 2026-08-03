package org.opentripplanner.updater.trip.gtfs.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * The stop-level changes a GTFS-RT trip update makes relative to a trip's planned stop pattern:
 * pickup and drop-off overrides plus stop replacements (a skipped stop is modelled as a
 * {@code CANCELLED} pickup and drop-off). When the update touches any of these, the trip moves onto
 * a real-time stop pattern that differs from the planned one and is reported as {@code MODIFIED}.
 */
public record StopPatternChanges(
  Map<Integer, PickDrop> updatedPickups,
  Map<Integer, PickDrop> updatedDropoffs,
  Map<Integer, String> replacedStopIds
) {
  public StopPatternChanges {
    updatedPickups = Map.copyOf(updatedPickups);
    updatedDropoffs = Map.copyOf(updatedDropoffs);
    replacedStopIds = Map.copyOf(replacedStopIds);
  }

  /**
   * The real-time stop pattern these changes produce on top of {@code plannedPattern}, or empty
   * when they leave the planned pattern unchanged. A stop replacement whose id cannot be resolved
   * by {@code stopResolver} is dropped, so an update that only replaces unknown stops is not a
   * pattern change. The presence of a result is therefore the single source of truth for whether
   * the trip runs on a modified pattern.
   */
  public Optional<StopPattern> deriveStopPattern(
    TripPattern plannedPattern,
    Function<String, StopLocation> stopResolver
  ) {
    var newStops = resolveReplacedStops(stopResolver);
    if (updatedPickups.isEmpty() && updatedDropoffs.isEmpty() && newStops.isEmpty()) {
      return Optional.empty();
    }
    var candidate = plannedPattern
      .copyPlannedStopPattern()
      .updatePickups(updatedPickups)
      .updateDropoffs(updatedDropoffs)
      .replaceStops(newStops)
      .build();
    // A non-empty set of overrides can still reproduce the planned stops (e.g. an override that
    // repeats the scheduled pickup/drop-off). Only a candidate that actually differs is a pattern
    // modification.
    return candidate.equals(plannedPattern.getStopPattern())
      ? Optional.empty()
      : Optional.of(candidate);
  }

  private Map<Integer, StopLocation> resolveReplacedStops(
    Function<String, StopLocation> stopResolver
  ) {
    Map<Integer, StopLocation> newStops = new HashMap<>();
    replacedStopIds.forEach((stopPosition, stopId) -> {
      var stop = stopResolver.apply(stopId);
      if (stop != null) {
        newStops.put(stopPosition, stop);
      }
    });
    return newStops;
  }
}
