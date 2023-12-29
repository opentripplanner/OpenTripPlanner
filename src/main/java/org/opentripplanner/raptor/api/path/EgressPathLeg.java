package org.opentripplanner.raptor.api.path;

import java.util.Objects;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

/**
 * Represent a egress leg in a path. The egress leg is the last leg arriving at the destination. The
 * previous leg must be a transit leg - no other legs are allowed.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class EgressPathLeg<T extends RaptorTripSchedule> implements PathLeg<T> {

  private final RaptorAccessEgress egress;
  private final int fromTime;
  private final int toTime;
  private final int c1;

  public EgressPathLeg(RaptorAccessEgress egress, int fromTime, int toTime, int c1) {
    this.egress = egress;
    this.fromTime = fromTime;
    this.toTime = toTime;
    this.c1 = c1;
  }

  @Override
  public int fromTime() {
    return fromTime;
  }

  /**
   * The stop index where the leg start, also called the leg departure stop index.
   */
  @Override
  public int fromStop() {
    return egress.stop();
  }

  @Override
  public int toTime() {
    return toTime;
  }

  @Override
  public int c1() {
    return c1;
  }

  @Override
  public boolean isEgressLeg() {
    return true;
  }

  /**
   * @throws UnsupportedOperationException - an egress leg is the last leg in a path and does not
   *                                       have a next leg.
   */
  @Override
  public TransitPathLeg<T> nextLeg() {
    throw new java.lang.UnsupportedOperationException(
      "The egress leg is the last leg in a path. Use isEgressLeg() to identify last leg."
    );
  }

  public RaptorAccessEgress egress() {
    return egress;
  }

  @Override
  public int hashCode() {
    return Objects.hash(fromStop(), fromTime, toTime);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EgressPathLeg<?> that = (EgressPathLeg<?>) o;
    return fromStop() == that.fromStop() && fromTime == that.fromTime && toTime == that.toTime;
  }

  @Override
  public String toString() {
    return "Egress " + asString();
  }
}
