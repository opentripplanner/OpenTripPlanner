package org.opentripplanner.transit.transfer.regular.spi;

import java.util.Objects;
import org.opentripplanner.core.model.id.FeedScopedId;

/**
 * A path {@code P} found by {@link TransferSearch#findNearbyStops}, together with the stop it
 * arrives at and the {@link PathCriteria} it was found under. {@code P} itself stays fully opaque
 * to the regular-transfer module; the stop and the criteria are available without re-costing the
 * path with {@link TransferSearch#computePathCriteria}.
 */
public final class TransferPath<P> {

  private final FeedScopedId toStop;
  private final P path;
  private final PathCriteria criteria;

  /**
   *
   */
  public TransferPath(FeedScopedId toStop, P path, PathCriteria criteria) {
    this.toStop = toStop;
    this.path = path;
    this.criteria = criteria;
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
      Objects.equals(this.criteria, that.criteria)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(toStop, path, criteria);
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
      ']'
    );
  }
}
