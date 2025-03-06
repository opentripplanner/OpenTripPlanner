package org.opentripplanner.model.plan.paging.cursor;

import static org.opentripplanner.model.plan.paging.cursor.PageType.NEXT_PAGE;
import static org.opentripplanner.model.plan.paging.cursor.PageType.PREVIOUS_PAGE;

import java.time.Duration;
import java.time.Instant;
import javax.annotation.Nullable;
import org.opentripplanner.model.plan.ItinerarySortKey;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class PageCursorFactory {

  private final SortOrder sortOrder;
  private final Duration newSearchWindow;
  private PageType currentPageType;
  private Instant currentEdt = null;
  private Instant currentLat = null;
  private Duration currentSearchWindow = null;
  private boolean wholeSwUsed = true;
  private ItinerarySortKey itineraryPageCut = null;
  private PageCursorInput pageCursorInput = null;

  private PageCursor nextCursor = null;
  private PageCursor prevCursor = null;

  public PageCursorFactory(SortOrder sortOrder, Duration newSearchWindow) {
    this.sortOrder = sortOrder;
    this.newSearchWindow = newSearchWindow;
  }

  /**
   * Set the original search earliest-departure-time({@code edt}), latest-arrival-time ({@code lat},
   * optional) and the search-window used.
   */
  public PageCursorFactory withOriginalSearch(
    @Nullable PageType pageType,
    Instant edt,
    Instant lat,
    Duration searchWindow
  ) {
    this.currentPageType = pageType == null
      ? resolvePageTypeForTheFirstSearch(sortOrder)
      : pageType;

    this.currentEdt = edt;
    this.currentLat = lat;
    this.currentSearchWindow = searchWindow;
    return this;
  }

  /**
   * If there were itineraries removed in the current search because the numItineraries parameter
   * was used, then we want to allow the caller to move within some of the itineraries that were
   * removed in the next and previous pages. This means we will use information from when we cropped
   * the list of itineraries to create the new search encoded in the page cursors. We will also add
   * information necessary for removing potential duplicates when paging.
   *
   * @param pageCursorFactoryParams contains the result from the {@code PagingDuplicateFilter}
   */
  public PageCursorFactory withRemovedItineraries(PageCursorInput pageCursorFactoryParams) {
    this.wholeSwUsed = false;
    this.pageCursorInput = pageCursorFactoryParams;
    this.itineraryPageCut = pageCursorFactoryParams.pageCut();
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
      .addBoolIfTrue("searchWindowCropped", !wholeSwUsed)
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

  /** Create page cursor pair (next and previous) */
  private void createPageCursors() {
    if (currentEdt == null || nextCursor != null || prevCursor != null) {
      return;
    }

    Instant prevEdt;
    Instant nextEdt;

    if (wholeSwUsed) {
      prevEdt = edtBeforeNewSw();
      nextEdt = edtAfterUsedSw();
    }
    // If the whole search window was not used (i.e. if there were removed itineraries)
    else {
      if (currentPageType == NEXT_PAGE) {
        prevEdt = edtBeforeNewSw();
        nextEdt = pageCursorInput.earliestRemovedDeparture();
      } else {
        // The search-window start and end is [inclusive, exclusive], so to calculate the start of the
        // search-window from the last time included in the search window we need to include one extra
        // minute at the end.
        prevEdt = pageCursorInput.latestRemovedDeparture().minus(newSearchWindow).plusSeconds(60);
        nextEdt = edtAfterUsedSw();
      }
    }
    prevCursor = new PageCursor(
      PREVIOUS_PAGE,
      sortOrder,
      prevEdt,
      currentLat,
      newSearchWindow,
      itineraryPageCut
    );
    nextCursor = new PageCursor(
      NEXT_PAGE,
      sortOrder,
      nextEdt,
      null,
      newSearchWindow,
      itineraryPageCut
    );
  }

  private Instant edtBeforeNewSw() {
    return currentEdt.minus(newSearchWindow);
  }

  private Instant edtAfterUsedSw() {
    return currentEdt.plus(currentSearchWindow);
  }
}
