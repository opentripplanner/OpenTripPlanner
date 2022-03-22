package org.opentripplanner.transit.raptor.rangeraptor.path;

import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathBuilder;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorPathConstrainedTransferSearch;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorStopNameResolver;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TripTimesSearch;


/**
 * Build a path from a destination arrival - this maps between the domain of routing
 * to the domain of result paths. All values not needed for routing is computed as part of this mapping.
 */
public final class ForwardPathMapper<T extends RaptorTripSchedule> implements PathMapper<T> {
    private final RaptorPathConstrainedTransferSearch<T> transferConstraintsSearch;
    private final RaptorSlackProvider slackProvider;
    private final CostCalculator costCalculator;
    private final BoardAndAlightTimeSearch tripSearch;
    private final RaptorStopNameResolver stopNameResolver;

    private int iterationDepartureTime = -1;

    public ForwardPathMapper(
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
        this.tripSearch = forwardSearch(useApproximateTripTimesSearch);
        lifeCycle.onSetupIteration(this::setRangeRaptorIterationDepartureTime);
    }

    private void setRangeRaptorIterationDepartureTime(int iterationDepartureTime) {
        this.iterationDepartureTime = iterationDepartureTime;
    }

    @Override
    public Path<T> mapToPath(final DestinationArrival<T> destinationArrival) {
        var pathBuilder = PathBuilder.headPathBuilder(
                transferConstraintsSearch, slackProvider, costCalculator, stopNameResolver
        );

        pathBuilder.egress(destinationArrival.egressPath().egress());
        ArrivalView<T> arrival = destinationArrival.previous();

        while (true) {
            if (arrival.arrivedByTransit()) {
                var times = tripSearch.find(arrival);
                pathBuilder.transit(arrival.transitPath().trip(), times);
            } else if (arrival.arrivedByTransfer()) {
                pathBuilder.transfer(arrival.transferPath().transfer(), arrival.stop());
            } else if (arrival.arrivedByAccess()) {
                pathBuilder.access(arrival.accessPath().access());
                break;
            } else {
                throw new RuntimeException(
                        "Unknown arrival type: " + arrival.getClass().getSimpleName()
                );
            }
            arrival = arrival.previous();
        }

        return pathBuilder.build(iterationDepartureTime);
    }

    private static BoardAndAlightTimeSearch forwardSearch(boolean useApproximateTimeSearch) {
        return useApproximateTimeSearch
                ? TripTimesSearch::findTripForwardSearchApproximateTime
                : TripTimesSearch::findTripForwardSearch;
    }
}
