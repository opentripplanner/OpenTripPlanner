package org.opentripplanner.transit.raptor.rangeraptor.path;

import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathBuilder;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorPathConstrainedTransferSearch;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorStopNameResolver;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TripTimesSearch;


/**
 * Build a path from a destination arrival - this maps between the domain of routing to the domain
 * of result paths. All values not needed for routing is computed as part of this mapping.
 * <p/>
 * This mapper maps the result of a reverse-search. And is therefore responsible for doing some
 * adjustments to the result. The internal state stores 'latest possible arrival-times', so to map
 * back to paths the 'board slack' is removed from the 'latest possible arrival-time' to get the
 * next legs 'boardTime'. Also, the path is reversed again, so the original origin - temporally made
 * destination - is returned to origin again ;-)
 * <p/>
 * This mapper uses recursion to reverse the results.
 */
public final class ReversePathMapper<T extends RaptorTripSchedule> implements PathMapper<T> {
    private final RaptorPathConstrainedTransferSearch<T> transferConstraintsSearch;
    private final RaptorSlackProvider slackProvider;
    private final CostCalculator costCalculator;
    private final RaptorStopNameResolver stopNameResolver;
    private final BoardAndAlightTimeSearch tripSearch;

    private int iterationDepartureTime = -1;

    public ReversePathMapper(
            RaptorPathConstrainedTransferSearch<T> transferConstraintsSearch,
            RaptorSlackProvider slackProvider,
            CostCalculator costCalculator,
            RaptorStopNameResolver stopNameResolver,
            WorkerLifeCycle lifeCycle,
            boolean useApproximateTripTimesSearch
    ) {
        this.transferConstraintsSearch = transferConstraintsSearch;
        this.slackProvider = slackProvider;
        this.costCalculator = costCalculator;
        this.stopNameResolver = stopNameResolver;
        this.tripSearch = tripTimesSearch(useApproximateTripTimesSearch);
        lifeCycle.onSetupIteration(this::setRangeRaptorIterationDepartureTime);
    }

    private void setRangeRaptorIterationDepartureTime(int iterationDepartureTime) {
        this.iterationDepartureTime = iterationDepartureTime;
    }

    @Override
    public Path<T> mapToPath(final DestinationArrival<T> destinationArrival) {
        var pathBuilder = PathBuilder.tailPathBuilder(
                transferConstraintsSearch, slackProvider, costCalculator, stopNameResolver
        );
        var arrival = destinationArrival.previous();

        pathBuilder.access(destinationArrival.egressPath().egress());

        while (true) {
            if(arrival.arrivedByAccess()) {
                pathBuilder.egress(arrival.accessPath().access());
                return pathBuilder.build(iterationDepartureTime);
            }
            else if(arrival.arrivedByTransit()) {
                var times = tripSearch.find(arrival);
                var transit = arrival.transitPath();
                pathBuilder.transit(transit.trip(), times);
            }
            else if(arrival.arrivedByTransfer()) {
                pathBuilder.transfer(arrival.transferPath().transfer(), arrival.previous().stop());
            }
            else {
                throw new IllegalStateException("Unexpected arrival: " + arrival);
            }
            arrival = arrival.previous();
        }
    }


    private static BoardAndAlightTimeSearch tripTimesSearch(boolean useApproximateTimeSearch) {
        return useApproximateTimeSearch
                ? TripTimesSearch::findTripReverseSearchApproximateTime
                : TripTimesSearch::findTripReverseSearch;
    }
}
