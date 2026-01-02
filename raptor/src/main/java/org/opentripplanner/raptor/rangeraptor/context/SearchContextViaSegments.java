package org.opentripplanner.raptor.rangeraptor.context;

import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.transit.AccessPaths;
import org.opentripplanner.raptor.rangeraptor.transit.EgressPaths;
import org.opentripplanner.raptor.rangeraptor.transit.ViaConnections;

/**
 * A search can be split into one or more segments with a via point between each segment. The
 * {@code parent} search context will have a list of contexts, one for each segment. The access and
 * egress paths are injected into the appropriate segment based on the number-of-via-points they
 * contains {@link RaptorAccessEgress#numberOfViaLocationsVisited()}. Tf the access and egress is
 * not visiting any via locations, then the first segment will have all {@code accessPaths}
 * included and the last segment will have all {@code egressPaths}.
 */
public class SearchContextViaSegments<T extends RaptorTripSchedule> {

  private final SearchContext<T> parent;
  private final AccessPaths accessPaths;

  @Nullable
  private final ViaConnections viaConnections;

  @Nullable
  private final EgressPaths egressPaths;

  public SearchContextViaSegments(
    SearchContext<T> parent,
    AccessPaths accessPaths,
    @Nullable ViaConnections viaConnections,
    @Nullable EgressPaths egressPaths
  ) {
    this.parent = parent;
    this.accessPaths = accessPaths;
    this.viaConnections = viaConnections;
    this.egressPaths = egressPaths;
  }

  /**
   * The parent search context this segment is part of.
   */
  public SearchContext<T> parent() {
    return parent;
  }

  /**
   * The set of access paths to be used to board this segment. This method returns an empty
   * set of access-paths. Hence, it is null-safe.
   */
  public AccessPaths accessPaths() {
    return accessPaths;
  }

  /**
   * The via connections for the via-location this segment ends with. This is {@code null} if this
   * segment is the last segment.
   */
  @Nullable
  public ViaConnections viaConnections() {
    return viaConnections;
  }

  /**
   * The set of egress paths to go stright to the destination for given the segemnt.
   */
  @Nullable
  public EgressPaths egressPaths() {
    return egressPaths;
  }
}
