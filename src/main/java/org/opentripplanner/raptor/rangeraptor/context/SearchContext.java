package org.opentripplanner.raptor.rangeraptor.context;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.debug.RaptorTimers;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripPattern;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.api.path.RaptorStopNameResolver;
import org.opentripplanner.raptor.api.request.DebugRequest;
import org.opentripplanner.raptor.api.request.MultiCriteriaRequest;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.raptor.api.request.SearchParams;
import org.opentripplanner.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoundProvider;
import org.opentripplanner.raptor.rangeraptor.internalapi.SlackProvider;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.rangeraptor.lifecycle.LifeCycleEventPublisher;
import org.opentripplanner.raptor.rangeraptor.lifecycle.LifeCycleSubscriptions;
import org.opentripplanner.raptor.rangeraptor.support.TimeBasedBoardingSupport;
import org.opentripplanner.raptor.rangeraptor.transit.AccessPaths;
import org.opentripplanner.raptor.rangeraptor.transit.EgressPaths;
import org.opentripplanner.raptor.rangeraptor.transit.ForwardRaptorTransitCalculator;
import org.opentripplanner.raptor.rangeraptor.transit.RaptorTransitCalculator;
import org.opentripplanner.raptor.rangeraptor.transit.ReverseRaptorTransitCalculator;
import org.opentripplanner.raptor.rangeraptor.transit.RoundTracker;
import org.opentripplanner.raptor.rangeraptor.transit.SlackProviderAdapter;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;

