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
  private Instant removedItineraryStartTime = null;
  private Instant removedItineraryEndTime = null;

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
   * Set the start and end time for removed itineraries. The current implementation uses the FIRST
   * removed itinerary, but this can in some cases lead to missed itineraries in the next search.
   * So, we will document here what should be done.
   * <p>
   * For case {@code depart-after-crop-sw} and {@code arrive-by-crop-sw-reversed-filter} the {@code
   * startTime} should be the EARLIEST departure time for all removed itineraries.
   * <p>
   * For case {@code depart-after-crop-sw-reversed-filter} and {@code arrive-by-crop-sw} the {@code
   * startTime} should be the LATEST departure time for all removed itineraries.
   * <p>
   * The {@code endTime} should be replaced by removing duplicates between the to pages. This can
   * for example be done by including a hash for each potential itinerary in the token, and make a
   * filter to remove those in the following page response.
   *
   * @param startTime is rounded down to the closest minute.
   * @param endTime   is round up to the closest minute.
   */
  public PageCursorFactory withRemovedItineraries(Instant startTime, Instant endTime) {
    this.wholeSwUsed = false;
    this.removedItineraryStartTime = startTime.truncatedTo(ChronoUnit.MINUTES);
    this.removedItineraryEndTime = endTime.plusSeconds(59).truncatedTo(ChronoUnit.MINUTES);
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
      .addDateTime("removedItineraryStartTime", removedItineraryStartTime)
      .addDateTime("removedItineraryEndTime", removedItineraryEndTime)
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
    return sortOrder.isSortedByArrivalTimeAcceding() ? NEXT_PAGE : PREVIOUS_PAGE;
  }

  /** Create page cursor pair (next and previous) */
  private void createPageCursors() {
    if (current == null || nextCursor != null || prevCursor != null) {
      return;
    }

    SearchTime prev = new SearchTime(null, null);
    SearchTime next = new SearchTime(null, null);

    // Depart after, sort on arrival time with the earliest first
    if (sortOrder.isSortedByArrivalTimeAcceding()) {
      if (currentPageType == NEXT_PAGE) {
        prev.edt = calcPrevSwStartRelativeToUsedSw();
        next.edt = wholeSwUsed ? calcNextSwStartRelativeToUsedSw() : removedItineraryStartTime;
      }
      // current page type == PREV_PAGE
      else {
        if (wholeSwUsed) {
          prev.edt = calcPrevSwStartRelativeToUsedSw();
        } else {
          //TODO: The start time for the removed itinerary is not the best thing to use
          //      here. We should take the LATEST start time of all removed itineraries
          //      instead.
          prev.edt = calcPrevSwStartRelativeToRmItinerary();
          prev.lat = removedItineraryEndTime;
        }
        next.edt = calcNextSwStartRelativeToUsedSw();
      }
    }
    // Arrive-by, sort on departure time with the latest first
    else {
      if (currentPageType == PREVIOUS_PAGE) {
        if (wholeSwUsed) {
          prev.edt = calcPrevSwStartRelativeToUsedSw();
          prev.lat = current.lat;
        } else {
          prev.edt = calcPrevSwStartRelativeToRmItinerary();
          // TODO: Replace this by hashing removed itineraries
          prev.lat = removedItineraryEndTime;
        }
        next.edt = calcNextSwStartRelativeToUsedSw();
      }
      // Use normal sort and removal in ItineraryFilterChain
      else {
        prev.edt = calcPrevSwStartRelativeToUsedSw();
        prev.lat = current.lat;
        next.edt = wholeSwUsed ? calcNextSwStartRelativeToUsedSw() : removedItineraryStartTime;
      }
    }
    prevCursor = new PageCursor(PREVIOUS_PAGE, sortOrder, prev.edt, prev.lat, newSearchWindow);
    nextCursor = new PageCursor(NEXT_PAGE, sortOrder, next.edt, next.lat, newSearchWindow);
  }

  /**
   * The search-window start and end is [inclusive, exclusive], so to calculate the start of the
   * search-window from the last time included in the search window we need to include one extra
   * minute at the end.
   */
  private Instant calcPrevSwStartRelativeToRmItinerary() {
    return removedItineraryStartTime.minus(newSearchWindow).plusSeconds(60);
  }

  private Instant calcPrevSwStartRelativeToUsedSw() {
    return current.edt.minus(newSearchWindow);
  }

  private Instant calcNextSwStartRelativeToUsedSw() {
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
