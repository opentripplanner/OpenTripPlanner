package org.opentripplanner.model.plan.paging;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * The purpose of this class is to adjust the search-window for the next page so it better matches
 * the requested number-of-itineraries. There is no exact science behind the logic in this class,
 * but it performs well in practise, scaling fast to the appropriate size.
 */
public final class PagingSearchWindowAdjuster {

  private final Duration minSearchWindow;

  private final Duration maxSearchWindow;

  /**
   * Extra time is added to the search-window for the next request if the current result has few
   * itineraries.
   * <p>
   * Unit: minutes
   */
  private final int[] pagingSearchWindowAdjustments;

  public PagingSearchWindowAdjuster(
    Duration minSearchWindow,
    Duration maxSearchWindow,
    List<Duration> pagingSearchWindowAdjustments
  ) {
    this.minSearchWindow = minSearchWindow;
    this.maxSearchWindow = maxSearchWindow;
    this.pagingSearchWindowAdjustments = pagingSearchWindowAdjustments
      .stream()
      .mapToInt(d -> (int) d.toMinutes())
      .toArray();
  }

  /**
   * Take the given search-window and adjust it so it better matches the number-of-itineraries found
   * in the search - this is likely to be a good estimate for the next/previous page.
   *
   * @param searchWindowUsed         The search window used by raptor
   * @param searchWindowStartTime    The start time for the search window used
   * @param rmItineraryDepartureTime If the search-window is cropped, this is the departure time of
   *                                 the first removed itinerary. This should be {@code null} if the
   *                                 search-window is not cropped in the itinerary filter.
   * @param cropSearchWindowTail     This indicates which end of the search-window to crop. If
   *                                 {@code true} the search-window is cropped at the end, and if
   *                                 {@code false} it is cropped in the beginning. If no
   *                                 rmItineraryDepartureTime exist, then we do not care.
   */
  public Duration decreaseSearchWindow(
    Duration searchWindowUsed,
    Instant searchWindowStartTime,
    Instant rmItineraryDepartureTime,
    boolean cropSearchWindowTail
  ) {
    // We found more itineraries than requested, decrease the search window
    Duration searchWindowSlice = cropSearchWindowTail
      ? Duration.between(searchWindowStartTime, rmItineraryDepartureTime)
      : Duration.between(rmItineraryDepartureTime, searchWindowStartTime.plus(searchWindowUsed));

    return normalizeSearchWindow((int) searchWindowSlice.getSeconds());
  }

  /**
   * If the number of returned itineraries are less than the requested number of itineraries, then
   * increase the search window according to the configured {@code pagingSearchWindowAdjustments}
   * This is done to avoid short search windows in low frequency areas, where the client would need
   * to do multiple new request to fetch the next trips.
   */
  public Duration increaseOrKeepSearchWindow(
    Duration searchWindowUsed,
    int nRequestedItineraries,
    int nActualItinerariesFound
  ) {
    if (nActualItinerariesFound >= nRequestedItineraries) {
      return searchWindowUsed;
    }

    if (nActualItinerariesFound < pagingSearchWindowAdjustments.length) {
      return normalizeSearchWindow(
        // Multiply minutes with 60 to get seconds
        (int) searchWindowUsed.getSeconds() +
        60 * pagingSearchWindowAdjustments[nActualItinerariesFound]
      );
    }
    // No change
    return searchWindowUsed;
  }

  /**
   * Round value to the closest increment of given {@code step}. This is used to round of a time or
   * duration to the closest "step" of like 10 minutes.
   */
  static int ceiling(int value, int step) {
    if (value < 0) {
      return (value / step) * step;
    } else {
      return ((value + step - 1) / step) * step;
    }
  }

  /**
   * Round search-window({@code sw}) up:
   * <ul>
   *     <li>if {@code sw < minSearchWindow } then search-window is set to `minSearchWindow`
   *     <li>if {@code sw > maxSearchWindow} then return `maxSearchWindow`
   *     <li>if {@code sw <= 4h} then round search-window up to closest 10 minutes
   *     <li>if {@code sw > 4h} then round search-window up to closest 30 minutes
   * </ul>
   */
  Duration normalizeSearchWindow(int seconds) {
    if (seconds < minSearchWindow.getSeconds()) {
      return minSearchWindow;
    }
    if (seconds > maxSearchWindow.getSeconds()) {
      return maxSearchWindow;
    }
    // Round down to the closest minute
    int minutes = seconds / 60;

    if (minutes <= 240) {
      return Duration.ofMinutes(ceiling(minutes, 10));
    }
    return Duration.ofMinutes(ceiling(minutes, 30));
  }
}
