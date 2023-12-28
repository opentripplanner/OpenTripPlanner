package org.opentripplanner.model.plan;

import java.time.Instant;
import org.opentripplanner.framework.tostring.ValueObjectToStringBuilder;
import org.opentripplanner.routing.algorithm.filterchain.comparator.SortOrderComparator;
import org.opentripplanner.routing.algorithm.filterchain.filter.SortingFilter;

/**
 * This interface is used to sort itineraries and other instances that we might want to sort among
 * itineraries. It is used in the {@link SortingFilter} as defined by the
 * {@link SortOrderComparator}.
 * <p>
 * The methods in this interface are NOT documented here, but in the Itinerary class. To keep it simple, this
 * interface should be kept in sync with method names in the itinerary.
 */
public interface ItinerarySortKey {
  Instant startTimeAsInstant();
  Instant endTimeAsInstant();
  int getGeneralizedCost();
  int getNumberOfTransfers();
  boolean isOnStreetAllTheWay();

  default String keyAsString() {
    return ValueObjectToStringBuilder
      .of()
      .addText("[")
      .addTime(startTimeAsInstant())
      .addText(", ")
      .addTime(endTimeAsInstant())
      .addText(", ")
      .addCost(getGeneralizedCost())
      .addText(", Tx")
      .addNum(getNumberOfTransfers())
      .addText(", ")
      .addBool(isOnStreetAllTheWay(), "onStreet", "transit")
      .addText("]")
      .toString();
  }
}
