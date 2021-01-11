package org.opentripplanner.transit.raptor.rangeraptor.multicriteria;

import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.RoutingStrategy;
import org.opentripplanner.transit.raptor.rangeraptor.SlackProvider;
import org.opentripplanner.transit.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals.AbstractStopArrival;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TripScheduleSearch;
import org.opentripplanner.transit.raptor.util.paretoset.ParetoSet;

import static org.opentripplanner.transit.raptor.rangeraptor.multicriteria.PatternRide.paretoComparatorRelativeCost;


/**
 * The purpose of this class is to implement the multi-criteria specific functionality of
 * the worker.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class McTransitWorker<T extends RaptorTripSchedule> implements RoutingStrategy<T> {

    private final McRangeRaptorWorkerState<T> state;
    private final TransitCalculator calculator;
    private final CostCalculator<T> costCalculator;
    private final SlackProvider slackProvider;
    private final ParetoSet<PatternRide<T>> patternRides;

    private RaptorTripPattern pattern;
    private TripScheduleSearch<T> tripSearch;

    public McTransitWorker(
        McRangeRaptorWorkerState<T> state,
        SlackProvider slackProvider,
        TransitCalculator calculator,
        CostCalculator<T> costCalculator,
        DebugHandlerFactory<T> debugHandlerFactory
    ) {
        this.state = state;
        this.slackProvider = slackProvider;
        this.calculator = calculator;
        this.costCalculator = costCalculator;
        this.patternRides = new ParetoSet<>(
            paretoComparatorRelativeCost(),
            debugHandlerFactory.paretoSetPatternRideListener()
        );
    }

    @Override
    public void prepareForTransitWith(RaptorTripPattern pattern, TripScheduleSearch<T> tripSearch) {
        this.pattern = pattern;
        this.tripSearch = tripSearch;
        this.patternRides.clear();
        this.slackProvider.setCurrentPattern(pattern);
    }

    @Override
    public void routeTransitAtStop(int stopPos) {
        final int stopIndex = pattern.stopIndex(stopPos);

        // Alight at boardStopPos
        if (pattern.alightingPossibleAt(stopPos)) {
            for (PatternRide<T> ride : patternRides) {
                state.transitToStop(
                    ride,
                    stopIndex,
                    ride.trip.arrival(stopPos),
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

                if(prevArrival.arrivedByAccess()) {
                    prevArrival = prevArrival.timeShiftNewArrivalTime(boardTime - slackProvider.boardSlack());
                }

                // TODO OTP2 - Some access paths can be time-shifted towards the board time and
                //           - we need to account for this here, not in the calculator as done
                //           - now. If we don´t do that the alight slack of the first transit is
                //           - not added to the cost, giving the first transit path a lower cost
                //           - than other transit paths.
                final int boardWaitTime = boardTime - prevArrival.arrivalTime();

                final int relativeBoardCost = calculateOnTripRelativeCost(
                    prevArrival,
                    boardTime,
                    boardWaitTime,
                    trip
                );

                patternRides.add(
                    new PatternRide<>(
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
     * @param boardTime the wait-time at the board stop before boarding.
     * @param boardWaitTime the wait-time at the board stop before boarding.
     * @param trip boarded trip
     */
    private int calculateOnTripRelativeCost(
        AbstractStopArrival<T> prevArrival,
        int boardTime,
        int boardWaitTime,
        T trip
    ) {
        return costCalculator.onTripRidingCost(
            prevArrival,
            boardWaitTime,
            boardTime,
            trip
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
