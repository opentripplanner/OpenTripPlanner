package org.opentripplanner.model.plan.paging.cursor;

import java.time.Instant;
import org.opentripplanner.model.plan.ItinerarySortKey;

/**
 * This class holds information needed to create the next/previous page cursors when there were
 * itineraries removed due to cropping the list of itineraries using the numItineraries parameter.
 * <p>
 * The Instant fields come from the sets of itineraries that were removed and the ones that were
 * kept as a result of using the numItineraries parameter.
 */
public interface PageCursorInput {
  Instant earliestRemovedDeparture();
  Instant earliestKeptArrival();
  Instant latestRemovedDeparture();
  Instant latestRemovedArrival();
  ItinerarySortKey firstRemoved();
}
