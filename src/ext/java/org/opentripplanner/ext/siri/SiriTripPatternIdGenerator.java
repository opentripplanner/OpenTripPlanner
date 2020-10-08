package org.opentripplanner.ext.siri;

import org.opentripplanner.gtfs.GenerateTripPatternsOperation;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;

class SiriTripPatternIdGenerator {
  private int counter = 0;

  /**
   * Generate unique trip pattern code for real-time added trip pattern. This function roughly
   * follows the format of {@link GenerateTripPatternsOperation#generateUniqueIdForTripPattern(Route, int)}.
   * <p>
   * The generator add a postfix 'RT' to indicate that this trip pattern is generated at REAL-TIME.
   */
  FeedScopedId generateUniqueTripPatternId(Trip trip) {
    Route route = trip.getRoute();
    FeedScopedId routeId = route.getId();
    String directionId = trip.getDirectionId();
    if( directionId == null) { directionId = ""; }

    // OBA library uses underscore as separator, we're moving toward colon.
    String id = String.format("%s:%s:%03d:RT", routeId.getId(), directionId, ++counter);

    return new FeedScopedId(routeId.getFeedId(), id);
  }
}
