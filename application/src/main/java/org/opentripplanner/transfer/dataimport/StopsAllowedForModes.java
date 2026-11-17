package org.opentripplanner.transfer.dataimport;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.dataimport.TimetableDataImport;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.transfer.regular.api.maxduration.AllowedMode;
import org.opentripplanner.transit.transfer.regular.spi.AllowedModesLookup;

/**
 * The modes the stops are allowed for, used to select the max duration of the regular transfers.
 * <ul>
 *   <li>{@link AllowedMode#CAR}: the stop is visited by a trip that allows cars, for example a car
 *       ferry.</li>
 *   <li>{@link AllowedMode#BICYCLE}: the stop is visited by a trip that allows bicycles. A stop
 *       that is not visited by any pattern is also allowed for bicycles. This is needed when trains
 *       that carry bicycles can change platforms.</li>
 * </ul>
 * <p>
 * The sets are found on first use and cached. The lookup must not be used before the transit data
 * is imported, as the cache is not refreshed - the transfer generators are created when the graph
 * builder is assembled, but only use the lookup when they run.
 */
public class StopsAllowedForModes implements AllowedModesLookup {

  private static final Set<AllowedMode> NONE = Set.of();
  private static final Set<AllowedMode> CAR = Set.of(AllowedMode.CAR);
  private static final Set<AllowedMode> BICYCLE = Set.of(AllowedMode.BICYCLE);
  private static final Set<AllowedMode> BICYCLE_AND_CAR = Set.copyOf(
    EnumSet.of(AllowedMode.BICYCLE, AllowedMode.CAR)
  );

  private final TimetableDataImport timetableDataImport;
  private final StopsVisitedByPatterns stopsVisitedByPatterns;
  private volatile Index index;

  public StopsAllowedForModes(
    TimetableDataImport timetableDataImport,
    StopsVisitedByPatterns stopsVisitedByPatterns
  ) {
    this.timetableDataImport = timetableDataImport;
    this.stopsVisitedByPatterns = stopsVisitedByPatterns;
  }

  @Override
  public Set<AllowedMode> modesAt(FeedScopedId stopId) {
    var idx = index();
    boolean car = idx.carStops().contains(stopId);
    boolean bicycle =
      idx.bicycleStops().contains(stopId) || !stopsVisitedByPatterns.contains(stopId);
    if (car) {
      return bicycle ? BICYCLE_AND_CAR : CAR;
    }
    return bicycle ? BICYCLE : NONE;
  }

  /** Thread-safe, the transfers are generated in parallel. */
  private Index index() {
    var result = index;
    if (result == null) {
      synchronized (this) {
        result = index;
        if (result == null) {
          result = new Index(
            ids(timetableDataImport.getStopLocationsUsedForCarsAllowedTrips()),
            ids(timetableDataImport.getStopLocationsUsedForBikesAllowedTrips())
          );
          index = result;
        }
      }
    }
    return result;
  }

  private static Set<FeedScopedId> ids(Set<StopLocation> stops) {
    var ids = new HashSet<FeedScopedId>();
    for (var stop : stops) {
      ids.add(stop.getId());
    }
    return ids;
  }

  private record Index(Set<FeedScopedId> carStops, Set<FeedScopedId> bicycleStops) {}
}
