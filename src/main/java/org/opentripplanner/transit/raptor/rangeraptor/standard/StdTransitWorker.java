package org.opentripplanner.transit.raptor.rangeraptor.standard;

import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.SlackProvider;
import org.opentripplanner.transit.raptor.rangeraptor.TransitRoutingStrategy;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TripScheduleSearch;


/**
 * The purpose of this class is to implement the "Standard" specific functionality of the worker.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class StdTransitWorker<T extends RaptorTripSchedule> implements TransitRoutingStrategy<T> {

    private static final int NOT_SET = -1;

    private final StdWorkerState<T> state;
    private final TransitCalculator calculator;
    private final SlackProvider slackProvider;

    private int onTripIndex;
    private int onTripBoardTime;
    private int onTripBoardStop;
    private T onTrip;
    private RaptorTripPattern pattern;
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
    public void prepareForTransitWith(RaptorTripPattern pattern, TripScheduleSearch<T> tripSearch) {
        this.pattern = pattern;
        this.tripSearch = tripSearch;
        this.onTripIndex = NOT_SET;
        this.onTripBoardTime = 0;
        this.onTripBoardStop = -1;
        this.onTrip = null;
        this.slackProvider.setCurrentPattern(pattern);
    }

    @Override
    public void routeTransitAtStop(int stopPositionInPattern) {
        int stop = pattern.stopIndex(stopPositionInPattern);

        // attempt to alight if we're on board, done above the board search so that we don't check
        // for alighting when boarding
        if (onTripIndex != NOT_SET) {
            // TODO OTP2 - Are We allowed to alight at stop?
            state.transitToStop(
                    stop,
                    // In the normal case the trip alightTime is used,
                    // but in reverse search the board-slack is added; hence the calculator delegation
                    calculator.stopArrivalTime(
                            onTrip,
                            stopPositionInPattern,
                            slackProvider.alightSlack()
                    ),
                    onTripBoardStop,
                    onTripBoardTime,
                    onTrip
            );
        }

        // Don't attempt to board if this stop was not reached in the last round.
        // Allow to reboard the same pattern - a pattern may loop and visit the same stop twice
        if (state.isStopReachedInPreviousRound(stop)) {

            // Calculate the earliest possible board time, adding board slack (alight slack if in
            // reverse). The slackProvider takes care of picking the correct board/alight slack.
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
}
