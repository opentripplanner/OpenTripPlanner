package org.opentripplanner.updater.trip;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * Thread-safe mechanism for tracking any TripPatterns added to the graph via realtime messages.
 * This tracks only patterns added by realtime messages, not ones that already existed from the
 * scheduled data. This is a "cache" in the sense that it will keep returning the same TripPattern
 * when presented with the same StopPattern, so if realtime messages add many trips passing through
 * the same sequence of stops, they will all end up on this same TripPattern.
 *
 * <h3>Thread-safety</h3>
 *
 * The thread safety is guaranteed by only a single thread being allowed to access this cache through
 * {@link org.opentripplanner.updater.GraphWriterRunnable}.
 * <p>
 * <a href="https://github.com/opentripplanner/OpenTripPlanner/pull/6629#pullrequestreview-2854296598">Further reading</a>
 * <p>
 * TODO RT_TG: There is no clear strategy for what should be in the cache and the transit model and
 *             the flow between them.
 *             With the increased usage of DatedServiceJourneys, this should probably be part of the
 *             main model - not a separate cache. It is possible that this class works when it comes
 *             to the thread-safety, but just by looking at a few lines of code I see problems - a
 *             strategy needs to be analysed, designed and documented.
 */
public class TripPatternCache {

  /**
   * We cache the trip pattern based on the stop pattern only in order to de-duplicate them.
   * <p>
   * Note that we don't really have a definition which properties are really part of the trip
   * pattern and several pattern keys are used in different parts of OTP.
   */
  private final Map<StopPattern, TripPattern> cache = new HashMap<>();

  private final TripPatternIdGenerator tripPatternIdGenerator;

  private final Function<Trip, TripPattern> getPatternForTrip;

  /**
   * @param getPatternForTrip TripPatternCache needs only this one feature of TransitService, so we retain
   *                          only this function reference to effectively narrow the interface. This should also facilitate
   *                          testing.
   */
  public TripPatternCache(
    TripPatternIdGenerator tripPatternIdGenerator,
    Function<Trip, TripPattern> getPatternForTrip
  ) {
    this.tripPatternIdGenerator = tripPatternIdGenerator;
    this.getPatternForTrip = getPatternForTrip;
  }

  /**
   * Get cached trip pattern or create one if it doesn't exist yet.
   * <p>
   * Note: The synchronized key word leads to a false sense of security. Thread safety is achieved
   * by a single thread being allowed to access - see class documentation.
   *
   * @param stopPattern stop pattern to retrieve/create trip pattern
   * @param trip        Trip containing route of new trip pattern in case a new trip pattern will be
   *                    created
   * @return cached or newly created trip pattern
   */
  public synchronized TripPattern getOrCreateTripPattern(
    final StopPattern stopPattern,
    final Trip trip
  ) {
    TripPattern originalTripPattern = getPatternForTrip.apply(trip);

    // if a scheduled or a real-time-added pattern already exists then return that instead
    // of looking at the cache.
    // here GTFS and SIRI behave differently when dealing with ADDED/ExtraJourney
    // as SIRI already creates a pattern and adds it to the timetable snapshot before this class
    // is queried, but GTFS-RT doesn't. i'm not sure why that is.
    if (originalTripPattern != null && originalTripPattern.getStopPattern().equals(stopPattern)) {
      return originalTripPattern;
    }

    // Check cache for trip pattern
    TripPattern tripPattern = cache.get(stopPattern);

    // Create TripPattern if it doesn't exist yet
    if (tripPattern == null) {
      var id = tripPatternIdGenerator.generateUniqueTripPatternId(trip);
      tripPattern = TripPattern.of(id)
        .withRoute(trip.getRoute())
        .withMode(trip.getMode())
        .withNetexSubmode(trip.getNetexSubMode())
        .withStopPattern(stopPattern)
        .withCreatedByRealtimeUpdater(true)
        .withOriginalTripPattern(originalTripPattern)
        .build();

      // Add pattern to cache
      cache.put(stopPattern, tripPattern);
    }

    return tripPattern;
  }
}
