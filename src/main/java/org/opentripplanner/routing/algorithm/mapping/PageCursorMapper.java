package org.opentripplanner.routing.algorithm.mapping;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import javax.annotation.Nullable;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PageCursor;
import org.opentripplanner.transit.raptor.api.request.SearchParams;

public class PageCursorMapper {

    @Nullable
    public static PageCursor mapPreviousPage(
            boolean arriveBy,
            ZonedDateTime startOfTime,
            SearchParams searchParams,
            Itinerary firstRemovedItinerary,
            boolean reverseFilteringDirection
    ) {
        if(searchParams == null) {
            return null;
        }
        boolean forwards = arriveBy == reverseFilteringDirection;

        long startOfTimeSec = startOfTime.toEpochSecond();
        Instant edt = Instant.ofEpochSecond(startOfTimeSec + searchParams.earliestDepartureTime());
        Instant lat = Instant.ofEpochSecond(startOfTimeSec + searchParams.latestArrivalTime());
        Duration sw = Duration.ofSeconds(searchParams.searchWindowInSeconds());

        // Switching direction, no need to take filtered itineraries into account
        if (forwards) {
            edt = edt.minus(sw);
            if (arriveBy) {
                lat = lat.minus(sw);
                return PageCursor.arriveByCursor(edt, lat, sw, false);
            } else {
                return PageCursor.departAfterCursor(edt, sw, true);
            }
        } else {
            if (arriveBy) {
                if (firstRemovedItinerary != null) {
                    Instant previousLat = lat;
                    lat = firstRemovedItinerary.endTime().toInstant();
                    // Get start of next minute
                    lat = lat
                        .minusSeconds(1)
                        .truncatedTo(ChronoUnit.MINUTES)
                        .plus(1, ChronoUnit.MINUTES);

                    Duration shift = Duration.between(lat, previousLat);
                    edt = edt.minus(shift);
                } else {
                    lat = lat.minus(sw);
                    edt = edt.minus(sw);
                }
                return PageCursor.arriveByCursor(edt, lat, sw, false);
            } else {
                if (firstRemovedItinerary != null) {
                    lat = firstRemovedItinerary.endTime().toInstant();
                    // Get start of next minute
                    lat = lat
                            .minusSeconds(1)
                            .truncatedTo(ChronoUnit.MINUTES)
                            .plus(1, ChronoUnit.MINUTES);
                    //TODO: we don't know what time to start at
                    edt = edt.minus(sw);
                    return PageCursor.arriveByCursor(edt, lat, sw, true);
                }
                else {
                    edt = edt.minus(sw);
                    return PageCursor.departAfterCursor(edt, sw, true);
                }
            }
        }

    }


    @Nullable
    public static PageCursor mapNextPage(
            boolean arriveBy,
            ZonedDateTime startOfTime,
            SearchParams searchParams,
            Itinerary firstRemovedItinerary,
            boolean reverseFilteringDirection
    ) {
        if(searchParams == null) {
            return null;
        }
        boolean forwards = arriveBy == reverseFilteringDirection;

        long startOfTimeSec = startOfTime.toEpochSecond();
        Instant edt = Instant.ofEpochSecond(startOfTimeSec + searchParams.earliestDepartureTime());
        Instant lat = Instant.ofEpochSecond(startOfTimeSec + searchParams.latestArrivalTime());
        Duration sw = Duration.ofSeconds(searchParams.searchWindowInSeconds());

        if (forwards) {
            if (arriveBy) {
                if (firstRemovedItinerary != null) {
                    Instant previousEdt = edt;
                    edt = firstRemovedItinerary.startTime().toInstant();
                    edt = edt.truncatedTo(ChronoUnit.MINUTES);
                    Duration shift = Duration.between(edt, previousEdt);
                    lat = lat.plus(shift);
                } else {
                    lat = lat.plus(sw);
                    edt = edt.plus(sw);
                }
                return PageCursor.arriveByCursor(edt, lat, sw, true);
            } else {
                if (firstRemovedItinerary != null) {
                    Instant endOfSearchWindow = edt.plus(sw);
                    edt = firstRemovedItinerary.startTime().toInstant();
                    edt = edt.truncatedTo(ChronoUnit.MINUTES);
                    // If EDT would be outside SW, revert to end of SW
                    if (edt.isAfter(endOfSearchWindow)) {
                        edt = endOfSearchWindow;
                    }
                } else {
                    edt = edt.plus(sw);
                }
                return PageCursor.departAfterCursor(edt, sw, false);
            }
        // Switching direction, no need to take filtered itineraries into account
        } else {
            edt = edt.plus(sw);
            if (arriveBy) {
                lat = lat.plus(sw);
                return PageCursor.arriveByCursor(edt, lat, sw, true);
            } else {
                return PageCursor.departAfterCursor(edt, sw, false);
            }
        }
    }
}
