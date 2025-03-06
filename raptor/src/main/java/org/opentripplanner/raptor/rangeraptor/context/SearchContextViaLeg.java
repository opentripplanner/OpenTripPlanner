package org.opentripplanner.raptor.rangeraptor.context;

import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.transit.AccessPaths;
import org.opentripplanner.raptor.rangeraptor.transit.EgressPaths;
import org.opentripplanner.raptor.rangeraptor.transit.ViaConnections;

/**
 * A search can be split into one or more legs. The {@code parent} search context will have a list
 * of legs. The first leg will have a list of {@code accessPaths} and the {@link ViaConnections}
 * for transferring to the next leg. The last leg will have {@code egressPaths}.
 */
public class SearchContextViaLeg<T extends RaptorTripSchedule> {

  private final SearchContext<T> parent;
  private final AccessPaths accessPaths;

  @Nullable
  private final ViaConnections viaConnections;

  @Nullable
  private final EgressPaths egressPaths;

  public SearchContextViaLeg(
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
   * The parent search context this leg is part of.
   */
  public SearchContext<T> parent() {
    return parent;
  }

  /**
   * The set of access paths to be used to board this leg. This method returns an empty
   * set of access-paths if the leg is not the first leg. Hence, it is null-safe.
   */
  public AccessPaths accessPaths() {
    return accessPaths;
  }

  /**
   * The via connections for the via-location this leg ends with. This is {@code null} if this
   * leg is the last leg.
   */
  @Nullable
  public ViaConnections viaConnections() {
    return viaConnections;
  }

  /**
   * The egress path for search. Non-null if and only if this is the last leg.
   */
  @Nullable
  public EgressPaths egressPaths() {
    return egressPaths;
  }
}
