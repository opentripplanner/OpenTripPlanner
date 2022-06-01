package org.opentripplanner.updater.stoptime;

import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;
import org.opentripplanner.gtfs.GenerateTripPatternsOperation;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * A synchronized cache of trip patterns that are added to the graph due to GTFS-realtime messages.
 * This tracks only patterns added by realtime messages, not ones that already existed from the
 * scheduled GTFS.
 */
public class TripPatternCache {

  /**
   * This tracks only patterns added by realtime messages, it is not primed with TripPatterns that
   * already existed from the scheduled GTFS.
   */
  private final Map<StopPattern, TripPattern> cache = new HashMap<>();
  private int counter = 0;

  /**
   * Get cached trip pattern or create one if it doesn't exist yet. If a trip pattern is created,
   * vertices and edges for this trip pattern are also created in the graph.
   *
   * @param stopPattern         stop pattern to retrieve/create trip pattern
   * @param trip                the trip the new trip pattern will be created for
   * @param serviceCodes        graph's service codes
   * @param originalTripPattern the trip pattern the new pattern is based. If the pattern is
   *                            completely new, this will be null
   * @return cached or newly created trip pattern
   */
  public synchronized TripPattern getOrCreateTripPattern(
    @NotNull final StopPattern stopPattern,
    @NotNull final Trip trip,
    @NotNull final Map<FeedScopedId, Integer> serviceCodes,
    final TripPattern originalTripPattern
  ) {
    Route route = trip.getRoute();
    // Check cache for trip pattern
    TripPattern tripPattern = cache.get(stopPattern);

    // Create TripPattern if it doesn't exist yet
    if (tripPattern == null) {
      // Generate unique code for trip pattern
      var id = generateUniqueTripPatternCode(trip);

      tripPattern = new TripPattern(id, route, stopPattern);

      // Create an empty bitset for service codes (because the new pattern does not contain any trips)
      tripPattern.setServiceCodes(serviceCodes);

      // Finish scheduled time table
      tripPattern.getScheduledTimetable().finish();

      tripPattern.setCreatedByRealtimeUpdater();

      // Copy information from the TripPattern this is replacing
      if (originalTripPattern != null) {
        tripPattern.setOriginalTripPattern(originalTripPattern);
        tripPattern.setHopGeometriesFromPattern(originalTripPattern);
      }

      // Add pattern to cache
      cache.put(stopPattern, tripPattern);
    }

    return tripPattern;
  }

  /**
   * Generate unique trip pattern code for real-time added trip pattern. This function roughly
   * follows the format of the {@link GenerateTripPatternsOperation}.
   */
  private FeedScopedId generateUniqueTripPatternCode(Trip trip) {
    FeedScopedId routeId = trip.getRoute().getId();
    String directionId = trip.getGtfsDirectionIdAsString("");
    if (counter == Integer.MAX_VALUE) {
      counter = 0;
    } else {
      counter++;
    }
    // OBA library uses underscore as separator, we're moving toward colon.
    String code = String.format(
      "%s:%s:%s:rt#%d",
      routeId.getFeedId(),
      routeId.getId(),
      directionId,
      counter
    );
    return new FeedScopedId(trip.getId().getFeedId(), code);
  }
}
