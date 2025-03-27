package org.opentripplanner.model.plan;

import java.time.Instant;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.utils.tostring.ValueObjectToStringBuilder;

/**
 * This interface is used to sort itineraries and other instances that we might want to sort among
 * itineraries. It is used in the
 * {@link org.opentripplanner.routing.algorithm.filterchain.framework.filter.SortingFilter}
 * as defined by the
 * {@link org.opentripplanner.routing.algorithm.filterchain.framework.sort.SortOrderComparator}.
 * <p>
 * The methods in this interface are NOT documented here, but in the Itinerary class. To keep it simple, this
 * interface should be kept in sync with method names in the itinerary.
 */
public interface ItinerarySortKey {
  Instant startTimeAsInstant();
  Instant endTimeAsInstant();
  Cost generalizedCostIncludingPenalty();
  int numberOfTransfers();
  boolean isStreetOnly();

  default String keyAsString() {
    return ValueObjectToStringBuilder.of()
      .addText("[")
      .addTime(startTimeAsInstant())
      .addText(", ")
      .addTime(endTimeAsInstant())
      .addText(", ")
      .addObj(generalizedCostIncludingPenalty())
      .addText(", Tx")
      .addNum(numberOfTransfers())
      .addText(", ")
      .addBool(isStreetOnly(), "onStreet", "transit")
      .addText("]")
      .toString();
  }
}
