package org.opentripplanner.raptor.rangeraptor.transit;

import static org.opentripplanner.raptor.rangeraptor.transit.AccessEgressFunctions.groupByStop;
import static org.opentripplanner.raptor.rangeraptor.transit.AccessEgressFunctions.removeNoneOptimalPathsForStandardRaptor;

import gnu.trove.map.TIntObjectMap;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;

public class EgressPaths {

  private final TIntObjectMap<List<RaptorAccessEgress>> pathsByStop;

  private EgressPaths(TIntObjectMap<List<RaptorAccessEgress>> pathsByStop) {
    this.pathsByStop = pathsByStop;
  }

  public TIntObjectMap<List<RaptorAccessEgress>> byStop() {
    return pathsByStop;
  }

  public int[] stops() {
    return pathsByStop.keys();
  }

  public Collection<RaptorAccessEgress> listAll() {
    return pathsByStop
      .valueCollection()
      .stream()
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
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
  public static EgressPaths create(Collection<RaptorAccessEgress> paths, RaptorProfile profile) {
    if (!profile.is(RaptorProfile.MULTI_CRITERIA)) {
      paths = removeNoneOptimalPathsForStandardRaptor(paths);
    }
    return new EgressPaths(groupByStop(paths));
  }
}
