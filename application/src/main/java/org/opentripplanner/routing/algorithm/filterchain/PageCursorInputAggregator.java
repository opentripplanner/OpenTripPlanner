package org.opentripplanner.routing.algorithm.filterchain;

import java.util.function.Consumer;
import org.opentripplanner.model.plan.paging.cursor.PageCursorInput;
import org.opentripplanner.routing.algorithm.filterchain.filters.system.NumItinerariesFilter;
import org.opentripplanner.routing.algorithm.filterchain.filters.transit.RemoveTransitIfStreetOnlyIsBetter;
import org.opentripplanner.routing.algorithm.filterchain.paging.DefaultPageCursorInput;

/**
 * This class aggregates results from NumItinerariesFilter and RemoveTransitIfStreetOnlyIsBetter for PageCursorInput.
 */
public class PageCursorInputAggregator {

  private final NumItinerariesFilter numItinerariesFilter;
  private final RemoveTransitIfStreetOnlyIsBetter removeTransitIfStreetOnlyIsBetter;
  private final Consumer<PageCursorInput> pageCursorInputSubscriber;

  public static PageCursorInputAggregator.Builder of() {
    return new Builder(new PageCursorInputAggregator());
  }

  private PageCursorInputAggregator() {
    this.numItinerariesFilter = null;
    this.removeTransitIfStreetOnlyIsBetter = null;
    this.pageCursorInputSubscriber = null;
  }

  private PageCursorInputAggregator(Builder builder) {
    this.numItinerariesFilter = builder.numItinerariesFilter();
    this.removeTransitIfStreetOnlyIsBetter = builder.removeTransitIfStreetOnlyIsBetter();
    this.pageCursorInputSubscriber = builder.pageCursorInputSubscriber();
  }

  public void providePageCursorInput() {
    DefaultPageCursorInput.Builder pageCursorInputBuilder = DefaultPageCursorInput.of();
    if (
      numItinerariesFilter != null && numItinerariesFilter.getNumItinerariesFilterResult() != null
    ) {
      pageCursorInputBuilder = pageCursorInputBuilder
        .withEarliestRemovedDeparture(
          numItinerariesFilter.getNumItinerariesFilterResult().earliestRemovedDeparture()
        )
        .withLatestRemovedDeparture(
          numItinerariesFilter.getNumItinerariesFilterResult().latestRemovedDeparture()
        )
        .withPageCut(numItinerariesFilter.getNumItinerariesFilterResult().pageCut());
    }
    if (
      removeTransitIfStreetOnlyIsBetter != null &&
      removeTransitIfStreetOnlyIsBetter.getRemoveTransitIfStreetOnlyIsBetterResult() != null
    ) {
      pageCursorInputBuilder = pageCursorInputBuilder.withGeneralizedCostMaxLimit(
        removeTransitIfStreetOnlyIsBetter
          .getRemoveTransitIfStreetOnlyIsBetterResult()
          .generalizedCostMaxLimit()
      );
    }

    pageCursorInputSubscriber.accept(pageCursorInputBuilder.build());
  }

  public static class Builder {

    private NumItinerariesFilter numItinerariesFilter;
    private RemoveTransitIfStreetOnlyIsBetter removeTransitIfStreetOnlyIsBetter;
    private Consumer<PageCursorInput> pageCursorInputSubscriber;

    public Builder(PageCursorInputAggregator original) {
      this.numItinerariesFilter = original.numItinerariesFilter;
      this.removeTransitIfStreetOnlyIsBetter = original.removeTransitIfStreetOnlyIsBetter;
      this.pageCursorInputSubscriber = original.pageCursorInputSubscriber;
    }

    public NumItinerariesFilter numItinerariesFilter() {
      return numItinerariesFilter;
    }

    public Builder withNumItinerariesFilter(NumItinerariesFilter numItinerariesFilter) {
      this.numItinerariesFilter = numItinerariesFilter;
      return this;
    }

    public RemoveTransitIfStreetOnlyIsBetter removeTransitIfStreetOnlyIsBetter() {
      return removeTransitIfStreetOnlyIsBetter;
    }

    public Builder withRemoveTransitIfStreetOnlyIsBetter(
      RemoveTransitIfStreetOnlyIsBetter removeTransitIfStreetOnlyIsBetter
    ) {
      this.removeTransitIfStreetOnlyIsBetter = removeTransitIfStreetOnlyIsBetter;
      return this;
    }

    public Consumer<PageCursorInput> pageCursorInputSubscriber() {
      return pageCursorInputSubscriber;
    }

    public Builder withPageCursorInputSubscriber(
      Consumer<PageCursorInput> pageCursorInputSubscriber
    ) {
      this.pageCursorInputSubscriber = pageCursorInputSubscriber;
      return this;
    }

    public PageCursorInputAggregator build() {
      return new PageCursorInputAggregator(this);
    }
  }
}
