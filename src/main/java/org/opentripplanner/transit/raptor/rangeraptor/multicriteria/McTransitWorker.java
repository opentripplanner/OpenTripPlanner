package org.opentripplanner.transit.raptor.rangeraptor.multicriteria;

import static org.opentripplanner.transit.raptor.rangeraptor.multicriteria.PatternRide.paretoComparatorRelativeCost;

import java.util.function.IntConsumer;
import java.util.function.ToIntFunction;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripScheduleBoardOrAlightEvent;
import org.opentripplanner.transit.raptor.api.transit.TransitArrival;
import org.opentripplanner.transit.raptor.rangeraptor.RoutingStrategy;
import org.opentripplanner.transit.raptor.rangeraptor.SlackProvider;
import org.opentripplanner.transit.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals.AbstractStopArrival;
import org.opentripplanner.transit.raptor.util.paretoset.ParetoSet;


/**
 * The purpose of this class is to implement the multi-criteria specific functionality of
 * the worker.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class McTransitWorker<T extends RaptorTripSchedule> implements RoutingStrategy<T> {

    private final McRangeRaptorWorkerState<T> state;
    private final CostCalculator<T> costCalculator;
    private final SlackProvider slackProvider;
    private final ParetoSet<PatternRide<T>> patternRides;

    private AbstractStopArrival<T> prevArrival;

    public McTransitWorker(
        McRangeRaptorWorkerState<T> state,
        SlackProvider slackProvider,
        CostCalculator<T> costCalculator,
        DebugHandlerFactory<T> debugHandlerFactory
    ) {
        this.state = state;
        this.slackProvider = slackProvider;
        this.costCalculator = costCalculator;
        this.patternRides = new ParetoSet<>(
            paretoComparatorRelativeCost(),
            debugHandlerFactory.paretoSetPatternRideListener()
        );
    }

    @Override
    public void setAccessToStop(
        RaptorTransfer accessPath,
        int iterationDepartureTime,
        int timeDependentDepartureTime
    ) {
        state.setAccessToStop(accessPath, timeDependentDepartureTime);
    }

    @Override
    public void prepareForTransitWith(RaptorTripPattern pattern) {
        this.patternRides.clear();
        this.slackProvider.setCurrentPattern(pattern);
    }

    @Override
    public void alight(final int stopIndex, final int stopPos, ToIntFunction<T> stopArrivalTimeOp) {
        for (PatternRide<T> ride : patternRides) {
            state.transitToStop(
                    ride,
                    stopIndex,
                    ride.trip.arrival(stopPos),
                    slackProvider.alightSlack()
            );
        }
    }

    @Override
    public void forEachBoarding(int stopIndex, IntConsumer prevStopArrivalTimeConsumer) {
        for (AbstractStopArrival<T> prevArrival : state.listStopArrivalsPreviousRound(stopIndex)) {
            this.prevArrival = prevArrival;
            prevStopArrivalTimeConsumer.accept(prevArrival.arrivalTime());
        }
    }

    @Override
    public void board(
            final int stopIndex,
            final int earliestBoardTime,
            final RaptorTripScheduleBoardOrAlightEvent<T> result
    ) {
        final T trip = result.getTrip();
        final int boardTime = result.getTime();

        if(prevArrival.arrivedByAccess()) {
            // What if access is FLEX with rides, should not FLEX alightSlack and
            // transfersSlack be taken into account as well?
            prevArrival = prevArrival.timeShiftNewArrivalTime(boardTime - slackProvider.boardSlack());
        }

        final int boardWaitTimeForCostCalculation = boardTime - prevArrival.arrivalTime();
        final int relativeBoardCost = calculateOnTripRelativeCost(
            prevArrival,
            trip.transitReluctanceFactorIndex(),
            boardTime,
            boardWaitTimeForCostCalculation
        );

        patternRides.add(
            new PatternRide<>(
                prevArrival,
                stopIndex,
                result.getStopPositionInPattern(),
                boardTime,
                boardWaitTimeForCostCalculation,
                relativeBoardCost,
                trip
            )
        );
    }

    @Override
    public TransitArrival<T> previousTransit(int boardStopIndex) {
        return prevArrival.mostResentTransitArrival();
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
     */
    private int calculateOnTripRelativeCost(
        AbstractStopArrival<T> prevArrival,
        int transitReluctanceIndex,
        int boardTime,
        int boardWaitTime
    ) {
        return costCalculator.onTripRidingCost(
                prevArrival,
                boardWaitTime,
                boardTime,
                transitReluctanceIndex
        );
    }
}
