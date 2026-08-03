package org.opentripplanner.updater.trip.siri;

import javax.annotation.Nullable;
import org.opentripplanner.core.framework.deduplicator.DeduplicatorService;
import org.opentripplanner.transit.repository.TimetableRepository;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.updater.trip.patterncache.TripPatternCache;
import org.opentripplanner.updater.trip.patterncache.TripPatternIdGenerator;

/**
 * Application-scoped factory for SIRI-ET estimated timetable processing. Holds stable,
 * application-lifetime state and produces a per-task {@link SiriRealTimeUpdateHandler} via
 * {@link #forUpdate(TimetableRepository)}.
 */
public class SiriRealTimeTripUpdateAdapter {

  /**
   * Use an id generator to generate TripPattern ids for new TripPatterns created by RealTime
   * updates.
   */
  private final TripPatternIdGenerator tripPatternIdGenerator = new TripPatternIdGenerator();
  /**
   * A synchronized cache of trip patterns that are added to the graph due to GTFS-real-time
   * messages.
   */
  private final TripPatternCache tripPatternCache;

  private final DeduplicatorService deduplicator;
  private final TransitRepository transitRepository;

  @Nullable
  private final SiriFuzzyTripMatcherCache siriFuzzyTripMatcherCache;

  public SiriRealTimeTripUpdateAdapter(
    TransitRepository transitRepository,
    DeduplicatorService deduplicator,
    @Nullable SiriFuzzyTripMatcherCache siriFuzzyTripMatcherCache
  ) {
    this.deduplicator = deduplicator;
    this.transitRepository = transitRepository;
    this.siriFuzzyTripMatcherCache = siriFuzzyTripMatcherCache;
    this.tripPatternCache = new TripPatternCache(tripPatternIdGenerator);
  }

  /**
   * Create an update-scoped task for applying SIRI-ET estimated timetables. The task holds a
   * {@link org.opentripplanner.transit.service.TransitEditorService} constructed from the given
   * buffer, so all pattern and trip lookups within the task see in-progress real-time additions.
   */
  public SiriRealTimeUpdateHandler forUpdate(TimetableRepository buffer) {
    var transitService = new DefaultTransitService(transitRepository, buffer);
    var fuzzyTripMatcher = siriFuzzyTripMatcherCache != null
      ? new SiriFuzzyTripMatcher(siriFuzzyTripMatcherCache, transitService)
      : null;
    return new SiriRealTimeUpdateHandler(
      transitService,
      buffer,
      fuzzyTripMatcher,
      tripPatternCache,
      deduplicator,
      tripPatternIdGenerator
    );
  }
}
