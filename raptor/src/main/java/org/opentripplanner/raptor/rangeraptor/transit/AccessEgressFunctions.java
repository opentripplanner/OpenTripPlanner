package org.opentripplanner.raptor.rangeraptor.transit;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;
import org.opentripplanner.raptor.util.paretoset.ParetoSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final Logger LOG = LoggerFactory.getLogger(AccessEgressFunctions.class);

  /**
   * Filter standard (not multi-criteria) Raptor access and egress paths. A path is pareto optimal
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
   *         limits your options, you are not allowed to continue on foot and transfer(walk) to
   *         a nearby stop.
   *     </li>
   *     <li>
   *         No opening hours is better than being restricted
   *     </li>
   *     <li>
   *         If Both have opening hours, both need to be accepted
   *     </li>
   * </ol>
   */
  private static final ParetoComparator<RaptorAccessEgress> STANDARD_COMPARATOR = (l, r) ->
    ((l.stopReachedOnBoard() && !r.stopReachedOnBoard()) ||
      r.hasOpeningHours() ||
      l.numberOfRides() < r.numberOfRides() ||
      l.durationInSeconds() < r.durationInSeconds());

  /**
   * Filter Multi-criteria Raptor access and egress paths. This can be used to wash
   * access/egress paths - paths that are not optimal using this should not be passed into
   * Raptor - it is a bug.
   */
  private static final ParetoComparator<RaptorAccessEgress> MC_COMPARATOR = (l, r) ->
    STANDARD_COMPARATOR.leftDominanceExist(l, r) || l.c1() < r.c1();

  /** private constructor to prevent instantiation of utils class. */
  private AccessEgressFunctions() {}

  /**
   * Filter non-optimal paths away for the standard search. This method does not
   * look at the c1 value.
   */
  static Collection<RaptorAccessEgress> removeNonOptimalPathsForStandardRaptor(
    Collection<RaptorAccessEgress> paths
  ) {
    return removeNonOptimalPaths(paths, STANDARD_COMPARATOR);
  }

  /**
   * Filter non-optimal paths away for the multi-criteria search. This method should in theory
   * not remove any paths since the caller should not pass in duplicates, but it turns out that
   * this happens, so we do it.
   */
  static Collection<RaptorAccessEgress> removeNonOptimalPathsForMcRaptor(
    Collection<RaptorAccessEgress> paths
  ) {
    var result = removeNonOptimalPaths(paths, MC_COMPARATOR);
    if (LOG.isDebugEnabled() && result.size() < paths.size()) {
      var duplicates = new ArrayList<>(paths);
      duplicates.removeAll(result);
      // Note! This does not provide enough information to solve/debug this problem, but this is
      // not a problem in Raptor, so we do not want to add more specific logging here - this does
      // however document that the problem exist. Turn on debug logging and move the start/end
      // coordinate around until you see this message.
      //
      // See https://github.com/opentripplanner/OpenTripPlanner/issues/5601
      LOG.warn(
        "Duplicate access/egress paths passed into raptor:\n\t" +
        duplicates.stream().map(Objects::toString).collect(Collectors.joining("\n\t"))
      );
    }
    return result;
  }

  /**
   * Filter the given input keeping all elements satisfying the given include predicate. If the
   * {@code keepOne} flag is set only one raptor transfer is kept for each group of numOfRides.
   */
  static TIntObjectMap<List<RaptorAccessEgress>> groupByRound(
    Collection<RaptorAccessEgress> input,
    Predicate<RaptorAccessEgress> include
  ) {
    return groupBy(
      input.stream().filter(include).collect(Collectors.toList()),
      RaptorAccessEgress::numberOfRides
    );
  }

  static TIntObjectMap<List<RaptorAccessEgress>> groupByStop(Collection<RaptorAccessEgress> input) {
    return groupBy(input, RaptorAccessEgress::stop);
  }

  /* private methods */

  /**
   * Remove relevant access/egress paths. The given set of paths are grouped by stop and
   * the filtered based on the given pareto comparator.
   */
  static Collection<RaptorAccessEgress> removeNonOptimalPaths(
    Collection<RaptorAccessEgress> paths,
    ParetoComparator<RaptorAccessEgress> comparator
  ) {
    var mapByStop = groupByStop(paths);
    var set = new ParetoSet<>(comparator);
    var result = new ArrayList<RaptorAccessEgress>();

    for (int stop : mapByStop.keys()) {
      var list = mapByStop.get(stop);
      set.clear();
      set.addAll(list);
      result.addAll(set);
    }
    return result;
  }

  private static List<RaptorAccessEgress> getOrCreate(
    int key,
    TIntObjectMap<List<RaptorAccessEgress>> map
  ) {
    if (!map.containsKey(key)) {
      map.put(key, new ArrayList<>());
    }
    return map.get(key);
  }

  private static TIntObjectMap<List<RaptorAccessEgress>> groupBy(
    Collection<RaptorAccessEgress> input,
    ToIntFunction<RaptorAccessEgress> groupBy
  ) {
    var mapByRound = new TIntObjectHashMap<List<RaptorAccessEgress>>();

    for (RaptorAccessEgress it : input) {
      getOrCreate(groupBy.applyAsInt(it), mapByRound).add(it);
    }
    return mapByRound;
  }
}
