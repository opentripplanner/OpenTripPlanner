package org.opentripplanner.model.plan;

import java.time.ZonedDateTime;
import org.opentripplanner.routing.algorithm.filterchain.comparator.SortOrderComparator;
import org.opentripplanner.routing.algorithm.filterchain.filter.SortingFilter;

/**
 * This interface is used to sort itineraries and other instances that we might want to sort among
 * itineraries. It is used in the {@link SortingFilter} as defined by the
 * {@link SortOrderComparator}.
 */
public interface ItinerarySortKey {
  ZonedDateTime startTime();
  ZonedDateTime endTime();
  int getGeneralizedCost();
  int getNumberOfTransfers();
  boolean isOnStreetAllTheWay();
}
