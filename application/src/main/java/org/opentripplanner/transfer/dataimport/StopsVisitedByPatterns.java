package org.opentripplanner.transfer.dataimport;

import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.dataimport.TimetableDataImport;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * The stops visited by at least one scheduled trip pattern.
 * <p>
 * The set is found on first use and cached. It must not be used before the transit data is
 * imported, as the cache is not refreshed - the transfer generators are created when the graph
 * builder is assembled, but only use this service when they run.
 */
public class StopsVisitedByPatterns {

  private final TimetableDataImport timetableDataImport;
  private volatile Set<FeedScopedId> stops;

  public StopsVisitedByPatterns(TimetableDataImport timetableDataImport) {
    this.timetableDataImport = timetableDataImport;
  }

  public boolean contains(FeedScopedId stopId) {
    return stops().contains(stopId);
  }

  /** Thread-safe, the transfers are generated in parallel. */
  private Set<FeedScopedId> stops() {
    var result = stops;
    if (result == null) {
      synchronized (this) {
        result = stops;
        if (result == null) {
          result = findStops();
          stops = result;
        }
      }
    }
    return result;
  }

  private Set<FeedScopedId> findStops() {
    return timetableDataImport
      .getAllTripPatterns()
      .stream()
      .flatMap(pattern -> pattern.getStops().stream())
      .map(StopLocation::getId)
      .collect(Collectors.toSet());
  }
}
