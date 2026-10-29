package org.opentripplanner.transfer.dataimport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.place.nearbystopfinder.PatternsByStopLookup;
import org.opentripplanner.transit.dataimport.TimetableDataImport;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * The scheduled trip patterns visiting each stop, and so the stops visited by at least one pattern.
 * <p>
 * The index is found on first use and cached. It must not be used before the transit data is
 * imported, as the cache is not refreshed - the transfer generators are created when the graph
 * builder is assembled, but only use this service when they run.
 */
public class StopsVisitedByPatterns implements PatternsByStopLookup {

  private final TimetableDataImport timetableDataImport;
  private volatile Map<FeedScopedId, List<TripPattern>> patternsByStop;

  public StopsVisitedByPatterns(TimetableDataImport timetableDataImport) {
    this.timetableDataImport = timetableDataImport;
  }

  public boolean contains(FeedScopedId stopId) {
    return index().containsKey(stopId);
  }

  @Override
  public Collection<TripPattern> findPatterns(FeedScopedId stopId) {
    return index().getOrDefault(stopId, List.of());
  }

  /** The ids of the stops of each scheduled trip pattern. */
  public Collection<List<FeedScopedId>> stopsOfEachPattern() {
    var result = new ArrayList<List<FeedScopedId>>();
    for (TripPattern pattern : timetableDataImport.getAllTripPatterns()) {
      result.add(pattern.getStops().stream().map(StopLocation::getId).toList());
    }
    return result;
  }

  /** Thread-safe, the transfers are generated in parallel. */
  private Map<FeedScopedId, List<TripPattern>> index() {
    var result = patternsByStop;
    if (result == null) {
      synchronized (this) {
        result = patternsByStop;
        if (result == null) {
          result = createIndex();
          patternsByStop = result;
        }
      }
    }
    return result;
  }

  private Map<FeedScopedId, List<TripPattern>> createIndex() {
    var index = new HashMap<FeedScopedId, List<TripPattern>>();
    for (TripPattern pattern : timetableDataImport.getAllTripPatterns()) {
      for (StopLocation stop : pattern.getStops()) {
        index.computeIfAbsent(stop.getId(), id -> new ArrayList<>()).add(pattern);
      }
    }
    return index;
  }
}
