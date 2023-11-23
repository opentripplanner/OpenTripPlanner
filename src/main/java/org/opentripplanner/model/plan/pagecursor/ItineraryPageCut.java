package org.opentripplanner.model.plan.pagecursor;

import java.time.Instant;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.model.plan.ItinerarySortKey;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.DefaultCostCalculator;

/**
 * This class contains all the information needed to dedupe itineraries when
 * paging.
 * <p>
 * It implements the ItinerarySortKey interface so that it can be sorted with itineraries which
 * potentially contain duplicates.
 */
public record ItineraryPageCut(
  PagingDeduplicationSection deduplicationSection,
  Instant arrivalTimeThreshold,
  Instant departureTimeThreshold,
  int generalizedCostThreshold,
  int numOfTransfersThreshold,
  boolean onStreetAllTheWayThreshold
)
  implements ItinerarySortKey {
  @Override
  public Instant startTimeAsInstant() {
    return departureTimeThreshold();
  }

  @Override
  public Instant endTimeAsInstant() {
    return arrivalTimeThreshold();
  }

  @Override
  public int getGeneralizedCost() {
    return generalizedCostThreshold();
  }

  @Override
  public int getNumberOfTransfers() {
    return numOfTransfersThreshold();
  }

  @Override
  public boolean isOnStreetAllTheWay() {
    return isOnStreetAllTheWayThreshold();
  }

  public PagingDeduplicationSection deduplicationSection() {
    return deduplicationSection;
  }

  public boolean isOnStreetAllTheWayThreshold() {
    return onStreetAllTheWayThreshold;
  }

  public Instant arrivalTimeThreshold() {
    return arrivalTimeThreshold;
  }

  public int generalizedCostThreshold() {
    return generalizedCostThreshold;
  }

  public int numOfTransfersThreshold() {
    return numOfTransfersThreshold;
  }

  public Instant departureTimeThreshold() {
    return departureTimeThreshold;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(ItineraryPageCut.class)
      .addEnum("deduplicationSection", deduplicationSection)
      .addBool("isOnStreetAllTheWayThreshold", onStreetAllTheWayThreshold)
      .addDateTime("arrivalTimeThreshold", arrivalTimeThreshold)
      .addCost(
        "generalizedCostThreshold",
        generalizedCostThreshold,
        DefaultCostCalculator.ZERO_COST
      )
      .addNum("numOfTransfersThreshold", numOfTransfersThreshold)
      .addDateTime("departureTimeThreshold", departureTimeThreshold)
      .toString();
  }
}
