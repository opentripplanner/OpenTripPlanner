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

        boolean forwards = arriveBy == reverseFilteringDirection;

        Instant edtPrev, edtNext, latPrev, latNext;

        if (arriveBy) {
            if (forwards) {
                // Previous
                edtPrev = original.edt.minus(originalSearchWindow);
                latPrev = original.lat.minus(originalSearchWindow);

                // Next
                if (!swCropped) {
                    edtNext = original.edt.plus(originalSearchWindow);
                    latNext = original.lat.plus(originalSearchWindow);
                } else {
                    edtNext = removedItineraryStartTime;
                    latNext = original.lat.plus(Duration.between(edtNext, original.edt));
                }
            }
            else {
                // Previous
                latPrev = swCropped
                        ? removedItineraryEndTime
                        : original.lat.minus(originalSearchWindow);

                edtPrev = original.edt.minus(Duration.between(latPrev, original.lat));

                // Next
                latNext = original.lat.plus(originalSearchWindow);
                edtNext = original.edt.plus(originalSearchWindow);
            }
            prevCursor = new PageCursor(edtPrev, latPrev, originalSearchWindow, false);
            nextCursor = new PageCursor(edtNext, latNext, originalSearchWindow, true);
        }
        // Switching direction, no need to take filtered itineraries into account
        else {
            if (forwards) {
                // Previous
                edtPrev = original.edt.minus(originalSearchWindow);

                // Next
                if (!swCropped) {
                    edtNext = original.edt.plus(originalSearchWindow);
                } else {
                    Instant endOfSearchWindow = original.edt.plus(originalSearchWindow);
                    edtNext = removedItineraryStartTime;
                    // If EDT would be outside originalSearchWindow, revert to end of SW
                    if (original.edt.isAfter(endOfSearchWindow)) {
                        edtNext = endOfSearchWindow;
                    }
                }
                prevCursor = new PageCursor(edtPrev, null, originalSearchWindow, true);
            }
            else {
                // Previous
                if (swCropped) {
                    latPrev = removedItineraryEndTime;
                    //TODO: we don't know what time to start at
                    edtPrev = original.edt.minus(originalSearchWindow);
                    prevCursor = new PageCursor(edtPrev, latPrev, originalSearchWindow, true);
                }
                else {
                    edtPrev = original.edt.minus(originalSearchWindow);
                    prevCursor = new PageCursor(edtPrev, null, originalSearchWindow, true);
                }

                // Next
                edtNext = original.edt.plus(originalSearchWindow);
            }
            nextCursor = new PageCursor(edtNext, null, originalSearchWindow, false);
        }
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
}
