package org.opentripplanner.raptor.rangeraptor.path.configure;

import static org.opentripplanner.raptor.rangeraptor.path.PathParetoSetComparators.paretoComparator;

import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.DominanceFunction;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.rangeraptor.context.SearchContext;
import org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetCost;
import org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime;
import org.opentripplanner.raptor.rangeraptor.path.DestinationArrival;
import org.opentripplanner.raptor.rangeraptor.path.DestinationArrivalParetoSetComparators;
import org.opentripplanner.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.raptor.rangeraptor.path.ForwardPathMapper;
import org.opentripplanner.raptor.rangeraptor.path.PathMapper;
import org.opentripplanner.raptor.rangeraptor.path.ReversePathMapper;
import org.opentripplanner.raptor.rangeraptor.path.TimeShiftedDepartureTime;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorPathConstrainedTransferSearch;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.raptor.spi.RaptorStopNameResolver;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.SearchDirection;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;

/**
 * This class is responsible for creating a a result collector - the set of paths.
 * <p/>
 * This class has REQUEST scope, so a new instance should be created for each new request/travel
 * search.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class DestinationArrivalConfig<T extends RaptorTripSchedule> {

  private final SearchContext<T> ctx;

  public DestinationArrivalConfig(SearchContext<T> context) {
    this.ctx = context;
  }

  public DestinationArrivalPaths<T> createDestArrivalPathsStdSearch() {
    ParetoComparator<DestinationArrival<T>> comparator =
      DestinationArrivalParetoSetComparators.paretoComparator(
        paretoSetTimeConfig(),
        ParetoSetCost.NONE,
        null,
        null
      );
    return DestinationArrivalPaths.forStdRaptor(
      comparator,
      ctx.calculator(),
      null,
      createDepartureTimeCalculator(ParetoSetCost.NONE),
      ctx.slackProvider(),
      createPathMapper(false),
      ctx.debugFactory(),
      ctx.stopNameResolver(),
      ctx.lifeCycle()
    );
  }

  /**
   * Create a {@link DestinationArrivalPaths} for McRaptor (lazy path building).
   *
   * @param costConfig Supported configurations of c1, c2 and relaxed cost(c1).
   * @param c2Comp     Dominance function for c2; {@code null} means c2 is not used.
   */
  public DestinationArrivalPaths<T> createMcDestArrivalPaths(
    ParetoSetCost costConfig,
    DominanceFunction c2Comp
  ) {
    var relaxC1 = ctx.multiCriteria().relaxC1();
    ParetoComparator<DestinationArrival<T>> comparator =
      DestinationArrivalParetoSetComparators.paretoComparator(
        paretoSetTimeConfig(),
        costConfig,
        relaxC1,
        c2Comp
      );
    return DestinationArrivalPaths.forMcRaptor(
      comparator,
      ctx.calculator(),
      costConfig.useC1() ? ctx.costCalculator() : null,
      createDepartureTimeCalculator(costConfig),
      ctx.slackProvider(),
      createPathMapper(costConfig.useC1()),
      ctx.debugFactory(),
      ctx.stopNameResolver(),
      ctx.lifeCycle()
    );
  }

  public ParetoComparator<RaptorPath<T>> createPathParetoComparator(
    ParetoSetCost costConfig,
    DominanceFunction c2Comp
  ) {
    var relaxC1 = ctx.multiCriteria().relaxC1();
    return paretoComparator(paretoSetTimeConfig(), costConfig, relaxC1, c2Comp);
  }

  /* private members */

  private ParetoSetTime paretoSetTimeConfig() {
    boolean reverse = ctx.searchDirection().isInReverse();
    boolean preferLatestDeparture = ctx.searchParams().preferLateArrival() != reverse;

    if (ctx.searchParams().timetable()) {
      return reverse ? ParetoSetTime.USE_TIMETABLE_REVERSE : ParetoSetTime.USE_TIMETABLE;
    }
    if (preferLatestDeparture) {
      return reverse ? ParetoSetTime.USE_DEPARTURE_TIME_REVERSE : ParetoSetTime.USE_DEPARTURE_TIME;
    }
    return reverse ? ParetoSetTime.USE_ARRIVAL_TIME_REVERSE : ParetoSetTime.USE_ARRIVAL_TIME;
  }

  /**
   * The time-shifted departure is only needed to calculate the duration or when the departure time
   * is a criterion, and only for forward searches. The calculation is skipped for timetable
   * searches and searches that only use the arrival time.
   */
  @Nullable
  private TimeShiftedDepartureTime<T> createDepartureTimeCalculator(ParetoSetCost costConfig) {
    boolean needed =
      ctx.searchDirection().isForward() &&
      switch (paretoSetTimeConfig()) {
        case USE_ARRIVAL_TIME -> costConfig != ParetoSetCost.NONE;
        case USE_DEPARTURE_TIME -> true;
        default -> false;
      };
    return needed ? new TimeShiftedDepartureTime<>(ctx.raptorSlackProvider()) : null;
  }

  private PathMapper<T> createPathMapper(boolean includeCost) {
    return createPathMapper(
      ctx.searchDirection(),
      ctx.raptorSlackProvider(),
      includeCost ? ctx.costCalculator() : null,
      ctx.stopNameResolver(),
      ctx.transitData().transferConstraintsSearch()
    );
  }

  private static <S extends RaptorTripSchedule> PathMapper<S> createPathMapper(
    SearchDirection searchDirection,
    RaptorSlackProvider slackProvider,
    RaptorCostCalculator<S> costCalculator,
    RaptorStopNameResolver stopNameResolver,
    RaptorPathConstrainedTransferSearch<S> txConstraintsSearch
  ) {
    return searchDirection.isForward()
      ? new ForwardPathMapper<>(
          slackProvider,
          costCalculator,
          stopNameResolver,
          txConstraintsSearch
        )
      : new ReversePathMapper<>(
          slackProvider,
          costCalculator,
          stopNameResolver,
          txConstraintsSearch
        );
  }
}
