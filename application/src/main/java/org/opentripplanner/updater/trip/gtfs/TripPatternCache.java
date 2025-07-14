package org.opentripplanner.updater.trip.gtfs;

import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.gtfs.GenerateTripPatternsOperation;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Direction;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * Threadsafe mechanism for tracking any TripPatterns added to the graph via GTFS realtime messages.
 * This tracks only patterns added by realtime messages, not ones that already existed from the
 * scheduled GTFS. This is a "cache" in the sense that it will keep returning the same TripPattern
 * when presented with the same StopPattern, so if realtime messages add many trips passing through
 * the same sequence of stops, they will all end up on this same TripPattern.
 * <p>
 * Note that there are two versions of this class, this one for GTFS-RT and another for SIRI.
 * TODO RT_AB: consolidate TripPatternCache and SiriTripPatternCache. They seem to only be separate
 *    because SIRI- or GTFS-specific indexes of the added TripPatterns seem to have been added to
 *    this primary collection.
 * FIXME RT_AB: the name does not make it clear that this has anything to do with elements that are
 *    only added due to realtime updates, and it is only loosely a cache. RealtimeAddedTripPatterns?
 */
class TripPatternCache {

  /**
   * This tracks only patterns added by realtime messages, it is not primed with TripPatterns that
   * already existed from the scheduled GTFS.
   */
  private final Map<StopPattern, TripPattern> cache = new HashMap<>();

  /** Used for producing sequential integers to ensure each added pattern has a unique name. */
  private int counter = 0;

  /**
   * Get cached trip pattern or create one if it doesn't exist yet.
   *
   * @param stopPattern         stop pattern to retrieve/create trip pattern
   * @param trip                the trip the new trip pattern will be created for
   * @param originalTripPattern the trip pattern the new pattern is based. If the pattern is
   *                            completely new, this will be null
   * @return cached or newly created trip pattern
   */
  public synchronized TripPattern getOrCreateTripPattern(
    final StopPattern stopPattern,
    final Trip trip,
    final TripPattern originalTripPattern
  ) {
    Route route = trip.getRoute();
    // Check cache for trip pattern
    TripPattern tripPattern = cache.get(stopPattern);

    // Create TripPattern if it doesn't exist yet
    if (tripPattern == null) {
      // Generate unique code for trip pattern
      var id = generateUniqueTripPatternCode(trip);

      tripPattern = TripPattern.of(id)
        .withRoute(route)
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

  /**
   * Generate unique trip pattern code for real-time added trip pattern. This function roughly
   * follows the format of the {@link GenerateTripPatternsOperation}.
   * In the SIRI version of this class, this is provided by a SiriTripPatternIdGenerator. If the
   * GTFS-RT and SIRI version of these classes are merged, this function could become a second
   * implementation of TripPatternIdGenerator.
   * This method is not static because it references a monotonically increasing integer counter.
   * But like in SiriTripPatternIdGenerator, this could be encapsulated outside the cache object.
   * TODO RT_AB: create GtfsRtTripPatternIdGenerator as part of merging the two TripPatternCaches.
   */
  private FeedScopedId generateUniqueTripPatternCode(Trip trip) {
    FeedScopedId routeId = trip.getRoute().getId();
    Direction direction = trip.getDirection();
    String directionId = direction == Direction.UNKNOWN ? "" : Integer.toString(direction.gtfsCode);
    if (counter == Integer.MAX_VALUE) {
      counter = 0;
    } else {
      counter++;
    }
    String code = String.format("%s:%s:rt#%d", routeId.getId(), directionId, counter);
    return new FeedScopedId(routeId.getFeedId(), code);
  }
}
