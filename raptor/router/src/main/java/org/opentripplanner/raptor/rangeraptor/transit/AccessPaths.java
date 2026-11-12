package org.opentripplanner.raptor.rangeraptor.transit;

import static org.opentripplanner.raptor.rangeraptor.transit.AccessEgressFunctions.groupByRound;
import static org.opentripplanner.raptor.rangeraptor.transit.AccessEgressFunctions.removeNonOptimalPathsForMcRaptor;
import static org.opentripplanner.raptor.rangeraptor.transit.AccessEgressFunctions.removeNonOptimalPathsForStandardRaptor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorStartOnBoardAccess;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.IntIterators;
import org.opentripplanner.raptor.spi.RaptorConstants;
import org.opentripplanner.raptor.spi.SearchDirection;
import org.opentripplanner.raptor.util.collection.IntIndexList;

/**
 * This class is responsible for performing Raptor-specific functionality on access-paths. It groups
 * paths based on number-of-trips(FLEX mainly) and stop-arrival "mode" (on-board or on-foot). This
 * is used to insert the access into the Raptor rounds at the correct moment (round), so the
 * number-of-transfers criteria become correct.
 * <p>
 * This class also provides an iterator to iterate over iteration steps in the Raptor algorithm
 * to cover extra minutes outside the search-window for access with a time-penalty.
 */
public class AccessPaths {

  private final int iterationStep;
  private final int maxNumberOfRides;
  private final int maxTimePenalty;
  private final IntUnaryOperator iterationOp;
  private final IntIndexList<RaptorAccessEgress> arrivedOnStreetByNumOfRides;
  private final IntIndexList<RaptorAccessEgress> arrivedOnBoardByNumOfRides;
  private final List<RaptorStartOnBoardAccess> startOnBoardAccessPaths;
  private int iterationTimePenaltyLimit = RaptorConstants.TIME_NOT_SET;

  private AccessPaths(
    int iterationStep,
    int maxNumberOfRides,
    int maxTimePenalty,
    IntUnaryOperator iterationOp,
    IntIndexList<RaptorAccessEgress> arrivedOnStreetByNumOfRides,
    IntIndexList<RaptorAccessEgress> arrivedOnBoardByNumOfRides,
    List<RaptorStartOnBoardAccess> startOnBoardAccessPaths
  ) {
    this.iterationStep = iterationStep;
    this.maxNumberOfRides = maxNumberOfRides;
    this.maxTimePenalty = maxTimePenalty;
    this.iterationOp = iterationOp;
    this.arrivedOnStreetByNumOfRides = arrivedOnStreetByNumOfRides;
    this.arrivedOnBoardByNumOfRides = arrivedOnBoardByNumOfRides;
    this.startOnBoardAccessPaths = startOnBoardAccessPaths;
  }

  /**
   * The multi-criteria state can handle multiple access/egress paths to a single stop, but the
   * Standard and BestTime states do not. To get a deterministic behavior, we filter the paths and
   * return the paths with the shortest duration for non-multi-criteria search. If two paths have
   * the same duration, the first one is picked. Note! If the access/egress paths contains flex as
   * well, then we need to look at mode for arriving at tha stop as well. A Flex arrive-on-board can
   * be used with a transfer even if the time is worse compared with walking.
   * <p>
   * This method is static and package local to enable unit-testing.
   */
  public static AccessPaths create(
    int iterationStep,
    Collection<RaptorAccessEgress> paths,
    RaptorProfile profile,
    SearchDirection searchDirection
  ) {
    var split = partitioningByStartOnBoard(paths);
    var onBoardAccessPaths = mapToStartOnBoardAccess(split.get(true));

    paths = split.get(false);

    if (profile.is(RaptorProfile.MULTI_CRITERIA)) {
      paths = removeNonOptimalPathsForMcRaptor(paths);
    } else {
      paths = removeNonOptimalPathsForStandardRaptor(paths);
    }

    paths = decorateWithTimePenaltyLogic(paths);
    var arrivedOnStreetByNumOfRides = groupByRound(paths, RaptorAccessEgress::arrivedOnStreet);
    var arrivedOnBoardByNumOfRides = groupByRound(paths, RaptorAccessEgress::arrivedOnBoard);
    int maxNumberOfRides = Math.max(
      maxNumberOfRounds(arrivedOnStreetByNumOfRides),
      maxNumberOfRounds(arrivedOnBoardByNumOfRides)
    );
    int maxTimePenalty = Math.max(
      maxTimePenalty(arrivedOnStreetByNumOfRides),
      maxTimePenalty(arrivedOnBoardByNumOfRides)
    );

    return new AccessPaths(
      iterationStep,
      maxNumberOfRides,
      maxTimePenalty,
      iterationOp(searchDirection),
      arrivedOnStreetByNumOfRides,
      arrivedOnBoardByNumOfRides,
      onBoardAccessPaths
    );
  }

  /**
   * Return the transfer arriving at the stop on-street(walking) grouped by Raptor round. The Raptor
   * round is calculated from the number of rides in the transfer.
   * <p>
   * If no access exists for the given round, an empty list is returned.
   */
  public List<RaptorAccessEgress> arrivedOnStreetByNumOfRides(int round) {
    return filterOnTimePenaltyLimitIfExist(arrivedOnStreetByNumOfRides.get(round));
  }

  /**
   * Return the transfer arriving at the stop on-board a transit(flex) service grouped by Raptor
   * round. The Raptor round is calculated from the number of rides in the transfer.
   * <p>
   * If no access exists for the given round, an empty list is returned.
   */
  public List<RaptorAccessEgress> arrivedOnBoardByNumOfRides(int round) {
    return filterOnTimePenaltyLimitIfExist(arrivedOnBoardByNumOfRides.get(round));
  }

