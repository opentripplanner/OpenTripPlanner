package org.opentripplanner.model.plan.paging.cursor;

import java.time.Instant;
import org.opentripplanner.framework.model.NormalizedCost;
import org.opentripplanner.model.plan.ItinerarySortKey;

/**
 * This class contains all the information needed to dedupe itineraries when
 * paging - the exact same information as the {@link ItinerarySortKey}.
 * <p>
 * It implements the ItinerarySortKey interface so that it can be sorted with itineraries which
 * potentially contain duplicates.
 */
record DeduplicationPageCut(
  Instant departureTime,
  Instant arrivalTime,
  NormalizedCost generalizedCost,
  int numOfTransfers,
  boolean onStreet
) implements ItinerarySortKey {
  @Override
  public Instant startTimeAsInstant() {
    return departureTime;
  }

  @Override
  public Instant endTimeAsInstant() {
    return arrivalTime;
  }

  @Override
  public NormalizedCost generalizedCostIncludingPenalty() {
    return generalizedCost;
  }

  @Override
  public int numberOfTransfers() {
    return numOfTransfers;
  }

  @Override
  public boolean isStreetOnly() {
    return onStreet;
  }

  @Override
  public String toString() {
    return keyAsString();
  }
}
