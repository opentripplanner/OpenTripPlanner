package org.opentripplanner.model.plan.pagecursor;

import java.time.Instant;

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
  Instant firstRemovedArrivalTime();
  boolean firstRemovedIsOnStreetAllTheWay();
  int firstRemovedGeneralizedCost();
  int firstRemovedNumOfTransfers();
  Instant firstRemovedDepartureTime();
  PagingDeduplicationSection deduplicationSection();
}
