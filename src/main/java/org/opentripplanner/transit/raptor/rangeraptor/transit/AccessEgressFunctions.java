package org.opentripplanner.transit.raptor.rangeraptor.transit;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import org.opentripplanner.transit.raptor.api.transit.AccessEgress;
import org.opentripplanner.transit.raptor.rangeraptor.internalapi.SlackProvider;
import org.opentripplanner.transit.raptor.util.paretoset.ParetoComparator;
import org.opentripplanner.transit.raptor.util.paretoset.ParetoSet;

/**
 * This class contains functions used by the {@link AccessPaths} and {@link EgressPaths} classes.
 * The functions are all static utility functions, and are placed in this class to be shared between
 * the two classes.
 * <p>
 * The class also contains the ParetoSet comparator to filter Standard Raptor(not multi-criteria)
 * access and egress.
 * <p>
 */
public final class AccessEgressFunctions {

  /**
   * Filter standard(not multi-criteria) Raptor access and egress paths. A path is pareto optimal
   * for a given stop if
   * <ol>
   *     <li>
   *         time duration is lower
   *     </li>
   *     <li>
   *         number of rides is lower
   *     </li>
   *     <li>
   *         reached the stop on-board, and not on foot. This is optimal because arriving on foot
   *         limits your options, you are not allowed to contnue on foot and transfer(walk) to
   *         a nearby stop.
   *     </li>
   *     <li>
   *         No opening hours is better than being restricted
   *     </li>
   * </ol>
   */
  private static final ParetoComparator<AccessEgress> STANDARD_COMPARATOR = (l, r) ->
    (l.stopReachedOnBoard() && !r.stopReachedOnBoard()) ||
    (!l.hasOpeningHours() && r.hasOpeningHours()) ||
    l.numberOfRides() < r.numberOfRides() ||
    l.durationInSeconds() < r.durationInSeconds();

  /** private constructor to prevent instantiation of utils class. */
  private AccessEgressFunctions() {}

  /**
   * This method helps with calculating the egress departure time. It will add transit slack (egress
   * leaves on-board) and then time-shift the egress.
   */
  public static int calculateEgressDepartureTime(
    int arrivalTime,
    AccessEgress egressPath,
    SlackProvider slackProvider,
    TimeCalculator timeCalculator
  ) {
    int departureTime = arrivalTime;

    if (egressPath.stopReachedOnBoard()) {
      departureTime = timeCalculator.plusDuration(departureTime, slackProvider.transferSlack());
    }
    if (timeCalculator.searchForward()) {
      return egressPath.earliestDepartureTime(departureTime);
    } else {
      return egressPath.latestArrivalTime(departureTime);
    }
  }

  static Collection<AccessEgress> removeNoneOptimalPathsForStandardRaptor(
    Collection<AccessEgress> paths
  ) {
    // To avoid too many items in the pareto set we first group the paths by stop,
    // for each stop we filter it down to the optimal pareto set. We could do this
    // for multi-criteria as well, but it is likely not so important. The focus for
    // the mc-set should be that the list of access/egress created in OTP should not
    // contain to many non-optimal paths.
    var mapByStop = groupByStop(paths);
    var set = new ParetoSet<>(STANDARD_COMPARATOR);
    Collection<AccessEgress> result = new ArrayList<>();

    mapByStop.forEachValue(list -> {
      set.clear();
      set.addAll(list);
      result.addAll(set);
      return true;
    });
    return result;
  }

  /**
   * Filter the given input keeping all elements satisfying the given include predicate. If the
   * {@code keepOne} flag is set only one raptor transfer is kept for each group of numOfRides.
   */
  static TIntObjectMap<List<AccessEgress>> groupByRound(
    Collection<AccessEgress> input,
    Predicate<AccessEgress> include
  ) {
    return groupBy(
      input.stream().filter(include).collect(Collectors.toList()),
      AccessEgress::numberOfRides
    );
  }

  static TIntObjectMap<List<AccessEgress>> groupByStop(Collection<AccessEgress> input) {
    return groupBy(input, AccessEgress::stop);
  }

  /* private methods */

  private static List<AccessEgress> getOrCreate(int key, TIntObjectMap<List<AccessEgress>> map) {
    if (!map.containsKey(key)) {
      map.put(key, new ArrayList<>());
    }
    return map.get(key);
  }

  private static TIntObjectMap<List<AccessEgress>> groupBy(
    Collection<AccessEgress> input,
    ToIntFunction<AccessEgress> groupBy
  ) {
    var mapByRound = new TIntObjectHashMap<List<AccessEgress>>();

    for (AccessEgress it : input) {
      getOrCreate(groupBy.applyAsInt(it), mapByRound).add(it);
    }
    return mapByRound;
  }
}
