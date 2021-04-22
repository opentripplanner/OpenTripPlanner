package org.opentripplanner.transit.raptor.rangeraptor.transit;


import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.util.time.TimeUtils;

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
    public static <S extends RaptorTripSchedule> BoarAndAlightTime findTripForwardSearch(
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
    public static <S extends RaptorTripSchedule> BoarAndAlightTime findTripReverseSearch(
        ArrivalView<S> arrival
    ) {
        S trip = arrival.transitPath().trip();
        int fromStop = arrival.stop();
        int toStop = arrival.previous().stop();
        int earliestBoardTime = arrival.arrivalTime();

        return new TripTimesSearch<>(trip, fromStop, toStop).findTripAfter(earliestBoardTime);
    }

    /**
     * Search for board- and alight-times for the trip matching the given stop-arrival
     * when searching FORWARD. Hence, searching in the same direction as the trip travel
     * direction.
     */
    public static <S extends RaptorTripSchedule> BoarAndAlightTime findTripTimes(TransitPathLeg<S> leg) {
        return new TripTimesSearch<>(leg.trip(), leg.fromStop(), leg.toStop())
            .findTripBefore(leg.toTime());
    }

    /* private methods */

    private BoarAndAlightTime findTripAfter(final int earliestDepartureTime) {
        RaptorTripPattern p = schedule.pattern();
        final int size = p.numberOfStopsInPattern();

        int i = schedule.findDepartureStopPosition(earliestDepartureTime, fromStop);

        if(i < 0) {
            throw notFoundException(
                "No stops matching 'fromStop'", "earliestDepartureTime", earliestDepartureTime
            );
        }

        int boardStopPos = i;

        // Goto next stop, boarding and alighting can not happen on the same stop
        ++i;

        // Search for arrival
        while (i < size && p.stopIndex(i) != toStop) {
            ++i;
        }

        if(i == size) {
            throw notFoundException(
                "No stops matching 'toStop'",
                "earliestDepartureTime",
                earliestDepartureTime
            );
        }
        return new BoarAndAlightTime(schedule, boardStopPos, i);
    }

    private BoarAndAlightTime findTripBefore(int latestArrivalTime) {
        RaptorTripPattern p = schedule.pattern();
        int i = schedule.findArrivalStopPosition(latestArrivalTime, toStop);

        if(i < 0) {
            throw notFoundException(
                "No stops matching 'toStop'", "latestArrivalTime", latestArrivalTime
            );
        }

        int alightStopPos = i;

        // Goto next stop, boarding and alighting can not happen on the same stop
        --i;

        // Search for departure
        while (i >= 0 && p.stopIndex(i) != fromStop) {
            --i;
        }

        if(i < 0) {
            throw notFoundException(
                    "No stops matching 'fromStop'",
                    "latestArrivalTime",
                    latestArrivalTime
            );
        }
        return new BoarAndAlightTime(schedule, i, alightStopPos);
    }

    private IllegalStateException notFoundException(String hint, String lbl, int time) {
        return new IllegalStateException(
                "Trip not found: " + hint + ". "
                        + " [FromStop: " + fromStop
                        + ", toStop: " + toStop
                        + ", " + lbl + ": " + TimeUtils.timeToStrLong(time)
                        + ", pattern: " + schedule.pattern().debugInfo() + "]"
        );
    }
}
