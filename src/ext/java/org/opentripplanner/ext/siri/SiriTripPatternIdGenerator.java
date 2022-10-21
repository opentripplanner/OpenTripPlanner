package org.opentripplanner.ext.siri;

import java.util.concurrent.atomic.AtomicInteger;
import org.opentripplanner.gtfs.GenerateTripPatternsOperation;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.Direction;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * This class generate a new id for new TripPatterns created real-time by the SIRI updaters. It is
 * important to creat only on instance of this class, and inject it where it is needed.
 * <p>
 * The id generation is thread-safe, even if that is probably not needed.
 */
class SiriTripPatternIdGenerator {

  private final AtomicInteger counter = new AtomicInteger(0);

  /**
   * Generate unique trip pattern code for real-time added trip pattern. This function roughly
   * follows the format of {@link GenerateTripPatternsOperation}.
   * <p>
   * The generator add a postfix 'RT' to indicate that this trip pattern is generated at REAL-TIME.
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
