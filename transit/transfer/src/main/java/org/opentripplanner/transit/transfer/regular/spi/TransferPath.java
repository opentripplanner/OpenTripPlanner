package org.opentripplanner.transit.transfer.regular.spi;

import java.util.Objects;
import java.util.Set;
import org.opentripplanner.core.model.id.FeedScopedId;

/**
 * A path {@code P} found by {@link TransferSearch#findNearbyStops}, together with the stop it
 * arrives at and the {@link PathCriteria} it was found under. {@code P} itself stays fully opaque
 * to the regular-transfer module; the stop and the criteria are available without re-costing the
 * path with {@link TransferSearch#computePathCriteria}.
 * <p>
 * The {@link #closableFacilityIds()} are what the path depends on, for example a part of the street
 * network that may be closed. The module only compares them and passes them back to
 * {@link TransferSearch#findNearbyStops} to find a path that does not depend on them.
 */
public final class TransferPath<P> {

  private final FeedScopedId toStop;
  private final P path;
  private final PathCriteria criteria;
  private final Set<String> closableFacilityIds;

  /** A path that does not depend on anything that can be avoided. */
  public TransferPath(FeedScopedId toStop, P path, PathCriteria criteria) {
    this(toStop, path, criteria, Set.of());
  }

  /**
   * @param closableFacilityIds the opaque ids of what the path depends on, empty if nothing. The
   *                            set is copied.
   */
  public TransferPath(
    FeedScopedId toStop,
    P path,
    PathCriteria criteria,
    Set<String> closableFacilityIds
  ) {
    this.toStop = toStop;
    this.path = path;
    this.criteria = criteria;
    this.closableFacilityIds = Set.copyOf(closableFacilityIds);
  }

  public FeedScopedId toStop() {
    return toStop;
  }

  public P path() {
    return path;
  }

  public PathCriteria criteria() {
    return criteria;
  }

  /**
   * The ids of what the path depends on. The path can not be used if any of them is unavailable,
   * and is not found by a search that avoids one of them. Empty if the path depends on nothing.
   */
  public Set<String> closableFacilityIds() {
    return closableFacilityIds;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (TransferPath) obj;
    return (
      Objects.equals(this.toStop, that.toStop) &&
      Objects.equals(this.path, that.path) &&
      Objects.equals(this.criteria, that.criteria) &&
      this.closableFacilityIds.equals(that.closableFacilityIds)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(toStop, path, criteria, closableFacilityIds);
  }

  @Override
  public String toString() {
    return (
      "TransferPath[" +
      "toStop=" +
      toStop +
      ", " +
      "path=" +
      path +
      ", " +
      "criteria=" +
      criteria +
      ", " +
      "closableFacilityIds=" +
      closableFacilityIds +
      ']'
    );
  }
}
