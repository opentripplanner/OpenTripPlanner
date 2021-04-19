package org.opentripplanner.transit.raptor.rangeraptor.transit;

import javax.annotation.Nullable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

public class TripScheduleEventMatcher {

    /**
     * Find a matching trip in the timetable and return the index to the trip.
     *
     * @param <T> The TripSchedule type defined by the user of the raptor API.
     */
    public static <T extends RaptorTripSchedule> int findTripIndex(RaptorTimeTable<T> timeTable, T trip) {
        // Loop trough the trips to find the right one. This is probably inefficient,
        // but with relatively few guaranteed transfers it is hopefully good enough.
        final int n = timeTable.numberOfTripSchedules();
        for (int i = 0; i < n; i++) {
            T candidate = timeTable.getTripSchedule(i);
            if(trip.equals(candidate)) {
                return i;
            }
        }
        return -1;
    }
}
