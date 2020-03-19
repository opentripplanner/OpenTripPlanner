package org.opentripplanner.transit.raptor.rangeraptor.multicriteria;

import org.opentripplanner.transit.raptor.api.transit.IntIterator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.rangeraptor.TransitRoutingStrategy;
import org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals.AbstractStopArrival;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TripScheduleSearch;


/**
 * The purpose of this class is to implement the multi-criteria specific functionality of
 * the worker.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class McTransitWorker<T extends RaptorTripSchedule> implements TransitRoutingStrategy<T> {

    private final McRangeRaptorWorkerState<T> state;
    private final TransitCalculator calculator;

    private RaptorTripPattern pattern;
    private TripScheduleSearch<T> tripSearch;

    public McTransitWorker(McRangeRaptorWorkerState<T> state, TransitCalculator calculator) {
        this.state = state;
        this.calculator = calculator;
    }

    @Override
    public void prepareForTransitWith(RaptorTripPattern pattern, TripScheduleSearch<T> tripSearch) {
        this.pattern = pattern;
        this.tripSearch = tripSearch;
    }

    @Override
    public void routeTransitAtStop(int boardStopPos) {
        final int nPatternStops = pattern.numberOfStopsInPattern();
        int boardStopIndex = pattern.stopIndex(boardStopPos);

        for (AbstractStopArrival<T> boardFrom : state.listStopArrivalsPreviousRound(boardStopIndex)) {

            int earliestBoardTime = calculator.earliestBoardTime(boardFrom.arrivalTime());
            boolean found = tripSearch.search(earliestBoardTime, boardStopPos);

            if (found) {
                T trip = tripSearch.getCandidateTrip();
                final int tripDepartureTime = trip.departure(boardStopPos);
                IntIterator patternStops = calculator.patternStopIterator(boardStopPos, nPatternStops);

                while (patternStops.hasNext()) {
                    int alightStopPos = patternStops.next();
                    int alightStopIndex = pattern.stopIndex(alightStopPos);

                    state.transitToStop(
                            boardFrom,
                            alightStopIndex,
                            trip.arrival(alightStopPos),
                            tripDepartureTime,
                            trip
                    );
                }
            }
        }
    }
}