/**
 * The search context is used to hold search scoped instances and to pass these to who ever need
 * them.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class SearchContext<T extends RaptorTripSchedule> {

  /**
   * The request input used to customize the worker to the clients needs.
   */
  private final RaptorRequest<T> request;

  /**
   * the transit data role needed for routing
   */
  protected final RaptorTransitDataProvider<T> transit;

  private final RaptorTransitCalculator<T> calculator;
  private final RaptorTuningParameters tuningParameters;
  private final RoundTracker roundTracker;
  private final DebugHandlerFactory<T> debugFactory;
  private final EgressPaths egressPaths;
  private final AccessPaths accessPaths;
  private final LifeCycleSubscriptions lifeCycleSubscriptions = new LifeCycleSubscriptions();

  /** Lazy initialized */
  private RaptorCostCalculator<T> costCalculator = null;

  public SearchContext(
    RaptorRequest<T> request,
    RaptorTuningParameters tuningParameters,
    RaptorTransitDataProvider<T> transit
  ) {
    this.request = request;
    this.tuningParameters = tuningParameters;
    this.transit = transit;
    this.accessPaths = accessPaths(request);
    this.egressPaths = egressPaths(request);

    // Note that it is the "new" request that is passed in.
    this.calculator = createCalculator(this.request, tuningParameters);
    this.roundTracker =
      new RoundTracker(
        nRounds(),
        request.searchParams().numberOfAdditionalTransfers(),
        lifeCycle()
      );
    this.debugFactory = new DebugHandlerFactory<>(debugRequest(request), lifeCycle());
  }

  public AccessPaths accessPaths() {
    return accessPaths;
  }

  public EgressPaths egressPaths() {
    return egressPaths;
  }

  public int[] egressStops() {
    return egressPaths().stops();
  }

  public SearchParams searchParams() {
    return request.searchParams();
  }

  public RaptorProfile profile() {
    return request.profile();
  }

  public SearchDirection searchDirection() {
    return request.searchDirection();
  }

  public MultiCriteriaRequest<T> multiCriteria() {
    return request.multiCriteria();
  }

  public RaptorTransitDataProvider<T> transit() {
    return transit;
  }

  public RaptorTransitCalculator<T> calculator() {
    return calculator;
  }

  /**
   * Create new slack-provider for use in Raptor, handles reverse and forward search as well as
   * including transfer-slack into board-slack between transits.
   */
  public SlackProvider slackProvider() {
    return createSlackProvider(searchDirection(), raptorSlackProvider(), lifeCycle());
  }

  public RaptorSlackProvider raptorSlackProvider() {
    return transit.slackProvider();
  }

  /**
   * The board-slack (duration time in seconds) to add to the stop arrival time, before boarding
   * the given trip pattern. THIS DOES NOT INCLUDE THE transfer-slack, and should only be used to
   * time-shift the access-path.
   * <p>
   * Unit: seconds.
   */
  public ToIntFunction<RaptorTripPattern> boardSlackProvider() {
    return createBoardSlackProvider(searchDirection(), raptorSlackProvider());
  }

  @Nullable
  public RaptorCostCalculator<T> costCalculator() {
    if (costCalculator == null) {
      this.costCalculator = transit.multiCriteriaCostCalculator();
    }
    return costCalculator;
  }

  public DebugHandlerFactory<T> debugFactory() {
    return debugFactory;
  }

  public RaptorTimers performanceTimers() {
    return request.performanceTimers();
  }

  /** Number of stops in transit graph. */
  public int nStops() {
    return transit.numberOfStops();
  }

  /** Calculate the maximum number of rounds to perform. */
  public int nRounds() {
    if (request.searchParams().isMaxNumberOfTransfersSet()) {
      return request.searchParams().maxNumberOfTransfers() + 1;
    }
    return tuningParameters.maxNumberOfTransfers() + 1;
  }

  public RoundProvider roundProvider() {
    return roundTracker;
  }

  public WorkerLifeCycle lifeCycle() {
    return lifeCycleSubscriptions;
  }

  public LifeCycleEventPublisher createLifeCyclePublisher() {
    LifeCycleEventPublisher publisher = new LifeCycleEventPublisher(lifeCycleSubscriptions);
    // We want the code to fail if someone try to attach to the worker lifecycle
    // after it is initialized; Hence close for new subscriptions
    lifeCycleSubscriptions.close();
    return publisher;
  }

  /**
   * See {@link RaptorRequest#useConstrainedTransfers()}
   */
  public boolean useConstrainedTransfers() {
    return request.useConstrainedTransfers();
  }

  /* private methods */

  public RaptorStopNameResolver stopNameResolver() {
    return transit.stopNameResolver();
  }

  public TimeBasedBoardingSupport<T> createTimeBasedBoardingSupport() {
    return new TimeBasedBoardingSupport<>(
      accessPaths().hasTimeDependentAccess(),
      slackProvider(),
      calculator(),
      roundProvider(),
      lifeCycle()
    );
  }

  /**
   * The multi-criteria state can handle multiple access/egress paths to a single stop, but the
   * Standard and BestTime states do not. To get a deterministic behaviour we filter the paths and
   * return the paths with the shortest duration for none multi-criteria search. If two paths have
   * the same duration the first one is picked. Note! If the access/egress paths contains flex as
   * well, then we need to look at mode for arriving at tha stop as well. A Flex arrive-on-board can
   * be used with a transfer even if the time is worse compared with walking.
   * <p>
   * This method is static and package local to enable unit-testing.
   */
  static Collection<RaptorAccessEgress> accessOrEgressPaths(
    boolean getAccess,
    RaptorProfile profile,
    SearchParams searchParams
  ) {
    var paths = getAccess ? searchParams.accessPaths() : searchParams.egressPaths();

    if (profile.is(RaptorProfile.MULTI_CRITERIA)) {
      return paths;
    }

    // For none MC-search we only want the fastest transfer for each stop,
    // no duplicates are accepted
    Map<Integer, RaptorAccessEgress> bestTimePaths = new HashMap<>();
    for (RaptorAccessEgress it : paths) {
      RaptorAccessEgress existing = bestTimePaths.get(it.stop());
      if (existing == null || it.durationInSeconds() < existing.durationInSeconds()) {
        bestTimePaths.put(it.stop(), it);
      }
    }
    return List.copyOf(bestTimePaths.values());
  }

  /**
   * Create a new calculator depending on the desired search direction.
   */
  private static <T extends RaptorTripSchedule> RaptorTransitCalculator<T> createCalculator(
    RaptorRequest<T> r,
    RaptorTuningParameters t
  ) {
    SearchParams s = r.searchParams();
    return r.searchDirection().isForward()
      ? new ForwardRaptorTransitCalculator<>(s, t)
      : new ReverseRaptorTransitCalculator<>(s, t);
  }

  private static DebugRequest debugRequest(RaptorRequest<?> request) {
    return request.searchDirection().isForward()
      ? request.debug()
      : request.mutate().debug().reverseDebugRequest().build();
  }

  private static SlackProvider createSlackProvider(
    SearchDirection searchDirection,
    RaptorSlackProvider slackProvider,
    WorkerLifeCycle lifeCycle
  ) {
    return searchDirection.isForward()
      ? SlackProviderAdapter.forwardSlackProvider(slackProvider, lifeCycle)
      : SlackProviderAdapter.reverseSlackProvider(slackProvider, lifeCycle);
  }

  private static ToIntFunction<RaptorTripPattern> createBoardSlackProvider(
    SearchDirection searchDirection,
    RaptorSlackProvider slackProvider
  ) {
    return searchDirection.isForward()
      ? p -> slackProvider.boardSlack(p.slackIndex())
      : p -> slackProvider.alightSlack(p.slackIndex());
  }

  private static AccessPaths accessPaths(RaptorRequest<?> request) {
    boolean forward = request.searchDirection().isForward();
    var params = request.searchParams();
    var paths = forward ? params.accessPaths() : params.egressPaths();
    return AccessPaths.create(paths, request.profile());
  }

  private static EgressPaths egressPaths(RaptorRequest<?> request) {
    boolean forward = request.searchDirection().isForward();
    var params = request.searchParams();
    var paths = forward ? params.egressPaths() : params.accessPaths();
    return EgressPaths.create(paths, request.profile());
  }
}
