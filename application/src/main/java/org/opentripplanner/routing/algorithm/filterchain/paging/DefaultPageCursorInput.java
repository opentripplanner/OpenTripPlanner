package org.opentripplanner.routing.algorithm.filterchain.paging;

import org.opentripplanner.model.plan.paging.cursor.PageCursorInput;
import org.opentripplanner.routing.algorithm.filterchain.filters.system.NumItinerariesFilterResult;
import org.opentripplanner.routing.algorithm.filterchain.filters.transit.RemoveTransitIfStreetOnlyIsBetterResult;
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

  private final NumItinerariesFilterResult numItinerariesFilterResult;
  private final RemoveTransitIfStreetOnlyIsBetterResult removeTransitIfStreetOnlyIsBetterResult;

  private DefaultPageCursorInput() {
    this.numItinerariesFilterResult = null;
    this.removeTransitIfStreetOnlyIsBetterResult = null;
  }

  private DefaultPageCursorInput(Builder builder) {
    this.numItinerariesFilterResult = builder.numItinerariesFilterResult();
    this.removeTransitIfStreetOnlyIsBetterResult =
      builder.removeTransitIfStreetOnlyIsBetterResult();
  }

  public static DefaultPageCursorInput.Builder of() {
    return new Builder(new DefaultPageCursorInput());
  }

  public DefaultPageCursorInput.Builder copyOf() {
    return new Builder(this);
  }

  @Override
  public NumItinerariesFilterResult numItinerariesFilterResult() {
    return numItinerariesFilterResult;
  }

  @Override
  public RemoveTransitIfStreetOnlyIsBetterResult removeTransitIfStreetOnlyIsBetterResult() {
    return removeTransitIfStreetOnlyIsBetterResult;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(DefaultPageCursorInput.class)
      .addObj("numItinerariesFilterResult", numItinerariesFilterResult)
      .addObj("removeTransitIfStreetOnlyIsBetterResult", removeTransitIfStreetOnlyIsBetterResult)
      .toString();
  }

  public static class Builder {

    private NumItinerariesFilterResult numItinerariesFilterResult;
    private RemoveTransitIfStreetOnlyIsBetterResult removeTransitIfStreetOnlyIsBetterResult;

    public Builder(DefaultPageCursorInput original) {
      this.numItinerariesFilterResult = original.numItinerariesFilterResult;
      this.removeTransitIfStreetOnlyIsBetterResult =
        original.removeTransitIfStreetOnlyIsBetterResult;
    }

    public NumItinerariesFilterResult numItinerariesFilterResult() {
      return numItinerariesFilterResult;
    }

    public Builder withNumItinerariesFilterResult(
      NumItinerariesFilterResult numItinerariesFilterResult
    ) {
      this.numItinerariesFilterResult = numItinerariesFilterResult;
      return this;
    }

    public RemoveTransitIfStreetOnlyIsBetterResult removeTransitIfStreetOnlyIsBetterResult() {
      return removeTransitIfStreetOnlyIsBetterResult;
    }

    public Builder withRemoveTransitIfStreetOnlyIsBetterResult(
      RemoveTransitIfStreetOnlyIsBetterResult removeTransitIfStreetOnlyIsBetterResult
    ) {
      this.removeTransitIfStreetOnlyIsBetterResult = removeTransitIfStreetOnlyIsBetterResult;
      return this;
    }

    public DefaultPageCursorInput build() {
      return new DefaultPageCursorInput(this);
    }
  }
}
