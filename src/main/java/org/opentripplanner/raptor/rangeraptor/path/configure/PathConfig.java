package org.opentripplanner.raptor.rangeraptor.path.configure;

import static org.opentripplanner.raptor.rangeraptor.path.PathParetoSetComparators.paretoComparator;

import org.opentripplanner.raptor.api.model.DominanceFunction;
import org.opentripplanner.raptor.api.model.GeneralizedCostRelaxFunction;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.RelaxFunction;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.path.RaptorStopNameResolver;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.rangeraptor.context.SearchContext;
import org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetCost;
import org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime;
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
 * This class has REQUEST scope, so a new instance should be created for each new request/travel
 * search.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class PathConfig<T extends RaptorTripSchedule> {

  private final SearchContext<T> ctx;

  public PathConfig(SearchContext<T> context) {
    this.ctx = context;
  }

  public DestinationArrivalPaths<T> createDestArrivalPathsStdSearch() {
    return createDestArrivalPaths(ParetoSetCost.NONE, DominanceFunction.noop());
  }

  /**
   * Create a new {@link DestinationArrivalPaths}.
   * @param costConfig Supported configurations of c1, c2 and relaxed cost(c1).
   * @param c2Comp c2 comparator function to be used in the pareto set criteria. If c2 comparator is null
   *               then no c2 comparison will be used.
   */
  public DestinationArrivalPaths<T> createDestArrivalPaths(
    ParetoSetCost costConfig,
    DominanceFunction c2Comp
  ) {
    return new DestinationArrivalPaths<>(
      createPathParetoComparator(costConfig, c2Comp),
      ctx.calculator(),
      costConfig.includeC1() ? ctx.costCalculator() : null,
      ctx.acceptC2AtDestination(),
      ctx.slackProvider(),
      createPathMapper(costConfig.includeC1()),
      ctx.debugFactory(),
      ctx.stopNameResolver(),
      ctx.lifeCycle()
    );
  }

  /* private members */

  private ParetoComparator<RaptorPath<T>> createPathParetoComparator(
    ParetoSetCost costConfig,
    DominanceFunction c2Comp
  ) {
    // This code goes away when the USE_C1_RELAX_DESTINATION is deleted
    var relaxC1 =
      switch (costConfig) {
        case USE_C1_RELAXED_IF_C2_IS_OPTIMAL -> ctx.multiCriteria().relaxC1();
        case USE_C1_RELAX_DESTINATION -> GeneralizedCostRelaxFunction.of(
          ctx.multiCriteria().relaxCostAtDestination()
        );
        default -> RelaxFunction.NORMAL;
      };

    return paretoComparator(paretoSetTimeConfig(), costConfig, relaxC1, c2Comp);
  }

  private ParetoSetTime paretoSetTimeConfig() {
    boolean preferLatestDeparture =
      ctx.searchParams().preferLateArrival() != ctx.searchDirection().isInReverse();

    ParetoSetTime timeConfig = ctx.searchParams().timetable()
      ? ParetoSetTime.USE_TIMETABLE
      : (preferLatestDeparture ? ParetoSetTime.USE_DEPARTURE_TIME : ParetoSetTime.USE_ARRIVAL_TIME);
    return timeConfig;
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
