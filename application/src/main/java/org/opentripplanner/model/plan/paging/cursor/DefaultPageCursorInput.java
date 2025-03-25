package org.opentripplanner.model.plan.paging.cursor;

import org.opentripplanner.routing.algorithm.filterchain.filters.system.NumItinerariesFilterResult;
import org.opentripplanner.routing.algorithm.filterchain.filters.transit.RemoveTransitIfStreetOnlyIsBetterResults;
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
  private final RemoveTransitIfStreetOnlyIsBetterResults removeTransitIfStreetOnlyIsBetterResults;

  private DefaultPageCursorInput() {
    this.numItinerariesFilterResult = null;
    this.removeTransitIfStreetOnlyIsBetterResults = null;
  }

  private DefaultPageCursorInput(Builder builder) {
    this.numItinerariesFilterResult = builder.numItinerariesFilterResult();
    this.removeTransitIfStreetOnlyIsBetterResults =
      builder.removeTransitIfStreetOnlyIsBetterResults();
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
  public RemoveTransitIfStreetOnlyIsBetterResults removeTransitIfStreetOnlyIsBetterResults() {
    return removeTransitIfStreetOnlyIsBetterResults;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(DefaultPageCursorInput.class)
      .addObj("numItinerariesFilterResult", numItinerariesFilterResult)
      .addObj("removeTransitIfStreetOnlyIsBetterResults", removeTransitIfStreetOnlyIsBetterResults)
      .toString();
  }

  public static class Builder {

    private NumItinerariesFilterResult numItinerariesFilterResult;
    private RemoveTransitIfStreetOnlyIsBetterResults removeTransitIfStreetOnlyIsBetterResults;

    public Builder(DefaultPageCursorInput original) {
      this.numItinerariesFilterResult = original.numItinerariesFilterResult;
      this.removeTransitIfStreetOnlyIsBetterResults =
        original.removeTransitIfStreetOnlyIsBetterResults;
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

    public RemoveTransitIfStreetOnlyIsBetterResults removeTransitIfStreetOnlyIsBetterResults() {
      return removeTransitIfStreetOnlyIsBetterResults;
    }

    public Builder withRemoveTransitIfStreetOnlyIsBetterResults(
      RemoveTransitIfStreetOnlyIsBetterResults removeTransitIfStreetOnlyIsBetterResults
    ) {
      this.removeTransitIfStreetOnlyIsBetterResults = removeTransitIfStreetOnlyIsBetterResults;
      return this;
    }

    public DefaultPageCursorInput build() {
      return new DefaultPageCursorInput(this);
    }
  }
}
