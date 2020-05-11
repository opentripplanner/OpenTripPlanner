package org.opentripplanner.transit.raptor.rangeraptor.standard;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.rangeraptor.SlackProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.RoutingStrategy;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TripScheduleSearch;


/**
 * The purpose of this class is to implement the "Standard" specific functionality of the worker
 * with NO WAIT TIME between transfer and transit, except the boardSlack.
 * <p/>
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class NoWaitTransitWorker<T extends RaptorTripSchedule> implements RoutingStrategy<T> {

    private static final int NOT_SET = -1;

    private final StdWorkerState<T> state;
    private final TransitCalculator calculator;
    private final SlackProvider slackProvider;

    private int onTripIndex;
    private int onTripBoardTime;
    private int onTripBoardStop;
    private T onTrip;
    private int onTripTimeShift;
    private RaptorTripPattern pattern;
    private TripScheduleSearch<T> tripSearch;

    public NoWaitTransitWorker(
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
        this.onTripBoardTime = NOT_SET;
        this.onTripBoardStop = NOT_SET;
        this.onTrip = null;
        this.onTripTimeShift = NOT_SET;
        slackProvider.setCurrentPattern(pattern);
    }

    @Override
    public void routeTransitAtStop(int stopPositionInPattern) {
        int stop = pattern.stopIndex(stopPositionInPattern);

        // attempt to alight if we're on board, done above the board search so that we don't check for alighting
        // when boarding
        if (onTripIndex != NOT_SET) {
            if (pattern.alightingPossibleAt(stopPositionInPattern)) {
                state.transitToStop(
                        stop,
                        stopArrivalTime(onTrip, stopPositionInPattern),
                        onTripBoardStop,
                        onTripBoardTime,
                        onTrip
                );
            }
        }

        // Don't attempt to board if this stop was not reached in the last round.
        // Allow to reboard the same pattern - a pattern may loop and visit the same stop twice
        if (state.isStopReachedInPreviousRound(stop)) {
            if (pattern.boardingPossibleAt(stopPositionInPattern)) {
                int earliestBoardTime = calculator.plusDuration(state.bestTimePreviousRound(stop),
                        slackProvider.boardSlack()
                );

                // check if we can back up to an earlier trip due to this stop being reached earlier
                boolean found = tripSearch.search(earliestBoardTime,
                        stopPositionInPattern,
                        onTripIndex
                );

                if (found) {
                    onTripIndex = tripSearch.getCandidateTripIndex();
                    onTrip = tripSearch.getCandidateTrip();
                    onTripBoardTime = earliestBoardTime;
                    onTripBoardStop = stop;
                    // Calculate the time-shift, the time-shift will be a positive duration in a
                    // forward-search, and a negative value in case of a reverse-search.
                    onTripTimeShift = tripSearch.getCandidateTripTime() - earliestBoardTime;
                }
            }
        }
    }

    public int stopArrivalTime(final T trip, final int stopPositionInPattern) {
        int stopArrivalTime = calculator.stopArrivalTime(
                trip,
                stopPositionInPattern,
                slackProvider.alightSlack()
        );
        // Remove the wait time from the arrival-time. We don´t need to use the transit
        // calculator because of the way we compute the time-shift. It is positive in the case of a
        // forward-search and negative int he case of a reverse-search.
        return stopArrivalTime - onTripTimeShift;
    }

    @Override
    public void setInitialTimeForIteration(RaptorTransfer it, int iterationDepartureTime) {
        // Earliest possible departure time from the origin, or latest possible arrival time at the
        // destination if searching backwards, using this AccessEgress.
        int departureTime = calculator.departureTime(it, iterationDepartureTime);

        // This access is not available after the iteration departure time
        if (departureTime == -1) return;

        // Pass in the original departure time, as wait time should not be included
        state.setInitialTimeForIteration(it, iterationDepartureTime);
    }
}
