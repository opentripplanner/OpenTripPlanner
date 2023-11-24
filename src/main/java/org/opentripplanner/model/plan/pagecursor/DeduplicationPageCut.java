package org.opentripplanner.model.plan.pagecursor;

import java.time.Instant;
import org.opentripplanner.model.plan.ItinerarySortKey;

/**
 * This class contains all the information needed to dedupe itineraries when
 * paging.
 * <p>
 * It implements the ItinerarySortKey interface so that it can be sorted with itineraries which
 * potentially contain duplicates.
 */
public record DeduplicationPageCut(
  Instant arrivalTimeThreshold,
  Instant departureTimeThreshold,
  int generalizedCostThreshold,
  int numOfTransfersThreshold,
  boolean onStreetAllTheWayThreshold
)
  implements ItinerarySortKey {
  @Override
  public Instant startTimeAsInstant() {
    return departureTimeThreshold;
  }

  @Override
  public Instant endTimeAsInstant() {
    return arrivalTimeThreshold;
  }

  @Override
  public int getGeneralizedCost() {
    return generalizedCostThreshold;
  }

  @Override
  public int getNumberOfTransfers() {
    return numOfTransfersThreshold;
  }

  @Override
  public boolean isOnStreetAllTheWay() {
    return onStreetAllTheWayThreshold;
  }

  @Override
  public String toString() {
    return keyAsString();
  }
}
