package org.opentripplanner.raptor.rangeraptor.transit;

import static org.opentripplanner.raptor.rangeraptor.transit.AccessEgressFunctions.groupByRound;
import static org.opentripplanner.raptor.rangeraptor.transit.AccessEgressFunctions.removeNonOptimalPathsForMcRaptor;
import static org.opentripplanner.raptor.rangeraptor.transit.AccessEgressFunctions.removeNonOptimalPathsForStandardRaptor;

import gnu.trove.map.TIntObjectMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.request.RaptorProfile;

public class AccessPaths {

  private final TIntObjectMap<List<RaptorAccessEgress>> arrivedOnStreetByNumOfRides;
  private final TIntObjectMap<List<RaptorAccessEgress>> arrivedOnBoardByNumOfRides;

  private AccessPaths(
    TIntObjectMap<List<RaptorAccessEgress>> arrivedOnStreetByNumOfRides,
    TIntObjectMap<List<RaptorAccessEgress>> arrivedOnBoardByNumOfRides
  ) {
    this.arrivedOnStreetByNumOfRides = arrivedOnStreetByNumOfRides;
    this.arrivedOnBoardByNumOfRides = arrivedOnBoardByNumOfRides;
  }

  /**
   * Return the transfer arriving at the stop on-street(walking) grouped by Raptor round. The Raptor
   * round is calculated from the number of rides in the transfer.
   */
  public TIntObjectMap<List<RaptorAccessEgress>> arrivedOnStreetByNumOfRides() {
    return arrivedOnStreetByNumOfRides;
  }

  /**
   * Return the transfer arriving at the stop on-board a transit(flex) service grouped by Raptor
   * round. The Raptor round is calculated from the number of rides in the transfer.
   */
  public TIntObjectMap<List<RaptorAccessEgress>> arrivedOnBoardByNumOfRides() {
    return arrivedOnBoardByNumOfRides;
  }

  public int calculateMaxNumberOfRides() {
    return Math.max(
      Arrays.stream(arrivedOnStreetByNumOfRides.keys()).max().orElse(0),
      Arrays.stream(arrivedOnBoardByNumOfRides.keys()).max().orElse(0)
    );
  }

  /**
   * The multi-criteria state can handle multiple access/egress paths to a single stop, but the
   * Standard and BestTime states do not. To get a deterministic behaviour we filter the paths and
   * return the paths with the shortest duration for non-multi-criteria search. If two paths have
   * the same duration the first one is picked. Note! If the access/egress paths contains flex as
   * well, then we need to look at mode for arriving at tha stop as well. A Flex arrive-on-board can
   * be used with a transfer even if the time is worse compared with walking.
   * <p>
   * This method is static and package local to enable unit-testing.
   */
  public static AccessPaths create(Collection<RaptorAccessEgress> paths, RaptorProfile profile) {
    if (profile.is(RaptorProfile.MULTI_CRITERIA)) {
      paths = removeNonOptimalPathsForMcRaptor(paths);
    } else {
      paths = removeNonOptimalPathsForStandardRaptor(paths);
    }
    return new AccessPaths(
      groupByRound(paths, RaptorAccessEgress::stopReachedByWalking),
      groupByRound(paths, RaptorAccessEgress::stopReachedOnBoard)
    );
  }

  /** Raptor uses this information to optimize boarding of the first trip */
  public boolean hasTimeDependentAccess() {
    return (
      hasTimeDependentAccess(arrivedOnBoardByNumOfRides) ||
      hasTimeDependentAccess(arrivedOnStreetByNumOfRides)
    );
  }

  private static boolean hasTimeDependentAccess(TIntObjectMap<List<RaptorAccessEgress>> map) {
    for (List<RaptorAccessEgress> list : map.valueCollection()) {
      if (list.stream().anyMatch(RaptorAccessEgress::hasOpeningHours)) {
        return true;
      }
    }
    return false;
  }
}
