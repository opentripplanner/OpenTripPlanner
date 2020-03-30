package org.opentripplanner.transit.raptor.rangeraptor.transit;


import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

/**
 * This class is used to find the board and alight time for a known trip,
 * where you now the board-stop, earliest-board-time, alight-stop and latest-alight-time.
 * <p>
 * This class is needed to find board and alight times for transfer legs when
 * mapping to stop-arrivals to paths. The board and alight times are not stored in the
 * stop-arrival state to save memory and to speed up the search. Searching for this
 * after the search is done to create paths is ok, since the number of paths are a very small
 * number compared to stop-arrivals during the search.
 */
public class TripTimesSearch<T extends RaptorTripSchedule> {
    private final T schedule;
    private final int fromStop;
    private final int earliestDepartureTime;
    private final int toStop;
    private final int latestArrivalTime;

    public TripTimesSearch(
            T schedule, int fromStop, int earliestDepartureTime, int toStop, int latestArrivalTime
    ) {
        this.schedule = schedule;
        this.fromStop = fromStop;
        this.earliestDepartureTime = earliestDepartureTime;
        this.toStop = toStop;
        this.latestArrivalTime = latestArrivalTime;
    }

    public static <S extends RaptorTripSchedule> Result getTripTimes(
            S schedule, int fromStop, int earliestDepartureTime, int toStop, int latestArrivalTime
    ) {
        return new TripTimesSearch<>(
                schedule, fromStop, earliestDepartureTime, toStop, latestArrivalTime
        ).get();
    }

    public Result get() {
        RaptorTripPattern p = schedule.pattern();
        final int size = p.numberOfStopsInPattern();
        int i = 0;

        // Search for departure
        while (i < size && schedule.departure(i) < earliestDepartureTime) { ++i; }

        if(i == size) { throw noFoundException("No departures after earliest-departure-time"); }

        while (i < size && p.stopIndex(i) != fromStop) { ++i; }

        if(i == size) { throw noFoundException("No stops matching fromStop"); }

        int departureTime = schedule.departure(i);

        // Goto next stop, boarding and alighting can not happen on the same stop
        ++i;

        // Search for arrival
        while (i < size && p.stopIndex(i) != toStop) {
            ++i;
        }
        if(i == size) { throw noFoundException("No stops matching toStop"); }
        if(schedule.arrival(i) > latestArrivalTime) {
            throw noFoundException("No arrivals before latest-arrival-time");
        }

        int arrivalTime = schedule.arrival(i);

        return new Result(departureTime, arrivalTime);
    }

    private IllegalStateException noFoundException(String hint) {
        return new IllegalStateException(
                "Trip not found: " + hint + ". "
                        + " [FromStop: " + fromStop
                        + ", toStop: " + toStop
                        + ", EDT: " + earliestDepartureTime
                        + ", LAT: " + latestArrivalTime
                        + ", schedule: " + schedule + "]"
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
