package org.opentripplanner.transit.raptor.rangeraptor.transit;


import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.util.TimeUtils;

/**
 * This class is used to find the board and alight time for a known trip,
 * where you now the board-stop, alight-stop and either earliest-board-time or latest-alight-time.
 * <p>
 * This class is needed to find board and alight times for transfer legs when mapping to
 * stop-arrivals to paths. The board and alight times are not stored in the stop-arrival state
 * to save memory and to speed up the search. Searching for this after the search is done to
 * create paths is ok, since the number of paths are a very small number compared to stop-arrivals
 * during the search.
 */
public class TripTimesSearch<T extends RaptorTripSchedule> {
    private final T schedule;
    private final int fromStop;
    private final int toStop;

    private TripTimesSearch(T schedule, int fromStop, int toStop) {
        this.schedule = schedule;
        this.fromStop = fromStop;
        this.toStop = toStop;
    }

    /**
     * Search for board- and alight-times for the trip matching the given trip-schedule,
     * from-/to-stop AFTER the given earliest-departure-time. This is safe to use even
     * for trips that has been time-shifted BACKWARD in time, but not if the trip is running in a
     * loop and manage to do a hole loop within the time-shift.
     */
    public static <S extends RaptorTripSchedule> Result searchAfterEDT(
            S schedule, int fromStop, int toStop, int earliestDepartureTime
    ) {
        return new TripTimesSearch<>(schedule, fromStop, toStop).searchAfterEDT(earliestDepartureTime);
    }

    /**
     * Search for board- and alight-times for the trip matching the given trip-schedule,
     * from-/to-stop BEFORE the given latest-arrival-time. This is safe to use even
     * for trips that has been time-shifted FORWARD in time, but not if the trip is running in a
     * loop and manage to do a hole loop within the time-shift.
     */
    public static <S extends RaptorTripSchedule> Result searchBeforeLAT(
            S schedule, int fromStop, int toStop, int latestArrivalTime
    ) {
        return new TripTimesSearch<>(schedule, fromStop, toStop).searchBeforeLAT(latestArrivalTime);
    }

    private Result searchAfterEDT(int earliestDepartureTime) {
        RaptorTripPattern p = schedule.pattern();
        final int size = p.numberOfStopsInPattern();
        int i = 0;

        // Search for departure
        while (i < size && schedule.departure(i) < earliestDepartureTime) { ++i; }

        if(i == size) {
            throw noFoundException(
                    "No departures after earliest-departure-time", "EDT", earliestDepartureTime
            );
        }

        while (i < size && p.stopIndex(i) != fromStop) { ++i; }

        if(i == size) {
            throw noFoundException("No stops matching 'fromStop'", "EDT", earliestDepartureTime);
        }

        int departureTime = schedule.departure(i);

        // Goto next stop, boarding and alighting can not happen on the same stop
        ++i;

        // Search for arrival
        while (i < size && p.stopIndex(i) != toStop) {
            ++i;
        }

        if(i == size) {
            throw noFoundException("No stops matching 'toStop'", "EDT", earliestDepartureTime);
        }

        int arrivalTime = schedule.arrival(i);

        return new Result(departureTime, arrivalTime);
    }

    private Result searchBeforeLAT(int latestArrivalTime) {
        RaptorTripPattern p = schedule.pattern();
        final int size = p.numberOfStopsInPattern();
        int i = size-1;

        // Search for arrival
        while (i >= 0 && schedule.arrival(i) > latestArrivalTime) {
            --i;
        }

        if(i < 0) {
            throw noFoundException(
                    "No arrivals before latest-arrival-time", "LAT", latestArrivalTime
            );
        }

        while (i >= 0 && p.stopIndex(i) != toStop) { --i; }

        if(i < 0) {
            throw noFoundException("No stops matching 'toStop'", "LAT", latestArrivalTime);
        }

        int arrivalTime = schedule.arrival(i);

        // Goto next stop, boarding and alighting can not happen on the same stop
        --i;

        // Search for depature
        while (i >= 0 && p.stopIndex(i) != fromStop) {
            --i;
        }

        if(i < 0) {
            throw noFoundException("No stops matching 'fromStop'", "LAT", latestArrivalTime);
        }

        int departureTime = schedule.departure(i);

        return new Result(departureTime, arrivalTime);
    }

    private IllegalStateException noFoundException(String hint, String lbl, int time) {
        return new IllegalStateException(
                "Trip not found: " + hint + ". "
                        + " [FromStop: " + fromStop
                        + ", toStop: " + toStop
                        + ", " + lbl + ": " + TimeUtils.timeToStrLong(time)
                        + ", schedule: " + schedule.debugInfo() + "]"
        );
    }

    public static class Result {
        public final int boardTime;
        public final int alightTime;

        private Result(int boardTime, int alightTime) {
            this.boardTime = boardTime;
            this.alightTime = alightTime;
        }

        @Override
        public String toString() {
            return ToStringBuilder.of(Result.class)
                    .addServiceTime("boardTime", boardTime, -1)
                    .addServiceTime("alightTime", alightTime, -1)
                    .toString();
        }
    }
}
