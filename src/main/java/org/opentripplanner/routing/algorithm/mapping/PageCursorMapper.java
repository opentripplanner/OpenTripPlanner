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
    public static PageCursor map(
            boolean arriveBy,
            ZonedDateTime startOfTime,
            SearchParams searchParams,
            Itinerary firstRemovedItinerary
    ) {
        if(searchParams == null) {
            return null;
        }
        long startOfTimeSec = startOfTime.toEpochSecond();
        Instant edt = Instant.ofEpochSecond(startOfTimeSec + searchParams.earliestDepartureTime());
        Instant lat = Instant.ofEpochSecond(startOfTimeSec + searchParams.latestArrivalTime());
        Duration sw = Duration.ofSeconds(searchParams.searchWindowInSeconds());

        if (firstRemovedItinerary != null) {
            if (arriveBy) {
                Instant previousLat = lat;
                lat = firstRemovedItinerary.endTime().toInstant();
                // Get start of next minute
                lat = lat
                        .minusSeconds(1)
                        .truncatedTo(ChronoUnit.MINUTES)
                        .plus(1, ChronoUnit.MINUTES);

                Duration shift = Duration.between(lat, previousLat);
                edt = edt.minus(shift);
            } else  {
                edt = firstRemovedItinerary.startTime().toInstant();
                // Get start of minute
                edt = edt.truncatedTo(ChronoUnit.MINUTES);
            }

        } else {
            if (arriveBy) {
                lat = lat.minus(sw);
                edt = edt.minus(sw);
            } else {
                edt = edt.plus(sw);
            }
        }



        if(arriveBy) {
            return PageCursor.arriveByCursor(edt, lat, sw);
        }
        else {
            return PageCursor.departAfterCursor(edt, sw);
        }
    }
}
