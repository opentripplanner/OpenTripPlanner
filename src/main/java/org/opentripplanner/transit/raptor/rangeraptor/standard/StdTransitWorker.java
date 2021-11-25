package org.opentripplanner.transit.raptor.rangeraptor.standard;

import java.util.function.IntConsumer;
import java.util.function.ToIntFunction;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripScheduleBoardOrAlightEvent;
import org.opentripplanner.transit.raptor.api.transit.TransitArrival;
import org.opentripplanner.transit.raptor.rangeraptor.RoutingStrategy;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;


/**
 * The purpose of this class is to implement the "Standard" specific functionality of the worker.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class StdTransitWorker<T extends RaptorTripSchedule> implements RoutingStrategy<T> {

    private static final int NOT_SET = -1;

    private final TransitCalculator<T> calculator;
    private final StdWorkerState<T> state;

    private int onTripIndex;
    private int onTripBoardTime;
    private int onTripBoardStop;
    private T onTrip;

    public StdTransitWorker(TransitCalculator<T> calculator, StdWorkerState<T> state) {
        this.calculator = calculator;
        this.state = state;
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
    public int onTripIndex() {
        return onTripIndex;
    }

    @Override
    public void prepareForTransitWith(RaptorTripPattern pattern) {
        this.onTripIndex = NOT_SET;
        this.onTripBoardTime = NOT_SET;
        this.onTripBoardStop = NOT_SET;
        this.onTrip = null;
    }

    @Override
    public void alight(final int stopIndex, final int stopPos, final int alightSlack) {
        if (onTripIndex != NOT_SET) {
            final int stopArrivalTime = calculator.stopArrivalTime(onTrip, stopPos, alightSlack);
            state.transitToStop(stopIndex, stopArrivalTime, onTripBoardStop, onTripBoardTime, onTrip);
        }
    }

    @Override
    public void forEachBoarding(int stopIndex, IntConsumer prevStopArrivalTimeConsumer) {
        // Don't attempt to board if this stop was not reached in the last round.
        // Allow to reboard the same pattern - a pattern may loop and visit the same stop twice
        if (state.isStopReachedInPreviousRound(stopIndex)) {
            prevStopArrivalTimeConsumer.accept(state.bestTimePreviousRound(stopIndex));
        }
    }

    @Override
    public void board(
            int stopIndex,
            final int earliestBoardTime,
            RaptorTripScheduleBoardOrAlightEvent<T> result
    ) {
        onTripIndex = result.getTripIndex();
        onTrip = result.getTrip();
        onTripBoardTime = result.getTime();
        onTripBoardStop = stopIndex;
    }

    @Override
    public TransitArrival<T> previousTransit(int boardStopIndex) {
        return state.previousTransit(boardStopIndex);
    }
}
