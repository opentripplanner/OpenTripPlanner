package org.opentripplanner.raptor.rangeraptor.path;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.raptor.rangeraptor.internalapi.DebugHandler;
import org.opentripplanner.raptor.rangeraptor.internalapi.SlackProvider;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.rangeraptor.transit.RaptorTransitCalculator;
import org.opentripplanner.raptor.spi.RaptorConstants;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorStopNameResolver;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;
import org.opentripplanner.raptor.util.paretoset.ParetoSet;
import org.opentripplanner.utils.lang.OtpNumberFormat;
import org.opentripplanner.utils.logging.Throttle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The responsibility of this class is to collect result paths for destination arrivals. It does so
 * using a pareto set of {@link DestinationArrival}s. The comparator is passed in as an argument to
 * the constructor, making it possible to collect different sets in different scenarios.
 * <p/>
 * Two modes are supported, selected via the static factory methods:
 * <ul>
 *   <li><b>StdRaptor mode</b> ({@link #forStdRaptor}): paths are built eagerly when an arrival is
 *       accepted. Necessary because StdRaptor's stop-arrival state is mutable and is overwritten
 *       between iterations.
 *   <li><b>McRaptor mode</b> ({@link #forMcRaptor}): {@link DestinationArrival} objects are
 *       stored in the pareto set and paths are built lazily when {@link #listPaths()} is called.
 *       Safe because McRaptor's per-stop pareto sets keep all accepted arrivals indefinitely.
 * </ul>
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class DestinationArrivalPaths<T extends RaptorTripSchedule> {

  private static final Logger LOG = LoggerFactory.getLogger(DestinationArrivalPaths.class);
  private static final Throttle THROTTLE_MISS_MATCH = Throttle.ofOneSecond();

  private final RaptorTransitCalculator<T> transitCalculator;
  private final SlackProvider slackProvider;
  private final PathMapper<T> pathMapper;
  private final DebugHandler<RaptorPath<?>> debugPathHandler;
  private final RaptorStopNameResolver stopNameResolver;
  private boolean reachedCurrentRound = false;
  private int iterationDepartureTime = -1;

  private final ParetoSet<DestinationArrival<T>> arrivals;

  /**
   * Reused by {@link #qualify}; never added to {@link #arrivals}. Not thread-safe, see
   * {@link #qualify} for how to handle parallel use.
   */
  private final DestinationArrival<T> probe = DestinationArrival.createProbe();

  /**
   * Non-null in StdRaptor mode: paths are built eagerly and stored here, keyed by arrival
   * identity. Null in McRaptor mode: paths are built lazily in {@link #listPaths()}.
   */
  @Nullable
  private final IdentityHashMap<DestinationArrival<T>, RaptorPath<T>> eagerPaths;

  @Nullable
  private final RaptorCostCalculator<T> costCalculator;

  private DestinationArrivalPaths(
    ParetoSet<DestinationArrival<T>> arrivals,
    @Nullable IdentityHashMap<DestinationArrival<T>, RaptorPath<T>> eagerPaths,
    RaptorTransitCalculator<T> transitCalculator,
    @Nullable RaptorCostCalculator<T> costCalculator,
    SlackProvider slackProvider,
    PathMapper<T> pathMapper,
    DebugHandlerFactory<T> debugHandlerFactory,
    RaptorStopNameResolver stopNameResolver,
    WorkerLifeCycle lifeCycle
  ) {
    this.arrivals = arrivals;
    this.eagerPaths = eagerPaths;
    this.transitCalculator = transitCalculator;
    this.costCalculator = costCalculator;
    this.slackProvider = slackProvider;
    this.pathMapper = pathMapper;
    this.debugPathHandler = debugHandlerFactory.debugPathArrival();
    this.stopNameResolver = stopNameResolver;
    lifeCycle.onPrepareForNextRound(round -> clearReachedCurrentRoundFlag());
    lifeCycle.onSetupIteration(this::setRangeRaptorIterationDepartureTime);
  }

  /**
   * The pareto set elements are destination arrivals, but the debug API reports paths. Paths are
   * only built for debug events. The probe used by {@link #qualify} has no arrival chain, and is
   * skipped.
   */
  private static <T extends RaptorTripSchedule> Function<
    DestinationArrival<T>,
    RaptorPath<?>
  > toPath(PathMapper<T> pathMapper) {
    return arrival -> arrival.previous() == null ? null : pathMapper.mapToPath(arrival);
  }

  /** Create a {@link DestinationArrivalPaths} for StdRaptor (eager path building). */
  public static <T extends RaptorTripSchedule> DestinationArrivalPaths<T> forStdRaptor(
    ParetoComparator<DestinationArrival<T>> arrivalComparator,
    RaptorTransitCalculator<T> transitCalculator,
    @Nullable RaptorCostCalculator<T> costCalculator,
    SlackProvider slackProvider,
    PathMapper<T> pathMapper,
    DebugHandlerFactory<T> debugHandlerFactory,
    RaptorStopNameResolver stopNameResolver,
    WorkerLifeCycle lifeCycle
  ) {
    var arrivals = ParetoSet.of(
      arrivalComparator,
      debugHandlerFactory.paretoSetDebugPathListener(toPath(pathMapper))
    );
    return new DestinationArrivalPaths<>(
      arrivals,
      new IdentityHashMap<>(),
      transitCalculator,
      costCalculator,
      slackProvider,
      pathMapper,
      debugHandlerFactory,
      stopNameResolver,
      lifeCycle
    );
  }

  /** Create a {@link DestinationArrivalPaths} for McRaptor (lazy path building). */
  public static <T extends RaptorTripSchedule> DestinationArrivalPaths<T> forMcRaptor(
    ParetoComparator<DestinationArrival<T>> arrivalComparator,
    RaptorTransitCalculator<T> transitCalculator,
    @Nullable RaptorCostCalculator<T> costCalculator,
    SlackProvider slackProvider,
    PathMapper<T> pathMapper,
    DebugHandlerFactory<T> debugHandlerFactory,
    RaptorStopNameResolver stopNameResolver,
    WorkerLifeCycle lifeCycle
  ) {
    var arrivals = ParetoSet.of(
      arrivalComparator,
      debugHandlerFactory.paretoSetDebugPathListener(toPath(pathMapper))
    );
    return new DestinationArrivalPaths<>(
      arrivals,
      null,
      transitCalculator,
      costCalculator,
      slackProvider,
      pathMapper,
      debugHandlerFactory,
      stopNameResolver,
      lifeCycle
    );
  }

  public void add(ArrivalView<T> stopArrival, RaptorAccessEgress egressPath) {
    var destArrival = createDestinationArrivalView(stopArrival, egressPath);

    if (destArrival == null) {
      return;
    }

    var errors = new ArrayList<String>();

    rejectArrivalIfItExceedsTimeLimit(destArrival).ifPresent(errors::add);

    if (errors.isEmpty()) {
      addDestinationArrivalToPaths(destArrival);
    } else {
      debugReject(destArrival, String.join(" ", errors));
    }
  }

  private void addDestinationArrivalToPaths(DestinationArrival<T> destArrival) {
    if (arrivals.add(destArrival)) {
      reachedCurrentRound = true;
      if (eagerPaths != null) {
        // StdRaptor: build path eagerly since stop-arrival state is mutable between iterations
        var path = pathMapper.mapToPath(destArrival);
        assertGeneralizedCostIsCalculatedCorrectByMapper(destArrival, path);
        eagerPaths.put(destArrival, path);
      }
    }
  }

  /**
   * Check if destination was reached in the current round.
   */
  public boolean isReachedCurrentRound() {
    return reachedCurrentRound;
  }

  public void setRangeRaptorIterationDepartureTime(int iterationDepartureTime) {
    this.iterationDepartureTime = iterationDepartureTime;
  }

  public boolean isEmpty() {
    return arrivals.isEmpty();
  }

  /**
   * Check whether the given arrival could produce a pareto-optimal result. Called by McRaptor's
   * heuristic pruning only — never called by StdRaptor.
   * <p>
   * <b>Do NOT replace this method with one creating a new {@link DestinationArrival} (or a
   * {@link RaptorPath}) per call.</b> It is called for every stop arrival in every round, so it is
   * one of the hottest code paths in McRaptor. The pareto set needs an element to compare with,
   * and an object created here is not reliably removed by the JVM's escape analysis (the
   * qualification check is too large to be fully inlined). The result is millions of short-lived
   * objects and a measurable slowdown of the search. Instead, the same primitive signature is
   * used and the values are written into a single reusable probe. The probe is never stored by
   * the pareto set.
   * <p>
   * <b>Thread-safety:</b> This method is NOT thread-safe, because the probe is shared by all
   * callers of this instance. This is fine as long as each instance is used by one thread only,
   * which is the case today. If a search is ever changed to call this method from several threads
   * (OTP has optimizations that run searches in parallel), create one probe per thread (for
   * example using a {@link ThreadLocal}, or one probe per worker) instead of sharing it. Do not
   * fall back to allocating a probe per call.
   */
  public boolean qualify(int arrivalTime, int numberOfTransfers, int cost, int c2) {
    probe.setProbeValues(iterationDepartureTime, arrivalTime, numberOfTransfers, cost, c2);
    return arrivals.qualify(probe);
  }

  public Collection<RaptorPath<T>> listPaths() {
    if (eagerPaths != null) {
      // StdRaptor: paths were built eagerly; look up by arrival identity
      return arrivals.stream().map(eagerPaths::get).toList();
    }
    // McRaptor: build paths lazily from stored arrivals
    return arrivals.stream().map(this::buildAndValidatePath).toList();
  }

  private RaptorPath<T> buildAndValidatePath(DestinationArrival<T> arrival) {
    var path = pathMapper.mapToPath(arrival);
    assertGeneralizedCostIsCalculatedCorrectByMapper(arrival, path);
    return path;
  }

  public void debugReject(ArrivalView<T> stopArrival, RaptorAccessEgress egress, String reason) {
    if (isDebugOn()) {
      var destinationArrival = createDestinationArrivalView(stopArrival, egress);
      if (destinationArrival != null) {
        debugReject(destinationArrival, reason);
      }
    }
  }

  public void debugReject(DestinationArrival<T> arrival, String reason) {
    if (isDebugOn()) {
      var path = pathMapper.mapToPath(arrival);
      debugPathHandler.reject(path, null, reason);
    }
  }

  @Override
  public String toString() {
    return arrivals.toString(DestinationArrival::toString);
  }

  public final boolean isDebugOn() {
    return debugPathHandler != null;
  }

  /* private methods */

  private void clearReachedCurrentRoundFlag() {
    reachedCurrentRound = false;
  }

  @Nullable
  private DestinationArrival<T> createDestinationArrivalView(
    ArrivalView<T> stopArrival,
    RaptorAccessEgress egressPath
  ) {
    int departureTime = transitCalculator.calculateEgressDepartureTime(
      stopArrival.arrivalTime(),
      egressPath,
      slackProvider.transferSlack()
    );
    if (departureTime == RaptorConstants.TIME_NOT_SET) {
      return null;
    }

    int arrivalTime = transitCalculator.plusDuration(departureTime, egressPath.durationInSeconds());

    int waitTimeInSeconds = Math.abs(departureTime - stopArrival.arrivalTime());

    int additionalCost;

    // If the aggregatedCost exist(McRaptor), then the cost of waiting is added.
    if (costCalculator != null) {
      additionalCost = egressPath.c1();
      additionalCost += costCalculator.waitCost(waitTimeInSeconds);
      additionalCost += costCalculator.costEgress(egressPath.stop(), egressPath.hasRides());
    }
    // If the aggregatedCost is zero(StdRaptor), then cost calculation is skipped.
    else {
      additionalCost = RaptorConstants.ZERO;
    }

    return new DestinationArrival<>(
      egressPath,
      stopArrival,
      iterationDepartureTime,
      arrivalTime,
      additionalCost,
      stopArrival.c2()
    );
  }

  /**
   * If the total cost generated by the mapper is not equal to the total cost calculated by Raptor,
   * there is probably a mistake in the mapper! This is a rather critical error and should be fixed.
   * To avoid dropping legal paths from the result set, we log this as an error and allow the path
   * to be included in the result!!!
   * <p>
   * The path mapper might not map the cost to each leg exactly as the Raptor does but the total
   * should be the same. Raptor only have stop-arrival, while the path have legs. A transit leg
   * alight BEFORE the transit stop arrival due to alight-slack.
   */
  private void assertGeneralizedCostIsCalculatedCorrectByMapper(
    DestinationArrival<T> destArrival,
    RaptorPath<T> path
  ) {
    if (path.c1() != destArrival.c1()) {
      THROTTLE_MISS_MATCH.throttle(() ->
        LOG.warn(
          "Cost mismatch - Mapper: {}, stop-arrivals: {}, path: {}  {}",
          OtpNumberFormat.formatCostCenti(path.c1()),
          raptorCostsAsString(destArrival),
          path.toStringDetailed(stopNameResolver),
          THROTTLE_MISS_MATCH.setupInfo()
        )
      );
    }
  }

  /**
   * Return the cost of all stop arrivals including the destination in reverse order:
   * <p>
   * {@code $1200 $950 $600} (Egress, bus, and access arrival)
   */
  private String raptorCostsAsString(DestinationArrival<T> destArrival) {
    var arrivalCosts = new ArrayList<String>();
    ArrivalView<?> it = destArrival;
    while (it != null) {
      arrivalCosts.add(OtpNumberFormat.formatCostCenti(it.c1()));
      it = it.previous();
    }
    // Remove decimals if zero
    return String.join(" ", arrivalCosts).replaceAll("\\.00", "");
  }

  private Optional<String> rejectArrivalIfItExceedsTimeLimit(ArrivalView<T> destArrival) {
    var egress = destArrival.egressPath().egress();
    int arrivalTime = transitCalculator.timeMinusPenalty(destArrival.arrivalTime(), egress);
    if (transitCalculator.exceedsTimeLimit(arrivalTime)) {
      return Optional.of(transitCalculator.exceedsTimeLimitReason());
    }
    return Optional.empty();
  }
}
