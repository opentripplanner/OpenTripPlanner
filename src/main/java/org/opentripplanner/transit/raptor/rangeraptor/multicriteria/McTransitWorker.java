package org.opentripplanner.transit.raptor.rangeraptor.multicriteria;

import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.RoutingStrategy;
import org.opentripplanner.transit.raptor.rangeraptor.SlackProvider;
import org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals.AbstractStopArrival;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TripScheduleSearch;
import org.opentripplanner.transit.raptor.util.paretoset.ParetoSet;


/**
 * The purpose of this class is to implement the multi-criteria specific functionality of
 * the worker.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class McTransitWorker<T extends RaptorTripSchedule> implements RoutingStrategy<T> {

    private final McRangeRaptorWorkerState<T> state;
    private final TransitCalculator calculator;
    private final CostCalculator costCalculator;
    private final SlackProvider slackProvider;
    private final ParetoSet<Boarding<T>> patternBoardings = new ParetoSet<>(Boarding.paretoComparatorRelativeCost());

    private RaptorTripPattern pattern;
    private TripScheduleSearch<T> tripSearch;

    public McTransitWorker(
        McRangeRaptorWorkerState<T> state,
        SlackProvider slackProvider,
        TransitCalculator calculator,
        CostCalculator costCalculator
    ) {
        this.state = state;
        this.slackProvider = slackProvider;
        this.calculator = calculator;
        this.costCalculator = costCalculator;
    }

    @Override
    public void prepareForTransitWith(RaptorTripPattern pattern, TripScheduleSearch<T> tripSearch) {
        this.pattern = pattern;
        this.tripSearch = tripSearch;
        this.patternBoardings.clear();
        slackProvider.setCurrentPattern(pattern);
    }

    @Override
    public void routeTransitAtStop(int stopPos) {
        final int stopIndex = pattern.stopIndex(stopPos);

        // Alight at boardStopPos
        if (pattern.alightingPossibleAt(stopPos)) {
            for (Boarding<T> boarding : patternBoardings) {
                state.transitToStop(
                    boarding,
                    stopIndex,
                    boarding.trip.arrival(stopPos),
                    slackProvider.alightSlack()
                );
            }
        }

        // If it is not possible to board the pattern at this stop, then return
        if(!pattern.boardingPossibleAt(stopPos)) {
            return;
        }

        // For each arrival at the current stop
        for (AbstractStopArrival<T> prevArrival : state.listStopArrivalsPreviousRound(stopIndex)) {

            int earliestBoardTime = calculator.plusDuration(
                prevArrival.arrivalTime(),
                slackProvider.boardSlack()
            );

            boolean found = tripSearch.search(earliestBoardTime, stopPos);

            if (found) {
                final T trip = tripSearch.getCandidateTrip();
                final int boardTime = trip.departure(stopPos);

                // It the previous leg can
                if(prevArrival.arrivedByAccessLeg()) {
                    prevArrival = prevArrival.timeShiftNewArrivalTime(boardTime - slackProvider.boardSlack());
                }

                final int boardWaitTime = boardTime - prevArrival.arrivalTime();

                // We want to find the "relative-transit-time" spent onboard so we can add it to the
                // cost when comparing two trips A and B. Let A board at stop S at 10:00 and B
                // board at stop T at 10:05. Than, clearly it does not matter when we alight, Trip A
                // is going to spend 5 minutes more in transit than trip when alighting at the same
                // stop. Hence we can use the negative board-time as the "relative-transit-time".
                final int relativeTransitTime =  - boardTime;
                final int relativeBoardCost = calculateOnTripRelativeCost(
                    prevArrival,
                    relativeTransitTime,
                    boardWaitTime
                );

                patternBoardings.add(
                    new Boarding<>(
                        prevArrival,
                        stopIndex,
                        stopPos,
                        boardTime,
                        boardWaitTime,
                        relativeBoardCost,
                        trip,
                        tripSearch.getCandidateTripIndex()
                    )
                );
            }
        }
    }

    /**
     * Calculate a cost for riding a trip. It should include the cost from the beginning of the
     * journey all the way until a trip is boarded. The cost is used to compare trips boarding
     * the same pattern with the same number of transfers. It is ok for the cost to be relative
     * to any point in place or time - as long as it can be used to compare to paths that started
     * at the origin in the same iteration, having used the same number-of-rounds to board the same
     * trip.
     *
     * @param prevArrival The stop-arrival where the trip was boarded.
     * @param boardWaitTime the wait-time at the board stop before boarding.
     * @param relativeTransitTime The time spent on transit. We do not know the alight-stop,
     *                            so it is impossible to know the correct result. But the only thing
     *                            that maters is that the relative difference between to boardings
     *                            are correct.
     */
    private int calculateOnTripRelativeCost(
        AbstractStopArrival<T> prevArrival,
        int relativeTransitTime,
        int boardWaitTime
    ) {
        return costCalculator.onTripRidingCost(
            prevArrival.cost(),
            boardWaitTime,
            relativeTransitTime,
            prevArrival.stop()
        );
    }

    @Override
    public void setInitialTimeForIteration(RaptorTransfer it, int iterationDepartureTime) {
        // Earliest possible departure time from the origin, or latest possible arrival time at the
        // destination if searching backwards, using this AccessEgress.
        int departureTime = calculator.departureTime(it, iterationDepartureTime);

        // This access is not available after the iteration departure time
        if (departureTime == -1) { return; }

        state.setInitialTimeForIteration(it, departureTime);
    }
}
