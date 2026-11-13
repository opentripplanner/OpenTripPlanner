package org.opentripplanner.raptor.rangeraptor.transit;

import static org.opentripplanner.raptor.api.request.RaptorProfile.MULTI_CRITERIA;
import static org.opentripplanner.raptor.rangeraptor.transit.AccessEgressFunctions.groupByStop;
import static org.opentripplanner.raptor.rangeraptor.transit.AccessEgressFunctions.removeNonOptimalPathsForMcRaptor;
import static org.opentripplanner.raptor.rangeraptor.transit.AccessEgressFunctions.removeNonOptimalPathsForStandardRaptor;

import com.carrotsearch.hppc.IntObjectMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.request.RaptorProfile;

public class EgressPaths {

  private final IntObjectMap<List<RaptorAccessEgress>> pathsByStop;

  private EgressPaths(IntObjectMap<List<RaptorAccessEgress>> pathsByStop) {
    this.pathsByStop = pathsByStop;
  }

  /**
   * The multi-criteria state can handle multiple access/egress paths to a single stop, but the
   * Standard and BestTime states do not. To get a deterministic behaviour we filter the paths and
   * return the paths with the shortest duration for non-multi-criteria search. If two paths have
   * the same duration the first one is picked. Note! If the access/egress paths contains flex as
   * well, then we need to look at mode for arriving at that stop as well. A Flex arrive-on-board
   * can be used with a transfer even if the time is worse compared with walking.
   * <p>
   * This method is static and package local to enable unit-testing.
   */
  public static EgressPaths create(Collection<RaptorAccessEgress> paths, RaptorProfile profile) {
    paths = decorateWithTimePenaltyLogic(paths);

    if (MULTI_CRITERIA.is(profile)) {
      paths = removeNonOptimalPathsForMcRaptor(paths);
    } else {
      paths = removeNonOptimalPathsForStandardRaptor(paths);
    }
    return new EgressPaths(groupByStop(paths));
  }

  public IntObjectMap<List<RaptorAccessEgress>> byStop() {
    return pathsByStop;
  }

  public int[] stops() {
    return pathsByStop.keys().toArray();
  }

  public Collection<RaptorAccessEgress> listAll() {
    return allPaths().collect(Collectors.toList());
  }

  /**
   * List all stops with an egress path which start by walking. These egress paths can only be used
   * if arriving at the stop by transit.
   */
  public int[] egressesWitchStartByWalking() {
    return filterPathsAndGetStops(RaptorAccessEgress::arrivedOnStreet);
  }

  /**
   * List all stops with an egress path which start on-board a "transit" ride. These egress paths
   * can be used when arriving at the stop with both transfer or transit.
   */
  public int[] egressesWitchStartByARide() {
    return filterPathsAndGetStops(RaptorAccessEgress::arrivedOnBoard);
  }

  public EgressPaths filterOnSegment(int segment) {
    return new EgressPaths(AccessEgressFunctions.filterOnSegment(pathsByStop, segment));
  }

  /**
   * Decorate egress to implement time-penalty. This decoration will do the necessary adjustments to
   * apply the penalty in the raptor algorithm. See the decorator class for more info. The original
   * egress object is returned if it does not have a time-penalty set.
   */
  private static List<RaptorAccessEgress> decorateWithTimePenaltyLogic(
    Collection<RaptorAccessEgress> paths
  ) {
    return paths
      .stream()
      .map(it -> it.hasTimePenalty() ? new EgressWithPenalty(it) : it)
      .toList();
  }

  private int[] filterPathsAndGetStops(Predicate<RaptorAccessEgress> filter) {
    return allPaths().filter(filter).mapToInt(RaptorAccessEgress::stop).distinct().toArray();
  }

  private Stream<RaptorAccessEgress> allPaths() {
    var result = new ArrayList<RaptorAccessEgress>();
    for (var cursor : pathsByStop.values()) {
      result.addAll(cursor.value);
    }
    return result.stream();
  }
}
