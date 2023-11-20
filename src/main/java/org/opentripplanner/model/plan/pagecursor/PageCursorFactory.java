package org.opentripplanner.model.plan.pagecursor;

import static org.opentripplanner.model.plan.pagecursor.PageType.NEXT_PAGE;
import static org.opentripplanner.model.plan.pagecursor.PageType.PREVIOUS_PAGE;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.annotation.Nullable;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.model.plan.SortOrder;

public class PageCursorFactory {

  private final SortOrder sortOrder;
  private final Duration newSearchWindow;
  private PageType currentPageType;
  private SearchTime current = null;
  private Duration currentSearchWindow = null;
  private boolean wholeSwUsed = true;
  private ItineraryPageCut itineraryPageCut = null;
  private PageCursorFactoryParameters pageCursorFactoryParams = null;

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
    this.currentPageType =
      pageType == null ? resolvePageTypeForTheFirstSearch(sortOrder) : pageType;

    this.current = new SearchTime(edt, lat);
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
  public PageCursorFactory withRemovedItineraries(
    PageCursorFactoryParameters pageCursorFactoryParams
  ) {
    this.wholeSwUsed = false;
    this.pageCursorFactoryParams = pageCursorFactoryParams;
    this.itineraryPageCut =
      new ItineraryPageCut(
        pageCursorFactoryParams.earliestRemovedDeparture().truncatedTo(ChronoUnit.SECONDS),
        current.edt.plus(currentSearchWindow),
        sortOrder,
        pageCursorFactoryParams.deduplicationSection(),
        pageCursorFactoryParams.firstRemovedArrivalTime(),
        pageCursorFactoryParams.firstRemovedDepartureTime(),
        pageCursorFactoryParams.firstRemovedGeneralizedCost(),
        pageCursorFactoryParams.firstRemovedNumOfTransfers(),
        pageCursorFactoryParams.firstRemovedIsOnStreetAllTheWay()
      );
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
    return ToStringBuilder
      .of(PageCursorFactory.class)
      .addEnum("sortOrder", sortOrder)
      .addEnum("currentPageType", currentPageType)
      .addObj("current", current)
      .addDuration("currentSearchWindow", currentSearchWindow)
      .addDuration("newSearchWindow", newSearchWindow)
      .addBoolIfTrue("searchWindowCropped", !wholeSwUsed)
      .addObj("pageCursorFactoryParams", pageCursorFactoryParams)
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
    return sortOrder.isSortedByArrivalTimeAscending() ? NEXT_PAGE : PREVIOUS_PAGE;
  }

  /** Create page cursor pair (next and previous) */
  private void createPageCursors() {
    if (current == null || nextCursor != null || prevCursor != null) {
      return;
    }

    SearchTime prev = new SearchTime(null, null);
    SearchTime next = new SearchTime(null, null);

    if (wholeSwUsed) {
      prev.edt = edtBeforeNewSw();
      next.edt = edtAfterUsedSw();
      if (!sortOrder.isSortedByArrivalTimeAscending()) {
        prev.lat = current.lat;
      }
    } else { // If the whole search window was not used (i.e. if there were removed itineraries)
      if (currentPageType == NEXT_PAGE) {
        prev.edt = edtBeforeNewSw();
        next.edt = pageCursorFactoryParams.earliestRemovedDeparture();
        if (sortOrder.isSortedByArrivalTimeAscending()) {
          prev.lat = pageCursorFactoryParams.earliestKeptArrival().truncatedTo(ChronoUnit.MINUTES);
        } else {
          prev.lat = current.lat;
        }
      } else {
        // The search-window start and end is [inclusive, exclusive], so to calculate the start of the
        // search-window from the last time included in the search window we need to include one extra
        // minute at the end.
        prev.edt =
          pageCursorFactoryParams.latestRemovedDeparture().minus(newSearchWindow).plusSeconds(60);
        next.edt = edtAfterUsedSw();
        prev.lat = pageCursorFactoryParams.latestRemovedArrival();
      }
    }
    prevCursor = new PageCursor(PREVIOUS_PAGE, sortOrder, prev.edt, prev.lat, newSearchWindow);
    nextCursor = new PageCursor(NEXT_PAGE, sortOrder, next.edt, next.lat, newSearchWindow);

    if (itineraryPageCut != null) {
      nextCursor = nextCursor.withItineraryPageCut(itineraryPageCut);
      prevCursor = prevCursor.withItineraryPageCut(itineraryPageCut);
    }
  }

  private Instant edtBeforeNewSw() {
    return current.edt.minus(newSearchWindow);
  }

  private Instant edtAfterUsedSw() {
    return current.edt.plus(currentSearchWindow);
  }

  /** Temporary data class used to hold a pair of edt and lat */
  private static class SearchTime {

    Instant edt;
    Instant lat;

    private SearchTime(Instant edt, Instant lat) {
      this.edt = edt;
      this.lat = lat;
    }

    @Override
    public String toString() {
      return ToStringBuilder
        .of(SearchTime.class)
        .addDateTime("edt", edt)
        .addDateTime("lat", lat)
        .toString();
    }
  }
}
