package org.opentripplanner.model.plan.pagecursor;

import java.time.Instant;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.model.plan.ItinerarySortKey;
import org.opentripplanner.model.plan.SortOrder;

/**
 * The PagingDeduplicationParameters contains all the information needed to dedupe itineraries when
 * paging.
 * <p>
 * It implements the ItinerarySortKey interface so that it can be sorted with itineraries which
 * potentially contain duplicates.
 */
public class PagingDeduplicationParameters implements ItinerarySortKey {

  public final Instant windowStart;
  public final Instant windowEnd;
  public final PagingDeduplicationSection deduplicationSection;
  public final SortOrder sortOrder;
  public final boolean isOnStreetAllTheWayThreshold;
  public final Instant arrivalTimeThreshold;
  public final int generalizedCostThreshold;
  public final int numOfTransfersThreshold;
  public final Instant departureTimeThreshold;

  public PagingDeduplicationParameters(
    Instant windowStart,
    Instant windowEnd,
    SortOrder sortOrder,
    PagingDeduplicationSection deduplicationSection,
    boolean isOnStreetAllTheWayThreshold,
    Instant arrivalTimeThreshold,
    int generalizedCostThreshold,
    int numOfTransfersThreshold,
    Instant departureTimeThreshold
  ) {
    this.windowStart = windowStart;
    this.windowEnd = windowEnd;
    this.isOnStreetAllTheWayThreshold = isOnStreetAllTheWayThreshold;
    this.arrivalTimeThreshold = arrivalTimeThreshold;
    this.generalizedCostThreshold = generalizedCostThreshold;
    this.numOfTransfersThreshold = numOfTransfersThreshold;
    this.departureTimeThreshold = departureTimeThreshold;
    this.sortOrder = sortOrder;
    this.deduplicationSection = deduplicationSection;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(PagingDeduplicationParameters.class)
      .addDateTime("dedupeWindowStart", windowStart)
      .addDateTime("dedupeWindowEnd", windowEnd)
      .addEnum("sortOrder", sortOrder)
      .addEnum("deduplicationSection", deduplicationSection)
      .addBool("isOnStreetAllTheWayThreshold", isOnStreetAllTheWayThreshold)
      .addDateTime("arrivalTimeThreshold", arrivalTimeThreshold)
      .addNum("generalizedCostThreshold", generalizedCostThreshold)
      .addNum("numOfTransfersThreshold", numOfTransfersThreshold)
      .addDateTime("departureTimeThreshold", departureTimeThreshold)
      .toString();
  }

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
    return isOnStreetAllTheWayThreshold;
  }
}
