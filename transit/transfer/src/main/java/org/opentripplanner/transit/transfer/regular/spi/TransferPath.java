package org.opentripplanner.transit.transfer.regular.spi;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.core.model.id.FeedScopedId;

/**
 * A path {@code P} found by {@link TransferPathProvider#findNearbyStops} together with the stop
 * it arrives at and the {@link PathCriteria} it was found under. {@code P} itself stays fully
 * opaque to {@code raptor-data} - {@code toStop} and {@code criteria} are the pieces of identity
 * the generator needs out of it directly, to compare candidates and store the transfer, without a
 * separate {@link TransferPathProvider#computePathCriteria} round trip for the profile that found
 * the path.
 */
public final class TransferPath<P> implements Serializable {

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
