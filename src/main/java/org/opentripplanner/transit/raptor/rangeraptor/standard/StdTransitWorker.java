package org.opentripplanner.transit.raptor.rangeraptor.standard;

import java.util.function.IntConsumer;
import java.util.function.ToIntFunction;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.RoutingStrategy;
import org.opentripplanner.transit.raptor.rangeraptor.SlackProvider;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TripScheduleSearch;


/**
 * The purpose of this class is to implement the "Standard" specific functionality of the worker.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class StdTransitWorker<T extends RaptorTripSchedule> implements RoutingStrategy<T> {

    private static final int NOT_SET = -1;

    private final StdWorkerState<T> state;
    private final TransitCalculator calculator;
    private final SlackProvider slackProvider;

    private int onTripIndex;
    private int onTripBoardTime;
    private int onTripBoardStop;
    private T onTrip;
    private TripScheduleSearch<T> tripSearch;

    public StdTransitWorker(
            StdWorkerState<T> state,
            SlackProvider slackProvider,
            TransitCalculator calculator
    ) {
        this.state = state;
        this.slackProvider = slackProvider;
        this.calculator = calculator;
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
    public void prepareForTransitWith(RaptorTripPattern pattern, TripScheduleSearch<T> tripSearch) {
        this.tripSearch = tripSearch;
        this.onTripIndex = NOT_SET;
        this.onTripBoardTime = NOT_SET;
        this.onTripBoardStop = NOT_SET;
        this.onTrip = null;
        this.slackProvider.setCurrentPattern(pattern);
    }

    @Override
    public void alight(final int stopIndex, final int stopPos, ToIntFunction<T> stopArrivalTimeOp) {
        if (onTripIndex != NOT_SET) {
            final int stopArrivalTime = stopArrivalTimeOp.applyAsInt(onTrip);
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
    public void routeTransitAtStop(int stop, int stopPositionInPattern) {
        // Add board-slack(forward-search) or alight-slack(reverse-search)
        int earliestBoardTime = calculator.plusDuration(
            state.bestTimePreviousRound(stop),
            slackProvider.boardSlack()
        );

        // check if we can back up to an earlier trip due to this stop being reached earlier
        boolean found = tripSearch.search(
            earliestBoardTime,
            stopPositionInPattern,
            onTripIndex
        );

        if (found) {
            onTripIndex = tripSearch.getCandidateTripIndex();
            onTrip = tripSearch.getCandidateTrip();
            onTripBoardTime = tripSearch.getCandidateTripTime();
            onTripBoardStop = stop;
        }
    }
}
