package org.opentripplanner.model.plan.paging.cursor;

import static org.opentripplanner.model.plan.paging.cursor.PageType.NEXT_PAGE;
import static org.opentripplanner.model.plan.paging.cursor.PageType.PREVIOUS_PAGE;

import java.time.Duration;
import java.time.Instant;
import javax.annotation.Nullable;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.plan.ItinerarySortKey;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class PageCursorFactory {

  /**
   * The search-window start and end is [inclusive, exclusive], so to calculate the start of the
   * search-window from the last time included in the search window we need to include one extra
   * minute at the end.
   * <p>
   * The value is set to a minute because raptor operates in one minute increments.
   */
  private static final Duration SEARCH_WINDOW_END_EXCLUSIVITY_TIME_ADDITION = Duration.ofMinutes(1);

  private final SortOrder sortOrder;
  private final Duration newSearchWindow;
  private PageType currentPageType;
  private Instant currentEdt = null;
  private Instant currentLat = null;
  private Duration currentSearchWindow = null;
  private boolean wholeSearchWindowUsed = true;
  private ItinerarySortKey itineraryPageCut = null;
  private PageCursorInput pageCursorInput = null;
  private Instant firstSearchLatestItineraryDeparture = null;

  private PageCursor nextCursor = null;
  private PageCursor prevCursor = null;

  public PageCursorFactory(SortOrder sortOrder, Duration newSearchWindow) {
    this.sortOrder = sortOrder;
    this.newSearchWindow = newSearchWindow;
  }

  /**
   * Set the original search earliest-departure-time({@code edt}), latest-arrival-time ({@code lat},
   * optional) and the search-window used. Also resolve the page-type and
   * first-search-latest-itinerary-departure.
   */
  public PageCursorFactory withOriginalSearch(
    @Nullable PageType pageType,
    @Nullable Instant firstItineraryResultDeparture,
    Instant edt,
    Instant lat,
    Duration searchWindow
  ) {
    this.currentPageType = pageType == null
      ? resolvePageTypeForTheFirstSearch(sortOrder)
      : pageType;
    this.firstSearchLatestItineraryDeparture = resolveFirstSearchLatestItineraryDeparture(
      pageType,
      firstItineraryResultDeparture,
      edt
    );

    this.currentEdt = edt;
    this.currentLat = lat;
    this.currentSearchWindow = searchWindow;
    return this;
  }

  /**
   * This adds the page cursor input to the factory. The cursor input contains information about filtering results.
   * <p>
   * If there were itineraries removed in the current search because the numItineraries parameter
   * was used, then we want to allow the caller to move within some of the itineraries that were
   * removed in the next and previous pages. This means we will use information from when we cropped
   * the list of itineraries to create the new search encoded in the page cursors. We will also add
   * information necessary for removing potential duplicates when paging.
   *
   * @param pageCursorInput contains the generated page cursor input
   */
  public PageCursorFactory withPageCursorInput(PageCursorInput pageCursorInput) {
    this.pageCursorInput = pageCursorInput;
    // If the whole search window was not used (i.e. if there were removed itineraries)
    if (pageCursorInput.pageCut() != null) {
      this.wholeSearchWindowUsed = false;
      this.itineraryPageCut = pageCursorInput.pageCut();
    }
    return this;
  }

  @Nullable
  public PageCursor previousPageCursor() {
    createPageCursors();
    return prevCursor;
  }

  @Nullable
  public PageCursor nextPageCursor() {
    createPageCursors();
    return nextCursor;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(PageCursorFactory.class)
      .addEnum("sortOrder", sortOrder)
      .addEnum("currentPageType", currentPageType)
      .addDateTime("currentEdt", currentEdt)
      .addDateTime("currentLat", currentLat)
      .addDuration("currentSearchWindow", currentSearchWindow)
      .addDuration("newSearchWindow", newSearchWindow)
      .addBoolIfTrue("searchWindowCropped", !wholeSearchWindowUsed)
      .addObj("pageCursorFactoryParams", pageCursorInput)
      .addObj("nextCursor", nextCursor)
      .addObj("prevCursor", prevCursor)
      .toString();
  }

  /**
   * For the original/first search we set the page type equals to NEXT if the sort order is
   * ascending, and to PREVIOUS if descending. We do this because the logic for the first search is
   * equivalent when creating new cursors.
   */
  private static PageType resolvePageTypeForTheFirstSearch(SortOrder sortOrder) {
    return sortOrder.isSortedByAscendingArrivalTime() ? NEXT_PAGE : PREVIOUS_PAGE;
  }

  /**
   * If the first search is an arrive by search (PREVIOUS_PAGE type), the current search window is
   * misleading. Instead of using the current search window to set the page cursor of the next
   * page, the departure time of the latest itinerary result is used to avoid missing itineraries.
   */
  private Instant resolveFirstSearchLatestItineraryDeparture(
    @Nullable PageType pageType,
    @Nullable Instant firstItineraryResultDeparture,
    Instant edt
  ) {
    if (pageType == null && resolvePageTypeForTheFirstSearch(sortOrder) == PREVIOUS_PAGE) {
      if (firstItineraryResultDeparture != null) {
        return firstItineraryResultDeparture;
      } else {
        return edt;
      }
    }
    return null;
  }

  /** Create page cursor pair (next and previous) */
  private void createPageCursors() {
    if (currentEdt == null || nextCursor != null || prevCursor != null) {
      return;
    }

    Instant prevEdt;
    Instant nextEdt;

    if (wholeSearchWindowUsed) {
      prevEdt = edtBeforeNewSw();
      nextEdt = edtAfterUsedSw();
    }
    // If the whole search window was not used (i.e. if there were removed itineraries)
    else {
      if (currentPageType == NEXT_PAGE) {
        prevEdt = edtBeforeNewSw();
        nextEdt = pageCursorInput.earliestRemovedDeparture();
      } else {
        prevEdt = pageCursorInput
          .latestRemovedDeparture()
          .minus(newSearchWindow)
          .plus(SEARCH_WINDOW_END_EXCLUSIVITY_TIME_ADDITION);
        nextEdt = edtAfterUsedSw();
      }
    }

    Cost generalizedCostMaxLimit = pageCursorInput.generalizedCostMaxLimit();

    prevCursor = new PageCursor(
      PREVIOUS_PAGE,
      sortOrder,
      prevEdt,
      currentLat,
      newSearchWindow,
      itineraryPageCut,
      generalizedCostMaxLimit
    );
    nextCursor = new PageCursor(
      NEXT_PAGE,
      sortOrder,
      nextEdt,
      null,
      newSearchWindow,
      itineraryPageCut,
      generalizedCostMaxLimit
    );
  }

  private Instant edtBeforeNewSw() {
    return currentEdt.minus(newSearchWindow);
  }

  private Instant edtAfterUsedSw() {
    Instant defaultEdt = currentEdt.plus(currentSearchWindow);
    if (firstSearchLatestItineraryDeparture != null) {
      Instant edtFromLatestItineraryDeparture = firstSearchLatestItineraryDeparture.plus(
        SEARCH_WINDOW_END_EXCLUSIVITY_TIME_ADDITION
      );
      if (edtFromLatestItineraryDeparture.isBefore(defaultEdt)) {
        return edtFromLatestItineraryDeparture;
      }
    }
    return defaultEdt;
  }
}
