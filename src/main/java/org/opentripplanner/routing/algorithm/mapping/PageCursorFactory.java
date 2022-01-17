package org.opentripplanner.routing.algorithm.mapping;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import javax.annotation.Nullable;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PageCursor;
import org.opentripplanner.transit.raptor.api.request.SearchParams;

public class PageCursorFactory {

    private final boolean arriveBy;
    private final Itinerary firstRemovedItinerary;
    private final boolean forwards;
    private final Instant edt;
    private final Instant lat;
    private final Duration sw;
    private PageCursor next = null;
    private PageCursor previous = null;

    public PageCursorFactory(
            boolean arriveBy,
            ZonedDateTime startOfTime,
            SearchParams searchParams,
            Itinerary firstRemovedItinerary,
            boolean reverseFilteringDirection
    ) {
        this.arriveBy = arriveBy;
        this.forwards = arriveBy == reverseFilteringDirection;
        this.firstRemovedItinerary = firstRemovedItinerary;

        if(searchParams != null) {
            long startOfTimeSec = startOfTime.toEpochSecond();
            this.edt = Instant.ofEpochSecond(startOfTimeSec + searchParams.earliestDepartureTime());
            this.lat = Instant.ofEpochSecond(startOfTimeSec + searchParams.latestArrivalTime());
            this.sw = Duration.ofSeconds(searchParams.searchWindowInSeconds());
        }
        else {
            this.edt = null;
            this.lat = null;
            this.sw = null;
        }
    }

    @Nullable
    public PageCursor createPreviousPageCursor() {
        createPageCursors();
        return previous;
    }

    @Nullable
    public  PageCursor createNextPageCursor() {
        createPageCursors();
        return next;
    }

    /** Create page cursor pair (next and previous) */
    private void createPageCursors() {
        if(edt == null || next != null || previous != null) { return; }

        Instant edtPrev, edtNext, latPrev, latNext;

        if (arriveBy) {
            if (forwards) {
                // Previous
                edtPrev = edt.minus(sw);
                latPrev = lat.minus(sw);

                // Next
                if (firstRemovedItinerary == null) {
                    edtNext = edt.plus(sw);
                    latNext = lat.plus(sw);
                } else {
                    edtNext = getItineraryStartTime();
                    latNext = lat.plus(Duration.between(edtNext, edt));
                }
            }
            else {
                // Previous
                latPrev = firstRemovedItinerary == null ? lat.minus(sw) : getItineraryEndTime();
                edtPrev = edt.minus(Duration.between(latPrev, lat));

                // Next
                latNext = lat.plus(sw);
                edtNext = edt.plus(sw);
            }
            previous = PageCursor.arriveByCursor(edtPrev, latPrev, sw, false);
            next = PageCursor.arriveByCursor(edtNext, latNext, sw, true);
        }
        // Switching direction, no need to take filtered itineraries into account
        else {
            if (forwards) {
                // Previous
                edtPrev = this.edt.minus(sw);

                // Next
                if (firstRemovedItinerary == null) {
                    edtNext = edt.plus(sw);
                } else {
                    Instant endOfSearchWindow = edt.plus(sw);
                    edtNext = getItineraryStartTime();
                    // If EDT would be outside SW, revert to end of SW
                    if (edt.isAfter(endOfSearchWindow)) {
                        edtNext = endOfSearchWindow;
                    }
                }
                previous = PageCursor.departAfterCursor(edtPrev, sw, true);
            }
            else {
                // Previous
                if (firstRemovedItinerary != null) {
                    latPrev = getItineraryEndTime();
                    //TODO: we don't know what time to start at
                    edtPrev = this.edt.minus(sw);
                    previous = PageCursor.arriveByCursor(edtPrev, latPrev, sw, true);
                }
                else {
                    edtPrev = edt.minus(sw);
                    previous = PageCursor.departAfterCursor(edtPrev, sw, true);
                }

                // Next
                edtNext = edt.plus(sw);
            }
            next = PageCursor.departAfterCursor(edtNext, sw, false);
        }
    }

    /** Find the endTime for the {@code firstRemovedItinerary}, round down to the closest minute. */
    private Instant getItineraryStartTime() {
        return firstRemovedItinerary.startTime().toInstant()
                .truncatedTo(ChronoUnit.MINUTES);
    }

    /** Find the endTime for the {@code firstRemovedItinerary}, round up to the closest minute. */
    private Instant getItineraryEndTime() {
        return firstRemovedItinerary.endTime().toInstant()
                .minusSeconds(1)
                .truncatedTo(ChronoUnit.MINUTES)
                .plus(1, ChronoUnit.MINUTES);
    }
}
