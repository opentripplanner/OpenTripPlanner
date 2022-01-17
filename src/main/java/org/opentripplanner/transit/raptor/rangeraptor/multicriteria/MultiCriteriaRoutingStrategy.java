package org.opentripplanner.transit.raptor.rangeraptor.multicriteria;

import static org.opentripplanner.transit.raptor.rangeraptor.multicriteria.PatternRide.paretoComparatorRelativeCost;

import java.util.function.IntConsumer;
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
public final class MultiCriteriaRoutingStrategy<T extends RaptorTripSchedule>
        implements RoutingStrategy<T> {

    private final McRangeRaptorWorkerState<T> state;
    private final CostCalculator costCalculator;
    private final SlackProvider slackProvider;
    private final ParetoSet<PatternRide<T>> patternRides;

    private AbstractStopArrival<T> prevArrival;

    public MultiCriteriaRoutingStrategy(
        McRangeRaptorWorkerState<T> state,
        SlackProvider slackProvider,
        CostCalculator costCalculator,
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
    }

    @Override
    public void alight(final int stopIndex, final int stopPos, int alightSlack) {
        for (PatternRide<T> ride : patternRides) {
            state.transitToStop(
                    ride,
                    stopIndex,
                    ride.trip.arrival(stopPos),
                    alightSlack
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
    public TransitArrival<T> previousTransit(int boardStopIndex) {
        return prevArrival.mostResentTransitArrival();
    }

    @Override
    public void board(
            final int stopIndex,
            final int earliestBoardTime,
            final RaptorTripScheduleBoardOrAlightEvent<T> boarding
    ) {
        final T trip = boarding.getTrip();
        final int boardTime = boarding.getTime();

        if(prevArrival.arrivedByAccess()) {
            // TODO: What if access is FLEX with rides, should not FLEX transfersSlack be taken
            //       into account as well?
            int latestArrivalTime = boardTime - slackProvider.boardSlack(trip.pattern());
            prevArrival = prevArrival.timeShiftNewArrivalTime(latestArrivalTime);
        }

        final int boardCost = calculateCostAtBoardTime(prevArrival, boarding);

        final int relativeBoardCost = boardCost +
                calculateOnTripRelativeCost(trip.transitReluctanceFactorIndex(), boardTime);

        patternRides.add(
            new PatternRide<>(
                prevArrival,
                stopIndex,
                boarding.getStopPositionInPattern(),
                boardTime,
                boardCost,
                relativeBoardCost,
                trip
            )
        );
    }


    /**
     * Calculate a cost for riding a trip. It should include the cost from the beginning of the
     * journey all the way until a trip is boarded. Any slack at the end of the last leg is not
     * part of this, because that is already accounted for. If the previous leg is an access leg,
     * then it is already time-shifted, which is important for this calculation to be correct.
     *
     * @param prevArrival The stop-arrival where the trip was boarded.
     */
    private int calculateCostAtBoardTime(
            final AbstractStopArrival<T> prevArrival,
            final RaptorTripScheduleBoardOrAlightEvent<T> boardEvent
    ) {
        return prevArrival.cost() + costCalculator.boardingCost(
                prevArrival.isFirstRound(),
                prevArrival.arrivalTime(),
                boardEvent.getBoardStopIndex(),
                boardEvent.getTime(),
                boardEvent.getTrip(),
                boardEvent.getTransferConstraint()
        );
    }

    /**
     * Calculate a cost for riding a trip. It should include the cost from the beginning of the
     * journey all the way until a trip is boarded. The cost is used to compare trips boarding
     * the same pattern with the same number of transfers. It is ok for the cost to be relative
     * to any point in place or time - as long as it can be used to compare to paths that started
     * at the origin in the same iteration, having used the same number-of-rounds to board the same
     * trip.
     */
    private int calculateOnTripRelativeCost(int transitReluctanceIndex, int boardTime) {
        return costCalculator.onTripRelativeRidingCost(boardTime, transitReluctanceIndex);
    }
}