  public Iterable<RaptorAccessEgress> arrivedOnBoard() {
    return arrivedOnBoardByNumOfRides.elements();
  }

  /**
   * Return the on-board accesses
   */
  public List<RaptorStartOnBoardAccess> listStartOnBoardAccesses() {
    return startOnBoardAccessPaths;
  }

  public int maxNumberOfRides() {
    return maxNumberOfRides;
  }

  /**
   * This is used in the main "minutes" iteration to iterate over the extra minutes needed to
   * include access with time-penalty. This method returns an iterator for the minutes in front of
   * the normal search window starting at the given {@code earliestDepartureTime}.
   */
  public IntIterator iterateOverPathsWithPenalty(final int earliestDepartureTime) {
    if (!hasTimePenalty()) {
      return IntIterators.empty();
    }
    // In the first iteration, we want the time-limit to be zero and the raptor-iteration-time
    // to be one step before the earliest-departure-time in the search-window. This will include
    // all access with a penalty in the first iteration. Then:
    this.iterationTimePenaltyLimit = -iterationStep;
    final int raptorIterationStartTime = earliestDepartureTime - signedIterationStep(iterationStep);

    return new IntIterator() {
      @Override
      public boolean hasNext() {
        return AccessPaths.this.iterationTimePenaltyLimit + iterationStep < maxTimePenalty;
      }

      @Override
      public int next() {
        AccessPaths.this.iterationTimePenaltyLimit += iterationStep;
        return (
          raptorIterationStartTime - signedIterationStep(AccessPaths.this.iterationTimePenaltyLimit)
        );
      }
    };
  }

  /** Raptor uses this information to optimize boarding of the first trip */
  public boolean hasTimeDependentAccess() {
    return (
      hasTimeDependentAccess(arrivedOnBoardByNumOfRides) ||
      hasTimeDependentAccess(arrivedOnStreetByNumOfRides)
    );
  }

  public AccessPaths filterOnSegment(int segment) {
    return new AccessPaths(
      iterationStep,
      maxNumberOfRides,
      maxTimePenalty,
      iterationOp,
      AccessEgressFunctions.filterOnSegment(arrivedOnStreetByNumOfRides, segment),
      AccessEgressFunctions.filterOnSegment(arrivedOnBoardByNumOfRides, segment),
      AccessEgressFunctions.filterOnSegment(startOnBoardAccessPaths, segment)
    );
  }

  /* private methods */

  private static int maxTimePenalty(IntIndexList<RaptorAccessEgress> col) {
    return col
      .stream()
      .mapToInt(RaptorAccessEgress::timePenalty)
      .max()
      .orElse(RaptorConstants.TIME_NOT_SET);
  }

  private static int maxNumberOfRounds(IntIndexList<RaptorAccessEgress> col) {
    return Math.max(0, col.maxIndex());
  }

  /**
   * Decorate access to implement time-penalty. This decoration will do the necessary adjustments to
   * apply the penalty in the raptor algorithm. See the decorator class for more info. The original
   * access object is returned if it does not have a time-penalty set.
   */
  private static List<RaptorAccessEgress> decorateWithTimePenaltyLogic(
    Collection<RaptorAccessEgress> paths
  ) {
    return paths
      .stream()
      .map(it -> it.hasTimePenalty() ? new AccessWithPenalty(it) : it)
      .toList();
  }

  private boolean hasTimePenalty() {
    return maxTimePenalty != RaptorConstants.TIME_NOT_SET;
  }

  private static boolean hasTimeDependentAccess(IntIndexList<RaptorAccessEgress> index) {
    return index.stream().anyMatch(RaptorAccessEgress::hasOpeningHours);
  }

  private static List<RaptorStartOnBoardAccess> mapToStartOnBoardAccess(
    List<RaptorAccessEgress> startOnBoardPaths
  ) {
    return startOnBoardPaths.stream().map(RaptorStartOnBoardAccess.class::cast).toList();
  }

  /// This method partition the input paths in two, the RaptorStartOnBoardAccess(key=true) in one
  /// list and the rest in the other.
  ///
  /// Start on board access paths can not be filtered up-front to remove non-optimal accesses, and
  /// also cannot have time penalties, so we extract them from paths before pressessing the rest.
  private static Map<Boolean, List<RaptorAccessEgress>> partitioningByStartOnBoard(
    Collection<RaptorAccessEgress> paths
  ) {
    return paths
      .stream()
      .collect(Collectors.partitioningBy(RaptorStartOnBoardAccess.class::isInstance));
  }

  /**
   * This method will filter the given list of access using the time-penalty-limit - if the limit is
   * set. If the limit is not set, then the given list is returned. This only filters the list after
   * all iterations are done, and we start iterating over the vertual minutes to allow itineraries
   * with a time-penalty to be included, even if the start before the search-window.
   */
  private List<RaptorAccessEgress> filterOnTimePenaltyLimitIfExist(List<RaptorAccessEgress> list) {
    if (iterationTimePenaltyLimit != RaptorConstants.TIME_NOT_SET) {
      return list
        .stream()
        .filter(e -> e.timePenalty() > iterationTimePenaltyLimit)
        .toList();
    }
    return list;
  }

  /**
   * When searching forward, we step back in time (minus), and when searching in reverse, we step
   * forward in time (plus).
   */
  private int signedIterationStep(int value) {
    return iterationOp.applyAsInt(value);
  }

  /**
   * We step back/forward in Raptor depending on the search direction.
   *
   * @see #signedIterationStep(int)
   */
  private static IntUnaryOperator iterationOp(SearchDirection searchDirection) {
    return searchDirection.isForward() ? (int step) -> step : (int step) -> -step;
  }
}
