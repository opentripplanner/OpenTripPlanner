package org.opentripplanner.model.plan.paging.cursor;

import java.util.OptionalInt;
import org.opentripplanner.routing.algorithm.filterchain.filters.system.NumItinerariesFilterResults;
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
 * The RemoveTransitIfStreetOnlyIsBetter filter removes transit itineraries if the best street only itinerary has a lower cost.
 * This class stores the cost of the best street only itinerary for use with paging.
 */
public class DefaultPageCursorInput implements PageCursorInput {

  private final NumItinerariesFilterResults numItinerariesFilterResults;
  private final OptionalInt bestStreetOnlyCost;

  public DefaultPageCursorInput() {
    this.numItinerariesFilterResults = null;
    this.bestStreetOnlyCost = OptionalInt.empty();
  }

  public DefaultPageCursorInput(Builder builder) {
    this.numItinerariesFilterResults = builder.numItinerariesFilterResults();
    this.bestStreetOnlyCost = builder.bestStreetOnlyCost();
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
  public OptionalInt bestStreetOnlyCost() {
    return bestStreetOnlyCost;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(DefaultPageCursorInput.class)
      .addObj("numItinerariesFilterResults", numItinerariesFilterResults)
      .addObj("bestStreetOnlyCost", bestStreetOnlyCost)
      .toString();
  }

  public static class Builder {

    private NumItinerariesFilterResults numItinerariesFilterResults;
    private OptionalInt bestStreetOnlyCost;

    public Builder(DefaultPageCursorInput original) {
      this.numItinerariesFilterResults = original.numItinerariesFilterResults;
      this.bestStreetOnlyCost = original.bestStreetOnlyCost;
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

    public OptionalInt bestStreetOnlyCost() {
      return bestStreetOnlyCost;
    }

    public Builder withBestStreetOnlyCost(OptionalInt bestStreetOnlyCost) {
      this.bestStreetOnlyCost = bestStreetOnlyCost;
      return this;
    }

    public DefaultPageCursorInput build() {
      return new DefaultPageCursorInput(this);
    }
  }
}
