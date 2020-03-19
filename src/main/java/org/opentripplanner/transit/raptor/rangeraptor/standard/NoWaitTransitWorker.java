package org.opentripplanner.transit.raptor.rangeraptor.standard;

import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.TransitRoutingStrategy;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TripScheduleSearch;


/**
 * The purpose of this class is to implement the "Standard" specific functionality of the worker with
 * NO WAIT TIME between transfer and transit, except the boardSlack.
 * <p/>
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class NoWaitTransitWorker<T extends RaptorTripSchedule> implements TransitRoutingStrategy<T> {

    private static final int NOT_SET = -1;

    private final StdWorkerState<T> state;
    private final TransitCalculator calculator;

    private int onTripIndex;
    private int onTripBoardTime;
    private int onTripBoardStop;
    private T onTrip;
    private int onTripTimeShift;
    private RaptorTripPattern pattern;
    private TripScheduleSearch<T> tripSearch;

    public NoWaitTransitWorker(
            StdWorkerState<T> state,
            TransitCalculator calculator
    ) {
        this.state = state;
        this.calculator = calculator;
    }

    @Override
    public void prepareForTransitWith(RaptorTripPattern pattern, TripScheduleSearch<T> tripSearch) {
        this.pattern = pattern;
        this.tripSearch = tripSearch;
        this.onTripIndex = NOT_SET;
        this.onTripBoardTime = NOT_SET;
        this.onTripBoardStop = NOT_SET;
        this.onTrip = null;
        this.onTripTimeShift = NOT_SET;
    }

    @Override
    public void routeTransitAtStop(int stopPositionInPattern) {
        int stop = pattern.stopIndex(stopPositionInPattern);

        // attempt to alight if we're on board, done above the board search so that we don't check for alighting
        // when boarding
        if (onTripIndex != NOT_SET) {
            state.transitToStop(
                    stop,
                    stopArrivalTime(onTrip, stopPositionInPattern),
                    onTripBoardStop,
                    onTripBoardTime,
                    onTrip
            );
        }

        // Don't attempt to board if this stop was not reached in the last round.
        // Allow to reboard the same pattern - a pattern may loop and visit the same stop twice
        if (state.isStopReachedInPreviousRound(stop)) {
            int earliestBoardTime = calculator.earliestBoardTime(state.bestTimePreviousRound(stop));

            // check if we can back up to an earlier trip due to this stop being reached earlier
            boolean found = tripSearch.search(earliestBoardTime, stopPositionInPattern, onTripIndex);

            if (found) {
                onTripIndex = tripSearch.getCandidateTripIndex();
                onTrip = tripSearch.getCandidateTrip();
                onTripBoardTime = earliestBoardTime;
                onTripBoardStop = stop;
                onTripTimeShift = tripSearch.getCandidateTripTime() - earliestBoardTime;
            }
        }
    }

    public int stopArrivalTime(final T trip, final int stopPositionInPattern) {
        // In the normal case the arrivalTime is used, but in reverse search
        // the board slack is added; hence the calculator delegation
        return calculator.stopArrivalTime(trip, stopPositionInPattern) - onTripTimeShift;
    }
}
