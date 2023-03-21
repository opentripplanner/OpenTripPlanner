package org.opentripplanner.raptor.rangeraptor.path.configure;

import static org.opentripplanner.raptor.rangeraptor.path.PathParetoSetComparators.paretoComparator;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.path.RaptorStopNameResolver;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.rangeraptor.context.SearchContext;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.raptor.rangeraptor.path.ForwardPathMapper;
import org.opentripplanner.raptor.rangeraptor.path.PathMapper;
import org.opentripplanner.raptor.rangeraptor.path.ReversePathMapper;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorPathConstrainedTransferSearch;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;

/**
 * This class is responsible for creating a a result collector - the set of paths.
 * <p/>
 * This class have REQUEST scope, so a new instance should be created for each new request/travel
 * search.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class PathConfig<T extends RaptorTripSchedule> {

  private final SearchContext<T> ctx;

  public PathConfig(SearchContext<T> context) {
    this.ctx = context;
  }

  /**
   * Create a new {@link DestinationArrivalPaths}. The generalized cost is included in the pareto
   * set criteria and will be generated for each leg and a total for the path.
   */
  public DestinationArrivalPaths<T> createDestArrivalPathsWithGeneralizedCost() {
    return createDestArrivalPaths(true);
  }

  /**
   * Create a new {@link DestinationArrivalPaths} without generalized-cost.
   */
  public DestinationArrivalPaths<T> createDestArrivalPathsWithoutGeneralizedCost() {
    return createDestArrivalPaths(false);
  }

  /* private members */

  private DestinationArrivalPaths<T> createDestArrivalPaths(boolean includeCost) {
    return new DestinationArrivalPaths<>(
      createPathParetoComparator(includeCost),
      ctx.calculator(),
      includeCost ? ctx.costCalculator() : null,
      ctx.slackProvider(),
      createPathMapper(includeCost),
      ctx.debugFactory(),
      ctx.stopNameResolver(),
      ctx.lifeCycle()
    );
  }

  private ParetoComparator<RaptorPath<T>> createPathParetoComparator(boolean includeCost) {
    return paretoComparator(
      includeCost,
      ctx.searchParams().timetable(),
      ctx.searchParams().preferLateArrival(),
      ctx.searchDirection(),
      ctx.multiCriteria().relaxC1AtDestination()
    );
  }

  private PathMapper<T> createPathMapper(boolean includeCost) {
    return createPathMapper(
      ctx.profile(),
      ctx.searchDirection(),
      ctx.raptorSlackProvider(),
      includeCost ? ctx.costCalculator() : null,
      ctx.stopNameResolver(),
      ctx.transit().transferConstraintsSearch(),
      ctx.lifeCycle()
    );
  }

  private static <S extends RaptorTripSchedule> PathMapper<S> createPathMapper(
    RaptorProfile profile,
    SearchDirection searchDirection,
    RaptorSlackProvider slackProvider,
    RaptorCostCalculator<S> costCalculator,
    RaptorStopNameResolver stopNameResolver,
    RaptorPathConstrainedTransferSearch<S> txConstraintsSearch,
    WorkerLifeCycle lifeCycle
  ) {
    return searchDirection.isForward()
      ? new ForwardPathMapper<>(
        slackProvider,
        costCalculator,
        stopNameResolver,
        txConstraintsSearch,
        lifeCycle,
        profile.useApproximateTripSearch()
      )
      : new ReversePathMapper<>(
        slackProvider,
        costCalculator,
        stopNameResolver,
        txConstraintsSearch,
        lifeCycle,
        profile.useApproximateTripSearch()
      );
  }
}
