package org.opentripplanner.routing.algorithm.filterchain.paging;

import java.util.function.Consumer;

import org.opentripplanner.model.plan.paging.cursor.PageCursorInput;
import org.opentripplanner.routing.algorithm.filterchain.DefaultPageCursorInput;
import org.opentripplanner.routing.algorithm.filterchain.filters.system.NumItinerariesFilter;
import org.opentripplanner.routing.algorithm.filterchain.filters.transit.RemoveTransitIfStreetOnlyIsBetter;

/**
 * This class aggregates results from NumItinerariesFilter and RemoveTransitIfStreetOnlyIsBetter for PageCursorInput.
 */
public class PageCursorInputAggregator  {
  private final NumItinerariesFilter numItinerariesFilter;
  private final RemoveTransitIfStreetOnlyIsBetter removeTransitIfStreetOnlyIsBetter;
  private final Consumer<PageCursorInput> pageCursorInputSubscriber;

  private PageCursorInputAggregator(
    NumItinerariesFilter numItinerariesFilter,
    RemoveTransitIfStreetOnlyIsBetter removeTransitIfStreetOnlyIsBetter,
    Consumer<PageCursorInput> pageCursorInputSubscriber
  ) {
    this.numItinerariesFilter = numItinerariesFilter;
    this.removeTransitIfStreetOnlyIsBetter = removeTransitIfStreetOnlyIsBetter;
    this.pageCursorInputSubscriber = pageCursorInputSubscriber;
  }

  public void createPageCursorInput() {
    pageCursorInputSubscriber.accept(
      DefaultPageCursorInput.of()
      .withNumItinerariesFilterResult(numItinerariesFilter.getNumItinerariesFilterResult())
      .withRemoveTransitIfStreetOnlyIsBetterResult(removeTransitIfStreetOnlyIsBetter.getRemoveTransitIfStreetOnlyIsBetterResult())
      .build()
    );
  }
}
