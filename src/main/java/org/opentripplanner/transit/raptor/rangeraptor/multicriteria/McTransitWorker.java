package org.opentripplanner.transit.raptor.rangeraptor.multicriteria;

import org.opentripplanner.transit.raptor.api.transit.IntIterator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.SlackProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.rangeraptor.RoutingStrategy;
import org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals.AbstractStopArrival;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TripScheduleSearch;


/**
 * The purpose of this class is to implement the multi-criteria specific functionality of
 * the worker.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class McTransitWorker<T extends RaptorTripSchedule> implements RoutingStrategy<T> {

    private final McRangeRaptorWorkerState<T> state;
    private final TransitCalculator calculator;
    private final SlackProvider slackProvider;

    private RaptorTripPattern pattern;
    private TripScheduleSearch<T> tripSearch;

    public McTransitWorker(McRangeRaptorWorkerState<T> state, SlackProvider slackProvider, TransitCalculator calculator) {
        this.state = state;
        this.slackProvider = slackProvider;
        this.calculator = calculator;
    }

    @Override
    public void prepareForTransitWith(RaptorTripPattern pattern, TripScheduleSearch<T> tripSearch) {
        this.pattern = pattern;
        this.tripSearch = tripSearch;
        slackProvider.setCurrentPattern(pattern);
    }

    @Override
    public void routeTransitAtStop(int boardStopPos) {

        // If it is not possible to board the pattern at this stop, then return
        if(!pattern.boardingPossibleAt(boardStopPos)) {
            return;
        }

        final int nPatternStops = pattern.numberOfStopsInPattern();
        int boardStopIndex = pattern.stopIndex(boardStopPos);

        // For each arrival at the current stop
        for (AbstractStopArrival<T> prevArrival : state.listStopArrivalsPreviousRound(boardStopIndex)) {

            int earliestBoardTime = calculator.plusDuration(
                    prevArrival.arrivalTime(),
                    slackProvider.boardSlack()
            );

            boolean found = tripSearch.search(earliestBoardTime, boardStopPos);

            if (found) {
                T trip = tripSearch.getCandidateTrip();
                final int tripDepartureTime = trip.departure(boardStopPos);
                IntIterator patternStops = calculator.patternStopIterator(boardStopPos, nPatternStops);

                // Visit all stops after the boarded stop position and add transit arrival to
                // each alight stop
                while (patternStops.hasNext()) {
                    int alightStopPos = patternStops.next();
                    if(pattern.alightingPossibleAt(alightStopPos)) {
                        int alightStopIndex = pattern.stopIndex(alightStopPos);

                        state.transitToStop(
                                prevArrival,
                                alightStopIndex,
                                trip.arrival(alightStopPos),
                                slackProvider.alightSlack(),
                                tripDepartureTime,
                                trip
                        );
                    }
                }
            }
        }
    }

    @Override
    public void setInitialTimeForIteration(RaptorTransfer it, int iterationDepartureTime) {
        // Earliest possible departure time from the origin, or latest possible arrival time at the
        // destination if searching backwards, using this AccessEgress.
        int departureTime = calculator.departureTime(it, iterationDepartureTime);

        // This access is not available after the iteration departure time
        if (departureTime == -1) return;

        state.setInitialTimeForIteration(it, departureTime);
    }
}
