package org.opentripplanner.raptor.api.path;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

/**
 * Represent an access leg in a path. The access leg is the first leg from origin to the first
 * transit leg. The next leg must be a transit leg - no other legs are allowed.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class AccessPathLeg<T extends RaptorTripSchedule> implements PathLeg<T> {

  private final RaptorAccessEgress access;
  private final int fromTime;
  private final int toTime;
  private final int c1;
  private final PathLeg<T> next;

  public AccessPathLeg(
    @Nonnull RaptorAccessEgress access,
    int fromTime,
    int toTime,
    int c1,
    @Nonnull PathLeg<T> next
  ) {
    this.access = access;
    this.fromTime = fromTime;
    this.toTime = toTime;
    this.c1 = c1;
    this.next = next;
  }

  @Override
  public int fromTime() {
    return fromTime;
  }

  @Override
  public int toTime() {
    return toTime;
  }

  /**
   * The stop index where the leg end, also called arrival stop index.
   */
  @Override
  public int toStop() {
    return access.stop();
  }

  @Override
  public int c1() {
    return c1;
  }

  @Override
  public boolean isAccessLeg() {
    return true;
  }

  @Override
  public PathLeg<T> nextLeg() {
    return next;
  }

  public RaptorAccessEgress access() {
    return access;
  }

  @Override
  public int hashCode() {
    return Objects.hash(fromTime, toStop(), toTime, next);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AccessPathLeg<?> that = (AccessPathLeg<?>) o;
    return (
      fromTime == that.fromTime &&
      toStop() == that.toStop() &&
      toTime == that.toTime &&
      next.equals(that.next)
    );
  }

  @Override
  public String toString() {
    return "Access " + asString(toStop());
  }
}
