package org.opentripplanner.raptor.rangeraptor.path;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.path.RaptorStopNameResolver;
import org.opentripplanner.raptor.path.PathBuilder;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.rangeraptor.transit.TripTimesSearch;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorPathConstrainedTransferSearch;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;

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

  private final RaptorSlackProvider slackProvider;
  private final RaptorCostCalculator<T> costCalculator;
  private final RaptorStopNameResolver stopNameResolver;
  private final BoardAndAlightTimeSearch tripSearch;
  private final RaptorPathConstrainedTransferSearch<T> transferConstraintsSearch;

  private int iterationDepartureTime = -1;

  public ReversePathMapper(
    RaptorSlackProvider slackProvider,
    RaptorCostCalculator<T> costCalculator,
    RaptorStopNameResolver stopNameResolver,
    RaptorPathConstrainedTransferSearch<T> transferConstraintsSearch,
    WorkerLifeCycle lifeCycle,
    boolean useApproximateTripTimesSearch
  ) {
    this.slackProvider = slackProvider;
    this.costCalculator = costCalculator;
    this.stopNameResolver = stopNameResolver;
    this.transferConstraintsSearch = transferConstraintsSearch;
    this.tripSearch = tripTimesSearch(useApproximateTripTimesSearch);
    lifeCycle.onSetupIteration(this::setRangeRaptorIterationDepartureTime);
  }

  @Override
  public RaptorPath<T> mapToPath(final DestinationArrival<T> destinationArrival) {
    var pathBuilder = PathBuilder.tailPathBuilder(
      slackProvider,
      destinationArrival.arrivalTime(),
      costCalculator,
      stopNameResolver,
      transferConstraintsSearch
    );
    var arrival = destinationArrival.previous();

    pathBuilder.access(destinationArrival.egressPath().egress());

    while (true) {
      switch (arrival.arrivedBy()) {
        case ACCESS:
          pathBuilder.egress(arrival.accessPath().access());

          pathBuilder.c2(arrival.c2());

          return pathBuilder.build();
        case TRANSIT:
          var times = tripSearch.find(arrival);
          var transit = arrival.transitPath();
          pathBuilder.transit(transit.trip(), times);
          break;
        case TRANSFER:
          pathBuilder.transfer(arrival.transfer(), arrival.previous().stop());
          break;
        case EGRESS:
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

  private void setRangeRaptorIterationDepartureTime(int iterationDepartureTime) {
    this.iterationDepartureTime = iterationDepartureTime;
  }
}
