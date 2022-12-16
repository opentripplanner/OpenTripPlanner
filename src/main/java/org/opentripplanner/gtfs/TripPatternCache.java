package org.opentripplanner.gtfs;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Direction;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * A synchronized cache of trip patterns that are added to the graph.
 */
public class TripPatternCache {

  /**
   * Each instance track patterns separately, i.e. gtfs module or gtfs-realtime
   * as a result a suffix for the pattern ids is used to avoid conflict
   */
  private final String patternIdSuffix;
  private final Multimap<StopPattern, TripPattern> cache;
  private final Map<String, Integer> tripPatternIdCounters = new HashMap<>();

  /**
   * Create a new cache object using provided suffix for id generation
   * @param patternIdSuffix
   */
  public TripPatternCache(String patternIdSuffix) {
    this.patternIdSuffix = patternIdSuffix;
    this.cache = ArrayListMultimap.create();
  }

  /**
   * Create a new cache instance, using provided suffix for id generatio and
   * using an existing Multimap StopPattern -> TripPattern. Note: TripPattern
   * added to the cache will be inserted in the provided Multimap
   * @param patternIdSuffix
   * @param cache Existing cache to re-use
   */
  public TripPatternCache(String patternIdSuffix, Multimap<StopPattern, TripPattern> cache) {
    this.patternIdSuffix = patternIdSuffix;
    this.cache = cache;
  }

  /**
   * Get cached trip pattern. Match by StopPattern, Route and Direction
   *
   * @param stopPattern         stop pattern to find similar trip pattern
   * @param trip                the relevant trip for the trip pattern
   *
   * @return cached or newly created trip pattern
   */
  public TripPattern getTripPattern(
    @Nonnull final StopPattern stopPattern,
    @Nonnull final Trip trip
  ) {
    Route route = trip.getRoute();
    for (TripPattern tripPattern : cache.get(stopPattern)) {
      if (
        tripPattern.getRoute().equals(route) &&
        tripPattern.getDirection().equals(trip.getDirection()) &&
        tripPattern.getMode().equals(trip.getMode())
      ) {
        return tripPattern;
      }
    }
    return null;
  }

  /**
   * Patterns do not have unique IDs in GTFS, so we make some by concatenating the route id,
   * the direction a suffix and a counter. This only works if the Cache of TripPattern is used for every
   * TripPattern for the agency (with a given suffix).
   *
   * @param trip                the trip the new trip pattern will be created for
   * @return The unique trip pattern id for this trip
   */
  public FeedScopedId generateUniqueIdForTripPattern(Trip trip) {
    FeedScopedId routeId = trip.getRoute().getId();
    String directionId = trip.getDirection() == Direction.UNKNOWN
      ? ""
      : Integer.toString(trip.getDirection().gtfsCode);
    String key = routeId.getId() + ":" + trip.getDirection();

    // Add 1 to counter and update it
    int counter = tripPatternIdCounters.getOrDefault(key, 0) + 1;
    tripPatternIdCounters.put(key, counter);

    String id = String.format(
      "%s:%s:%s%02d",
      routeId.getId(),
      directionId,
      patternIdSuffix,
      counter
    );

    return new FeedScopedId(routeId.getFeedId(), id);
  }

  /**
   * Add a tripPattern to the cache
   * @param stopPattern
   * @param tripPattern
   */
  public void add(StopPattern stopPattern, TripPattern tripPattern) {
    this.cache.put(stopPattern, tripPattern);
  }
}
