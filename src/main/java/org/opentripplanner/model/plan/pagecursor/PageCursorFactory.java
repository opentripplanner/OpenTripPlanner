package org.opentripplanner.model.plan.pagecursor;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.annotation.Nullable;
import org.opentripplanner.model.base.ToStringBuilder;

public class PageCursorFactory {
    private final boolean arriveBy;
    private final boolean reverseFilteringDirection;
    private Search original = null;
    private Duration originalSearchWindow = null;
    private boolean swCropped = false;
    private Instant removedItineraryStartTime = null;
    private Instant removedItineraryEndTime = null;

    private PageCursor nextCursor = null;
    private PageCursor prevCursor = null;

    public PageCursorFactory(
            boolean arriveBy,
            boolean reverseFilteringDirection
    ) {
        this.arriveBy = arriveBy;
        this.reverseFilteringDirection = reverseFilteringDirection;
    }

    /**
     * Set the original search earliest-departure-time({@code edt}), latest-arrival-time
     * ({@code lat}, optional) and the search-window used.
     */
    public PageCursorFactory withOriginalSearch(Instant edt, Instant lat, Duration searchWindow) {
        this.original = new Search(edt, lat);
        this.originalSearchWindow = searchWindow;
        return this;
    }

    /**
     * Set the start and end time for removed itineraries. The current implementation uses the
     * FIRST removed itinerary, but this can in some cases lead to missed itineraries in the
     * next search. So, we will document here what should be done.
     * <p>
     * For case {@code depart-after-crop-sw} and {@code arrive-by-crop-sw-reversed-filter} the
     * {@code startTime} should be the EARLIEST departure time for all removed itineraries.
     * <p>
     * For case {@code depart-after-crop-sw-reversed-filter} and {@code arrive-by-crop-sw} the
     * {@code startTime} should be the LATEST departure time for all removed itineraries.
     * <p>
     * The {@code endTime} should be replaced by removing duplicates between the to pages.
     * This can for example be done by including a hash for each potential itinerary in the
     * token, and make a filter to remove those in the following page response.
     *
     * @param startTime is rounded down to the closest minute.
     * @param endTime is round up to the closest minute.
     */
    public PageCursorFactory withRemovedItineraries(
            Instant startTime,
            Instant endTime
    ) {
        this.swCropped = true;
        this.removedItineraryStartTime = startTime.truncatedTo(ChronoUnit.MINUTES);
        this.removedItineraryEndTime = endTime.minusSeconds(1)
                .truncatedTo(ChronoUnit.MINUTES)
                .plus(1, ChronoUnit.MINUTES);
        return this;
    }

    @Nullable
    public PageCursor previousPageCursor() {
        createPageCursors();
        return prevCursor;
    }

    @Nullable
    public  PageCursor nextPageCursor() {
        createPageCursors();
        return nextCursor;
    }

    /** Create page cursor pair (next and previous) */
    private void createPageCursors() {
        if(original == null || nextCursor != null || prevCursor != null) { return; }

        Search prev = new Search(null, null);
        Search next = new Search(null, null);

        // Depart after
        if (!arriveBy) {
            if (!reverseFilteringDirection) {
                // Previous
                prev.edt = original.edt.minus(originalSearchWindow);

                // Next
                if (!swCropped) {
                    next.edt = original.edt.plus(originalSearchWindow);
                } else {
                    Instant endOfSearchWindow = original.edt.plus(originalSearchWindow);
                    next.edt = removedItineraryStartTime;
                    // If EDT would be outside originalSearchWindow, revert to end of SW
                    if (original.edt.isAfter(endOfSearchWindow)) {
                        next.edt = endOfSearchWindow;
                    }
                }
            }
            else {
                // Previous
                if (swCropped) {
                    prev.lat = removedItineraryEndTime;
                    //TODO: we don't know what time to start at
                    prev.edt = original.edt.minus(originalSearchWindow);
                }
                else {
                    prev.edt = original.edt.minus(originalSearchWindow);
                }

                // Next
                next.edt = original.edt.plus(originalSearchWindow);
            }
        }
        // Arrive-by
        else {
            // reverse sort and removal itinerary-filter
            if (!reverseFilteringDirection) {
                prev.lat = swCropped
                        ? removedItineraryEndTime
                        : original.lat.minus(originalSearchWindow);
                prev.edt = original.edt.minus(Duration.between(prev.lat, original.lat));

                next.lat = original.lat.plus(originalSearchWindow);
                next.edt = original.edt.plus(originalSearchWindow);
            }
            // Use normal sort and removal in ItineraryFilterChain
            else {
                prev.edt = original.edt.minus(originalSearchWindow);
                prev.lat = original.lat.minus(originalSearchWindow);

                if (!swCropped) {
                    next.edt = original.edt.plus(originalSearchWindow);
                    next.lat = original.lat.plus(originalSearchWindow);
                } else {
                    next.edt = removedItineraryStartTime;
                    next.lat = original.lat.plus(Duration.between(next.edt, original.edt));
                }
            }
        }
        prevCursor = new PageCursor(prev.edt, prev.lat, originalSearchWindow, !arriveBy);
        nextCursor = new PageCursor(next.edt, next.lat, originalSearchWindow, arriveBy);
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(PageCursorFactory.class)
                .addBool("arriveBy", arriveBy)
                .addBool("reverseFilteringDirection", reverseFilteringDirection)
                .addObj("original", original)
                .addDuration("originalSearchWindow", originalSearchWindow)
                .addBool("swCropped", swCropped)
                .addTime("firstRemovedItineraryStartTime", removedItineraryStartTime)
                .addTime("firstRemovedItineraryEndTime", removedItineraryEndTime)
                .addObj("nextCursor", nextCursor)
                .addObj("prevCursor", prevCursor)
                .toString();
    }

    /** Temporary data class used to hold a pair of edt and lat */
    private static class Search {
        Instant edt;
        Instant lat;

        private Search(Instant edt, Instant lat) {
            this.edt = edt;
            this.lat = lat;
        }

        @Override
        public String toString() {
            return ToStringBuilder.of(Search.class)
                .addTime("edt", edt)
                .addTime("lat", lat)
                .toString();
        }
    }
}
