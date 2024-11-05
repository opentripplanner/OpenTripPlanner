package org.opentripplanner.raptor.api.request;

import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorStopNameResolver;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.utils.time.DurationUtils;

/**
 * A via-connection is used to define one of the physical locations in a via location Raptor must
 * visit. At least one connection in a {@link RaptorViaLocation} must be used. A connection can be
 * a single stop or a stop and a transfer to another stop. The last is useful if you want to use
 * the connection to visit something other than a stop, like a street location.
 * This is not an alternative to transfers. Raptor supports several use-cases
 * through via-connections:
 *
 * <h4>Route via a pass-through-stop</h4>
 * Raptor will allow a path to go through a pass-through-stop. The stop can be visited on-board
 * transit, or at the alight- or board-stop. The from-stop and to-stop must be the same, and the
 * minimum-wait-time must be zero.
 *
 * <h4>Route via a single stop with a minimum-wait-time</h4>
 * Raptor will allow a path to go through a single stop, if the from-stop and to-stop is the
 * same. If the minimum-wait-time is greater than zero(0) the path will either alight or board
 * transit at this stop, and the minimum-wait-time criteria is enforced.
 *
 * <h4>Route via a coordinate</h4>
 *
 * To route through a coordinate you need to find all nearby stops, then find all access and egress
 * paths to and from the street location. Then combine all access and egress paths to form
 * complete transfers. Raptor does not know/see the actual via street location, it only uses the
 * connection from a stop to another, the total time it takes and the total cost. You must generate
 * a transfer with two "legs" in it. One leg going from the 'from-stop' to the street location, and
 * one leg going back to the 'to-stop'. If you have 10 stops around the via street location, then
 * you must combine all ten access paths and egress paths.
 *
 * The min-wait-time in the {@link RaptorViaLocation} is added to the transfers
 * {@code durationInSeconds}. The calculation of {@code c1} should include the walk time, but not
 * the min-wait-time (assuming all connections have the same minimum wait time).
 */
public final class RaptorViaConnection {

  private final int fromStop;
  private final int durationInSeconds;

  @Nullable
  private final RaptorTransfer transfer;

  RaptorViaConnection(RaptorViaLocation parent, int fromStop, @Nullable RaptorTransfer transfer) {
    this.fromStop = fromStop;
    this.transfer = transfer;
    this.durationInSeconds =
      parent.minimumWaitTime() +
      (transfer == null ? RaptorConstants.ZERO : transfer.durationInSeconds());
  }

  /**
   * Stop index where the connection starts.
   */
  public int fromStop() {
    return fromStop;
  }

  @Nullable
  public RaptorTransfer transfer() {
    return transfer;
  }

  /**
   * Stop index where the connection ends. This can be the same as the {@code fromStop}.
   */
  public int toStop() {
    return isSameStop() ? fromStop : transfer.stop();
  }

  /**
   * The time duration to walk or travel from the {@code fromStop} to the {@code toStop}.
   */
  public int durationInSeconds() {
    return durationInSeconds;
  }

  /**
   * The generalized cost of this via-connection in centi-seconds.
   * <p>
   * This method is called many times, so care needs to be taken that the value is stored, not
   * calculated for each invocation.
   */
  public int c1() {
    return isSameStop() ? RaptorConstants.ZERO : transfer.c1();
  }

  public boolean isSameStop() {
    return transfer == null;
  }

  /**
   * This method is used to check that all connections are unique/provide an optimal path.
   * The method returns {@code true} if this instance is better or equals to the given other
   * stop with respect to being pareto-optimal.
   */
  boolean isBetterOrEqual(RaptorViaConnection other) {
    if (fromStop != other.fromStop || toStop() != other.toStop()) {
      return false;
    }
    return durationInSeconds() <= other.durationInSeconds() && c1() <= other.c1();
  }

  /**
   * Only from and to stop is part of the equals/hashCode, duplicate connection between to stops
   * are not allowed.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RaptorViaConnection that = (RaptorViaConnection) o;
    return fromStop == that.fromStop && Objects.equals(transfer, that.transfer);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fromStop, transfer);
  }

  @Override
  public String toString() {
    return toString(Integer::toString);
  }

  public String toString(RaptorStopNameResolver stopNameResolver) {
    var buf = new StringBuilder(stopNameResolver.apply(fromStop));
    if (transfer != null) {
      buf.append("~").append(stopNameResolver.apply(toStop()));
    }
    int d = durationInSeconds();
    if (d > RaptorConstants.ZERO) {
      buf.append(" ").append(DurationUtils.durationToStr(d));
    }
    return buf.toString();
  }
}
