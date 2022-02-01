package org.opentripplanner.model.plan;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.annotation.Nullable;

/**
 * The purpose of this class is to adjust the search-window for the next page so it better
 * matches the requested number-of-itineraries. There is no exact science behind the logic in this
 * class, but it performs well in practise, scaling fast to the appropriate size.
 */
public final class PagingSearchWindowAdjuster {

    private final Duration minSearchWindow;

    private final Duration maxSearchWindow;

    /**
     * Extra time is added to the search-window for the next request if the current
     * result has few itineraries.
     * <p>
     * Unit: minutes
     */
    private final int[] pagingSearchWindowAdjustments;

    public PagingSearchWindowAdjuster(
            int minSearchWindowMinutes,
            int maxSearchWindowMinutes,
            List<Duration> pagingSearchWindowAdjustments
    ) {
        this.minSearchWindow = Duration.ofMinutes(minSearchWindowMinutes);
        this.maxSearchWindow = Duration.ofMinutes(maxSearchWindowMinutes);
        this.pagingSearchWindowAdjustments = pagingSearchWindowAdjustments.stream()
                .mapToInt(d -> (int)d.toMinutes())
                .toArray();
    }

    /**
     * Take the given search-window and adjust it so it better matches the number-of-itineraries
     * found in the seach - this is likely to be a good estimate for the next/previous page.
     *
     * @param searchWindowUsed         The search window used by raptor
     * @param nActual                  The number of itineraries to be returned to the client
     * @param nRequested               The number of itineraries requested by the client
     * @param searchWindowStartTime    The start time for the search window used
     * @param rmItineraryDepartureTime If the search-window is cropped, this is the departure time
     *                                 of the first removed itinerary. This should be {@code null}
     *                                 if the search-window is not cropped in the itinerary filter.
     * @param swCropHead               This indicates witch end of the list of itineraries witch
     *                                 is cropped. If {@code true} the list is cropped in the
     *                                 beginning, and if {@code false} if is cropped at the end. If
     *                                 not cropped, then we do not care.
     */
    public Duration adjustSearchWindow(
            Duration searchWindowUsed,
            int nActual,
            int nRequested,
            Instant searchWindowStartTime,
            @Nullable Instant rmItineraryDepartureTime,
            boolean swCropHead
            ) {
        if(nActual == nRequested) { return searchWindowUsed; }

        // n < numItineraries
        if(nActual < nRequested) {
            return increaseOrKeepSearchWindow(searchWindowUsed, nActual);
        }

        // For some reason the search-window is not cropped in the itinerary-filter, so
        // we use the number-of-itineraries to estimate the actual search-window
        if(rmItineraryDepartureTime == null) {
            return reduceSearchWindow(
                    Duration.ofSeconds(searchWindowUsed.getSeconds() * nRequested / nActual),
                    searchWindowUsed
            );
        }

        // Search-window cropped
        Duration searchWindowSlice = swCropHead
                ? Duration.between(rmItineraryDepartureTime, searchWindowStartTime.plus(searchWindowUsed))
                : Duration.between(searchWindowStartTime, rmItineraryDepartureTime);

        return reduceSearchWindow(searchWindowSlice, searchWindowUsed);
    }

    /**
     * We look at the search data after the trip search to see if we should adjust the search-
     * window. This is done to avoid short search windows in low frequency areas, where the client
     * would need to do multiple new request to fetch the next trips.
     *
     * @param searchWindowSlice The part of the search-window actually used. Estimated based on the
     *                          number of itineraries returned or the departure time of the first
     *                          removed itinerary.
     * @param searchWindowUsed The search-window used by Raptor to perform the search
     */
    Duration reduceSearchWindow(
            Duration searchWindowSlice,
            Duration searchWindowUsed
    ) {
        // The search result was reduced using the max itineraries limit
        long diffSW = searchWindowUsed.toSeconds() - searchWindowSlice.toSeconds();

        // Remove 7/8 (87.5%) of the unused part of the search-window. We leave a little slack
        // to be sure we get enough results to fill up the num-of-itineraries in the next
        // result as well.
        int newSearchWindow = (int) (searchWindowUsed.toSeconds() - 7 * diffSW / 8);

        // Round down to minutes
        return normalizeSearchWindow(newSearchWindow);
    }

    /**
     * If the number of returned itineraries are less than the requested number of itineraries,
     * then increase the search window according to the configured
     * {@code pagingSearchWindowAdjustments} This is done to avoid short search windows in low
     * frequency areas, where the client would need to do multiple new request to fetch the next
     * trips.
     */
    Duration increaseOrKeepSearchWindow(
            Duration usedSearchWindow,
            int nItinerariesInSearchWindow
    ) {
        if (nItinerariesInSearchWindow < pagingSearchWindowAdjustments.length) {
            return normalizeSearchWindow(
                    // Multiply minutes with 60 to get seconds
                    (int)usedSearchWindow.getSeconds()
                    + 60 * pagingSearchWindowAdjustments[nItinerariesInSearchWindow]
            );
        }
        // No change
        return usedSearchWindow;
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
        if(seconds > maxSearchWindow.getSeconds()) {
            return maxSearchWindow;
        }
        // Round down to the closest minute
        int minutes = seconds / 60;

        if (minutes <= 240) {
            return Duration.ofMinutes(ceiling(minutes, 10));
        }
        return Duration.ofMinutes(ceiling(minutes, 30));
    }

    /**
     * Round value to the closest increment of given {@code step}. This is used
     * to round of a time or duration to the closest "step" of like 10 minutes.
     */
    static int ceiling(int value, int step) {
        if (value < 0) {
            return (value / step) * step;
        }
        else {
            return ((value + step - 1) / step) * step;
        }
    }
}
