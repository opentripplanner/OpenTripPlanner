package org.opentripplanner.raptor.rangeraptor.context;

import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.transit.AccessPaths;
import org.opentripplanner.raptor.rangeraptor.transit.EgressPaths;
import org.opentripplanner.raptor.rangeraptor.transit.ViaConnections;

/**
 * Represents the configuration for a search that is split into one or more segments, with a via
 * point between each segment.
 * <p>
 * The {@code parent} search context holds a list of segment contexts, one for each segment. The
 * access and egress paths are assigned to the appropriate segment based on how many via points
 * they pass through, as given by
 * {@link RaptorAccessEgress#numberOfViaLocationsVisited()}.
 * <p>
 * If an access or egress path does not visit any via locations, then:
 * <ul>
 *   <li>all such {@code accessPaths} are added to the first segment, and</li>
 *   <li>all such {@code egressPaths} are added to the last segment.</li>
 * </ul>
 * <p>
 * Each segment is linked to the next by copying stop arrivals at via stops from one segment to the
 * following segment. This is implemented using Pareto set event listeners.
 * </p>
 */
public class SearchContextViaSegments<T extends RaptorTripSchedule> {

  private final SearchContext<T> parent;
  private final AccessPaths accessPaths;

  @Nullable
  private final ViaConnections viaConnections;

  private final EgressPaths egressPaths;

  public SearchContextViaSegments(
    SearchContext<T> parent,
    AccessPaths accessPaths,
    @Nullable ViaConnections viaConnections,
    EgressPaths egressPaths
  ) {
    this.parent = Objects.requireNonNull(parent);
    this.accessPaths = Objects.requireNonNull(accessPaths);
    this.viaConnections = viaConnections;
    this.egressPaths = Objects.requireNonNull(egressPaths);
  }

  /**
   * Returns the parent search context that this segment belongs to.
   */
  public SearchContext<T> parent() {
    return parent;
  }

  /**
   * Returns the set of access paths to be used to board in this segment. This method always
   * returns a non-null instance; if there are no access paths, the set is simply empty.
   */
  public AccessPaths accessPaths() {
    return accessPaths;
  }

  /**
   * Returns the via connections for the via location at which this segment ends, or {@code null}
   * if this is the last segment.
   */
  @Nullable
  public ViaConnections viaConnections() {
    return viaConnections;
  }

  /**
   * Returns the set of egress paths that lead directly to the destination in this segment.
   */
  @Nullable
  public EgressPaths egressPaths() {
    return egressPaths;
  }
}
