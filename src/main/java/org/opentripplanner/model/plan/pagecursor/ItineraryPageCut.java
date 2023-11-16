package org.opentripplanner.model.plan.pagecursor;

import java.time.Instant;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.model.plan.ItinerarySortKey;
import org.opentripplanner.model.plan.SortOrder;

/**
 * This class contains all the information needed to dedupe itineraries when
 * paging.
 * <p>
 * It implements the ItinerarySortKey interface so that it can be sorted with itineraries which
 * potentially contain duplicates.
 */
public class ItineraryPageCut implements ItinerarySortKey {

  private final Instant windowStart;
  private final Instant windowEnd;
  private final PagingDeduplicationSection deduplicationSection;
  private final SortOrder sortOrder;
  private final boolean isOnStreetAllTheWayThreshold;
  private final Instant arrivalTimeThreshold;
  private final int generalizedCostThreshold;
  private final int numOfTransfersThreshold;
  private final Instant departureTimeThreshold;

  public ItineraryPageCut(
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
      .of(ItineraryPageCut.class)
      .addDateTime("dedupeWindowStart", getWindowStart())
      .addDateTime("dedupeWindowEnd", getWindowEnd())
      .addEnum("sortOrder", getSortOrder())
      .addEnum("deduplicationSection", getDeduplicationSection())
      .addBool("isOnStreetAllTheWayThreshold", isOnStreetAllTheWayThreshold())
      .addDateTime("arrivalTimeThreshold", getArrivalTimeThreshold())
      .addNum("generalizedCostThreshold", getGeneralizedCostThreshold())
      .addNum("numOfTransfersThreshold", getNumOfTransfersThreshold())
      .addDateTime("departureTimeThreshold", getDepartureTimeThreshold())
      .toString();
  }

  @Override
  public Instant startTimeAsInstant() {
    return getDepartureTimeThreshold();
  }

  @Override
  public Instant endTimeAsInstant() {
    return getArrivalTimeThreshold();
  }

  @Override
  public int getGeneralizedCost() {
    return getGeneralizedCostThreshold();
  }

  @Override
  public int getNumberOfTransfers() {
    return getNumOfTransfersThreshold();
  }

  @Override
  public boolean isOnStreetAllTheWay() {
    return isOnStreetAllTheWayThreshold();
  }

  public Instant getWindowStart() {
    return windowStart;
  }

  public Instant getWindowEnd() {
    return windowEnd;
  }

  public PagingDeduplicationSection getDeduplicationSection() {
    return deduplicationSection;
  }

  public SortOrder getSortOrder() {
    return sortOrder;
  }

  public boolean isOnStreetAllTheWayThreshold() {
    return isOnStreetAllTheWayThreshold;
  }

  public Instant getArrivalTimeThreshold() {
    return arrivalTimeThreshold;
  }

  public int getGeneralizedCostThreshold() {
    return generalizedCostThreshold;
  }

  public int getNumOfTransfersThreshold() {
    return numOfTransfersThreshold;
  }

  public Instant getDepartureTimeThreshold() {
    return departureTimeThreshold;
  }
}
