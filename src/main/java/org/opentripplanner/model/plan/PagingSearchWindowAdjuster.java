package org.opentripplanner.model.plan;

import java.time.Duration;
import java.time.Instant;
import java.util.List;


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
     * We look at the search data after the trip search to see if we should adjust the search-
     * window. This is done to avoid short search windows in low frequency areas, where the client
     * would need to do multiple new request to fetch the next trips.
     */
    public Duration calculateNewSearchWindow(
            Duration usedSearchWindow,
            Instant earliestDepartureTime,
            Instant latestDepartureTime,
            int nItinerariesInSearchWindow
    ) {
        int sw = (int) usedSearchWindow.toMinutes();

        // The search result was reduced using the max itineraries limit
        if (latestDepartureTime != null) {
            long actualSW = Duration.between(earliestDepartureTime, latestDepartureTime).toSeconds();
            long diffSW = usedSearchWindow.toSeconds() - actualSW;

            // Remove 7/8 (87.5%) of the unused part of the search-window. We leave a little slack
            // to be sure we get enough results to fill up the num-of-itineraries in the next
            // result as well.
            int newSearchWindow = (int) (usedSearchWindow.toSeconds() - 7 * diffSW / 8);

            // Round down to minutes
            return normalizeSearchWindow(newSearchWindow);
        }
        if (nItinerariesInSearchWindow < pagingSearchWindowAdjustments.length) {
            return normalizeSearchWindow(
                    // Multiply minutes with 60 to get seconds
                    60 * (sw + pagingSearchWindowAdjustments[nItinerariesInSearchWindow])
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
