package org.opentripplanner.updater.trip.siri;

import java.util.concurrent.atomic.AtomicInteger;
import org.opentripplanner.gtfs.GenerateTripPatternsOperation;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.Direction;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * This class generates new unique IDs for TripPatterns created in response to real-time updates
 * from the SIRI updaters. In non-test usage it is important to create only one instance of this
 * class, and inject that single instance wherever it is needed. However, this single-instance
 * usage pattern is not enforced due to differing needs in tests.
 * The ID generation is threadsafe, even if that is probably not needed.
 */
class SiriTripPatternIdGenerator {

  private final AtomicInteger counter = new AtomicInteger(0);

  /**
   * Generate a unique ID for a trip pattern added in response to a realtime message. This function
   * roughly follows the format of {@link GenerateTripPatternsOperation}. The generator suffixes the
   * ID with 'RT' to indicate that this trip pattern is generated in response to a realtime message.
   */
  FeedScopedId generateUniqueTripPatternId(Trip trip) {
    Route route = trip.getRoute();
    FeedScopedId routeId = route.getId();
    Direction direction = trip.getDirection();
    String directionId = direction == Direction.UNKNOWN ? "" : Integer.toString(direction.gtfsCode);

    String id = String.format(
      "%s:%s:%03d:RT",
      routeId.getId(),
      directionId,
      counter.incrementAndGet()
    );

    return new FeedScopedId(routeId.getFeedId(), id);
  }
}
