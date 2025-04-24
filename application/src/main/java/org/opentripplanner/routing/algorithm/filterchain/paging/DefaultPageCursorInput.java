package org.opentripplanner.routing.algorithm.filterchain.paging;

import java.time.Instant;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.plan.ItinerarySortKey;
import org.opentripplanner.model.plan.paging.cursor.PageCursorInput;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * This class stores input for the PageCursor. The input is related to the NumItinerariesFilter and RemoveTransitIfStreetOnlyIsBetter.
 * <p>
 * The NumItinerariesFilter removes itineraries from a list of itineraries based on the number to
 * keep and whether it should crop at the head or the tail of the list. This class keeps
 * the extreme endpoints of the sets of itineraries that were kept and removed, as well as more
 * details about the first itinerary removed (bottom of the head, or top of the tail) and whether
 * itineraries were cropped at the head or the tail.
 * <p>
 * The {@link org.opentripplanner.routing.algorithm.filterchain.filters.transit.RemoveTransitIfStreetOnlyIsBetter}
 * filter removes transit itineraries if the best street only itinerary has a lower cost.
 * This class stores the cost of the best street only itinerary for use with paging.
 */
public class DefaultPageCursorInput implements PageCursorInput {

  private final Instant earliestRemovedDeparture;
  private final Instant latestRemovedDeparture;
  private final ItinerarySortKey pageCut;
  private final Cost generalizedCostMaxLimit;

  private DefaultPageCursorInput() {
    this.earliestRemovedDeparture = null;
    this.latestRemovedDeparture = null;
    this.pageCut = null;
    this.generalizedCostMaxLimit = null;
  }

  private DefaultPageCursorInput(Builder builder) {
    this.earliestRemovedDeparture = builder.earliestRemovedDeparture();
    this.latestRemovedDeparture = builder.latestRemovedDeparture();
    this.pageCut = builder.pageCut();
    this.generalizedCostMaxLimit = builder.generalizedCostMaxLimit();
  }

  public static DefaultPageCursorInput.Builder of() {
    return new Builder(new DefaultPageCursorInput());
  }

  public DefaultPageCursorInput.Builder copyOf() {
    return new Builder(this);
  }

  @Override
  public Instant earliestRemovedDeparture() {
    return earliestRemovedDeparture;
  }

  @Override
  public Instant latestRemovedDeparture() {
    return latestRemovedDeparture;
  }

  @Override
  public ItinerarySortKey pageCut() {
    return pageCut;
  }

  @Override
  public Cost generalizedCostMaxLimit() {
    return generalizedCostMaxLimit;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(DefaultPageCursorInput.class)
      .addDateTime("earliestRemovedDeparture", earliestRemovedDeparture)
      .addDateTime("latestRemovedDeparture", latestRemovedDeparture)
      .addObjOp("pageCut", pageCut, ItinerarySortKey::keyAsString)
      .addObj("generalizedCostMaxLimit", generalizedCostMaxLimit)
      .toString();
  }

  public static class Builder {

    private Instant earliestRemovedDeparture;
    private Instant latestRemovedDeparture;
    private ItinerarySortKey pageCut;
    private Cost generalizedCostMaxLimit;

    public Builder(DefaultPageCursorInput original) {
      this.earliestRemovedDeparture = original.earliestRemovedDeparture;
      this.latestRemovedDeparture = original.latestRemovedDeparture;
      this.pageCut = original.pageCut;
      this.generalizedCostMaxLimit = original.generalizedCostMaxLimit;
    }

    public Instant earliestRemovedDeparture() {
      return earliestRemovedDeparture;
    }

    public Builder withEarliestRemovedDeparture(Instant earliestRemovedDeparture) {
      this.earliestRemovedDeparture = earliestRemovedDeparture;
      return this;
    }

    public Instant latestRemovedDeparture() {
      return latestRemovedDeparture;
    }

    public Builder withLatestRemovedDeparture(Instant latestRemovedDeparture) {
      this.latestRemovedDeparture = latestRemovedDeparture;
      return this;
    }

    public ItinerarySortKey pageCut() {
      return pageCut;
    }

    public Builder withPageCut(ItinerarySortKey pageCut) {
      this.pageCut = pageCut;
      return this;
    }

    public Cost generalizedCostMaxLimit() {
      return generalizedCostMaxLimit;
    }

    public Builder withGeneralizedCostMaxLimit(Cost generalizedCostMaxLimit) {
      this.generalizedCostMaxLimit = generalizedCostMaxLimit;
      return this;
    }

    public DefaultPageCursorInput build() {
      return new DefaultPageCursorInput(this);
    }
  }
}
