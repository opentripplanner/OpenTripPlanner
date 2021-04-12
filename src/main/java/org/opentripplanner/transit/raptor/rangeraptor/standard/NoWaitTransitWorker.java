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
    public void setAccessToStop(
        RaptorTransfer accessPath,
        int iterationDepartureTime,
        int timeDependentDepartureTime
    ) {
        // Pass in the original departure time, as wait time should not be included
        state.setAccessToStop(accessPath, iterationDepartureTime);
    }

    @Override
    public void prepareForTransitWith(RaptorTripPattern pattern, TripScheduleSearch<T> tripSearch) {
        this.tripSearch = tripSearch;
        this.onTripIndex = NOT_SET;
        this.onTripBoardTime = NOT_SET;
        this.onTripBoardStop = NOT_SET;
        this.onTrip = null;
        this.onTripTimeShift = NOT_SET;
        this.slackProvider.setCurrentPattern(pattern);
    }

    @Override
    public void alight(int stopIndex, int stopPos, ToIntFunction<T> getStopArrivalTime) {
        // attempt to alight if we're on board
        if (onTripIndex != NOT_SET) {
            // Trip alightTime + alight-slack(forward-search) or board-slack(reverse-search)
            final int stopArrivalTime0 = getStopArrivalTime.applyAsInt(onTrip);

            // Remove the wait time from the arrival-time. We don´t need to use the transit
            // calculator because of the way we compute the time-shift. It is positive in the case
            // of a forward-search and negative int he case of a reverse-search.
            final int stopArrivalTime = stopArrivalTime0 - onTripTimeShift;

            state.transitToStop(stopIndex, stopArrivalTime, onTripBoardStop, onTripBoardTime, onTrip);
        }
    }

    @Override
    public void forEachBoarding(int stopIndex, IntConsumer prevStopArrivalTimeConsumer) {
        if (state.isStopReachedInPreviousRound(stopIndex)) {
            prevStopArrivalTimeConsumer.accept(state.bestTimePreviousRound(stopIndex));
        }
    }
    @Override
    public void routeTransitAtStop(int stopIndex, int stopPos) {
        // Add board-slack(forward-search) or alight-slack(reverse-search)
        int earliestBoardTime = calculator.plusDuration(
            state.bestTimePreviousRound(stopIndex),
            slackProvider.boardSlack()
        );

        // check if we can back up to an earlier trip due to this stop being reached earlier
        boolean found = tripSearch.search(
            earliestBoardTime,
            stopPos,
            onTripIndex
        );

        if (found) {
            onTripIndex = tripSearch.getCandidateTripIndex();
            onTrip = tripSearch.getCandidateTrip();
            onTripBoardTime = earliestBoardTime;
            onTripBoardStop = stopIndex;
            // Calculate the time-shift, the time-shift will be a positive duration in a
            // forward-search, and a negative value in case of a reverse-search.
            onTripTimeShift = tripSearch.getCandidateTripTime() - earliestBoardTime;
        }
    }
}
