package org.opentripplanner.transit.raptor.rangeraptor.standard;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.SlackProvider;
import org.opentripplanner.transit.raptor.rangeraptor.RoutingStrategy;
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
    public void setAccessToStop(
        RaptorTransfer accessPath,
        int iterationDepartureTime,
        int timeDependentDepartureTime
    ) {
        state.setAccessToStop(accessPath, timeDependentDepartureTime);
    }

    @Override
    public void prepareForTransitWith(RaptorTripPattern pattern, TripScheduleSearch<T> tripSearch) {
        this.pattern = pattern;
        this.tripSearch = tripSearch;
        this.onTripIndex = NOT_SET;
        this.onTripBoardTime = NOT_SET;
        this.onTripBoardStop = NOT_SET;
        this.onTrip = null;
        this.slackProvider.setCurrentPattern(pattern);
    }

    @Override
    public void routeTransitAtStop(int stopPositionInPattern) {
        int stop = pattern.stopIndex(stopPositionInPattern);

        // attempt to alight if we're on board, done above the board search so that we don't check
        // for alighting when boarding
        if (onTripIndex != NOT_SET) {
            if (pattern.alightingPossibleAt(stopPositionInPattern)) {
                final int alightTime = alightTime(onTrip, stopPositionInPattern);
                state.transitToStop(stop, alightTime, onTripBoardStop, onTripBoardTime, onTrip);
            }
        }

        // Don't attempt to board if this stop was not reached in the last round.
        // Allow to reboard the same pattern - a pattern may loop and visit the same stop twice
        if (state.isStopReachedInPreviousRound(stop)) {
            if (pattern.boardingPossibleAt(stopPositionInPattern)) {
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
    }

    private int alightTime(final T trip, final int stopPositionInPattern) {
        // Trip alightTime + alight-slack(forward-search) or board-slack(reverse-search)
        return calculator.stopArrivalTime(
            trip,
            stopPositionInPattern,
            slackProvider.alightSlack()
        );
    }
}
