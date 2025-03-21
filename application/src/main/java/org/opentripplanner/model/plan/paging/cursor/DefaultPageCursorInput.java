package org.opentripplanner.model.plan.paging.cursor;

import org.opentripplanner.routing.algorithm.filterchain.filters.system.NumItinerariesFilterResults;
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

  private final NumItinerariesFilterResults numItinerariesFilterResults;
  private final RemoveTransitIfStreetOnlyIsBetterResults removeTransitIfStreetOnlyIsBetterResults;

  private DefaultPageCursorInput() {
    this.numItinerariesFilterResults = null;
    this.removeTransitIfStreetOnlyIsBetterResults = null;
  }

  private DefaultPageCursorInput(Builder builder) {
    this.numItinerariesFilterResults = builder.numItinerariesFilterResults();
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
  public NumItinerariesFilterResults numItinerariesFilterResults() {
    return numItinerariesFilterResults;
  }

  @Override
  public RemoveTransitIfStreetOnlyIsBetterResults removeTransitIfStreetOnlyIsBetterResults() {
    return removeTransitIfStreetOnlyIsBetterResults;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(DefaultPageCursorInput.class)
      .addObj("numItinerariesFilterResults", numItinerariesFilterResults)
      .addObj("removeTransitIfStreetOnlyIsBetterResults", removeTransitIfStreetOnlyIsBetterResults)
      .toString();
  }

  public static class Builder {

    private NumItinerariesFilterResults numItinerariesFilterResults;
    private RemoveTransitIfStreetOnlyIsBetterResults removeTransitIfStreetOnlyIsBetterResults;

    public Builder(DefaultPageCursorInput original) {
      this.numItinerariesFilterResults = original.numItinerariesFilterResults;
      this.removeTransitIfStreetOnlyIsBetterResults =
        original.removeTransitIfStreetOnlyIsBetterResults;
    }

    public NumItinerariesFilterResults numItinerariesFilterResults() {
      return numItinerariesFilterResults;
    }

    public Builder withNumItinerariesFilterResults(
      NumItinerariesFilterResults numItinerariesFilterResults
    ) {
      this.numItinerariesFilterResults = numItinerariesFilterResults;
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
