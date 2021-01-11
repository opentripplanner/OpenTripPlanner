package org.opentripplanner.transit.raptor.rangeraptor.transit;


import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.util.TimeUtils;

/**
 * This class is used to find the board and alight time for a known trip, where you now the
 * board-stop and the alight-stop. You must also specify either a earliest-board-time or a
 * latest-alight-time - this is done to avoid boarding at the correct stop, but at the wrong time.
 * This can happen for patterns goes in a loop, visit the same stop more than once.
 * <p>
 * This class is used to find board- and alight-times for transfer paths when mapping stop-arrivals
 * to paths. The board and alight times are not stored in the stop-arrival state to save memory and
 * to speed up the search. Searching for this after the search is done to create paths is ok, since
 * the number of paths are a very small number compared to stop-arrivals during the search.
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
     * Search for board- and alight-times for the trip matching the given stop-arrival
     * when searching FORWARD. Hence, searching in the same direction as the trip travel
     * direction.
     */
    public static <S extends RaptorTripSchedule> BoarAlightTimes findTripForwardSearch(
        ArrivalView<S> arrival
    ) {
        S trip = arrival.transitPath().trip();
        int fromStop = arrival.previous().stop();
        int toStop = arrival.stop();
        int latestArrivalTime = arrival.arrivalTime();

        return new TripTimesSearch<>(trip, fromStop, toStop).findTripBefore(latestArrivalTime);
    }

    /**
     * Search for board- and alight-times for the trip matching the given stop-arrival
     * when searching in REVERSE. Hence, searching in the opposite direction of the trip
     * travel direction.
     */
    public static <S extends RaptorTripSchedule> BoarAlightTimes findTripReverseSearch(
        ArrivalView<S> arrival
    ) {
        S trip = arrival.transitPath().trip();
        int fromStop = arrival.stop();
        int toStop = arrival.previous().stop();
        int earliestBoardTime = arrival.arrivalTime();

        return new TripTimesSearch<>(trip, fromStop, toStop).findTripAfter(earliestBoardTime);
    }

    private BoarAlightTimes findTripAfter(int earliestDepartureTime) {
        RaptorTripPattern p = schedule.pattern();
        final int size = p.numberOfStopsInPattern();
        int i = 0;

        // Search for departure
        while (i < size && schedule.departure(i) < earliestDepartureTime) { ++i; }

        if(i == size) {
            throw noFoundException(
                    "No departures after 'earliestDepartureTime'",
                    "earliestDepartureTime",
                    earliestDepartureTime
            );
        }

        while (i < size && p.stopIndex(i) != fromStop) { ++i; }

        if(i == size) {
            throw noFoundException(
                "No stops matching 'fromStop'",
                "earliestDepartureTime",
                earliestDepartureTime
            );
        }

        int departureTime = schedule.departure(i);

        // Goto next stop, boarding and alighting can not happen on the same stop
        ++i;

        // Search for arrival
        while (i < size && p.stopIndex(i) != toStop) {
            ++i;
        }

        if(i == size) {
            throw noFoundException(
                "No stops matching 'toStop'",
                "earliestDepartureTime",
                earliestDepartureTime
            );
        }

        int arrivalTime = schedule.arrival(i);

        return new BoarAlightTimes(departureTime, arrivalTime);
    }

    private BoarAlightTimes findTripBefore(int latestArrivalTime) {
        RaptorTripPattern p = schedule.pattern();
        final int size = p.numberOfStopsInPattern();
        int i = size-1;

        // Search for arrival
        while (i >= 0 && schedule.arrival(i) > latestArrivalTime) {
            --i;
        }

        if(i < 0) {
            throw noFoundException(
                    "No arrivals before 'latestArrivalTime'",
                    "latestArrivalTime",
                    latestArrivalTime
            );
        }

        while (i >= 0 && p.stopIndex(i) != toStop) { --i; }

        if(i < 0) {
            throw noFoundException(
                    "No stops matching 'toStop'",
                    "latestArrivalTime",
                    latestArrivalTime
            );
        }

        int arrivalTime = schedule.arrival(i);

        // Goto next stop, boarding and alighting can not happen on the same stop
        --i;

        // Search for depature
        while (i >= 0 && p.stopIndex(i) != fromStop) {
            --i;
        }

        if(i < 0) {
            throw noFoundException(
                    "No stops matching 'fromStop'",
                    "latestArrivalTime",
                    latestArrivalTime
            );
        }

        int departureTime = schedule.departure(i);

        return new BoarAlightTimes(departureTime, arrivalTime);
    }

    private IllegalStateException noFoundException(String hint, String lbl, int time) {
        return new IllegalStateException(
                "Trip not found: " + hint + ". "
                        + " [FromStop: " + fromStop
                        + ", toStop: " + toStop
                        + ", " + lbl + ": " + TimeUtils.timeToStrLong(time)
                        + ", pattern: " + schedule.pattern().debugInfo() + "]"
        );
    }

    public static class BoarAlightTimes {
        public final int boardTime;
        public final int alightTime;

        private BoarAlightTimes(int boardTime, int alightTime) {
            this.boardTime = boardTime;
            this.alightTime = alightTime;
        }

        @Override
        public String toString() {
            return ToStringBuilder.of(BoarAlightTimes.class)
                    .addServiceTime("boardTime", boardTime, -1)
                    .addServiceTime("alightTime", alightTime, -1)
                    .toString();
        }
    }
}
