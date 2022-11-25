package org.opentripplanner.raptor.rangeraptor.path;

import org.opentripplanner.raptor.api.path.Path;
import org.opentripplanner.raptor.api.path.PathBuilder;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.rangeraptor.transit.TripTimesSearch;
import org.opentripplanner.raptor.spi.CostCalculator;
import org.opentripplanner.raptor.spi.RaptorPathConstrainedTransferSearch;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.raptor.spi.RaptorStopNameResolver;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * Build a path from a destination arrival - this maps between the domain of routing to the domain
 * of result paths. All values not needed for routing is computed as part of this mapping.
 */
public final class ForwardPathMapper<T extends RaptorTripSchedule> implements PathMapper<T> {

  private final RaptorPathConstrainedTransferSearch<T> transferConstraintsSearch;
  private final RaptorSlackProvider slackProvider;
  private final CostCalculator<T> costCalculator;
  private final BoardAndAlightTimeSearch tripSearch;
  private final RaptorStopNameResolver stopNameResolver;

  private int iterationDepartureTime = -1;

  public ForwardPathMapper(
    RaptorPathConstrainedTransferSearch<T> transferConstraintsSearch,
    RaptorSlackProvider slackProvider,
    CostCalculator<T> costCalculator,
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

  @Override
  public Path<T> mapToPath(final DestinationArrival<T> destinationArrival) {
    var pathBuilder = PathBuilder.headPathBuilder(
      transferConstraintsSearch,
      slackProvider,
      costCalculator,
      stopNameResolver
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
        throw new RuntimeException("Unknown arrival type: " + arrival.getClass().getSimpleName());
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

  private void setRangeRaptorIterationDepartureTime(int iterationDepartureTime) {
    this.iterationDepartureTime = iterationDepartureTime;
  }
}
