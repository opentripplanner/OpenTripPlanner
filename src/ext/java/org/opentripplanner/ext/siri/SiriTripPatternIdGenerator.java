package org.opentripplanner.ext.siri;

import org.opentripplanner.gtfs.GenerateTripPatternsOperation;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class generate a new id for new TripPatterns created real-time by the
 * SIRI updaters. It is important to creat only on instance of this class, and inject it
 * where it is needed.
 * <p>
 * The id generation is thread-safe, even if that is probably not needed.
 */
class SiriTripPatternIdGenerator {
  private final AtomicInteger counter = new AtomicInteger(0);

  /**
   * Generate unique trip pattern code for real-time added trip pattern. This function roughly
   * follows the format of {@link GenerateTripPatternsOperation#generateUniqueIdForTripPattern(Route, int)}.
   * <p>
   * The generator add a postfix 'RT' to indicate that this trip pattern is generated at REAL-TIME.
   */
  FeedScopedId generateUniqueTripPatternId(Trip trip) {
    Route route = trip.getRoute();
    FeedScopedId routeId = route.getId();
    String directionId = trip.getGtfsDirectionIdAsString("");

    // OBA library uses underscore as separator, we're moving toward colon.
    String id = String.format("%s:%s:%03d:RT", routeId.getId(), directionId, counter.incrementAndGet());

    return new FeedScopedId(routeId.getFeedId(), id);
  }
}
