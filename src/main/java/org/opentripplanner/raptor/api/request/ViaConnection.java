package org.opentripplanner.raptor.api.request;

import java.time.Duration;
import java.util.Objects;
import org.opentripplanner.framework.lang.IntUtils;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorStopNameResolver;

/**
 * A via-connection is used to connect two stops during the raptor routing. The Raptor
 * implementation uses these connections to force the path through at least one connection per 
 * via-location. This is not an alternative to transfers. Raptor supports several use-cases through
 * via-connections:

 * <h4>Route via a single stop with a minimum-wait-time</h4>

 * Raptor will allow a path to go through a single stop, if the from- and to-stop is the same and
 * the {@code durationInSeconds} is at least one(1) second.

 * <h4>Route via a pass-through-stop</h4>

 * Raptor will allow a path to go through a pass-through-stop, if the {@code durationInSeconds} is
 * zero and the from and to stop is the same.

 * <h4>Route via a coordinate</h4>

 * To route through a coordinate you need to find all nearby stops, then calculate the "walk"
 * durations and produce the set of connection. If you want to spend a min-wait-time at the
 * coordinate, this time must be added to the {@code durationInSeconds}. The calculation of
 * {@code c1} need to include the walk time, but not the wait time (assuming all connections
 * have the same minimum wait time).
 */
public final class ViaConnection {

  private static final int MAX_WAIT_TIME_LIMIT = (int) Duration.ofHours(24).toSeconds();

  private final int fromStop;
  private final int toStop;
  private final int c1;
  private final int durationInSeconds;

  private ViaConnection(int fromStop, int toStop, int durationInSeconds, int c1) {
    this.fromStop = fromStop;
    this.toStop = toStop;
    // To transfer from one stop to another must take at least one second
    int minDuration = isSameStop() ? RaptorConstants.ZERO : 1;
    this.durationInSeconds =
      IntUtils.requireInRange(
        durationInSeconds,
        minDuration,
        MAX_WAIT_TIME_LIMIT,
        "durationInSeconds"
      );
    this.c1 = IntUtils.requireNotNegative(c1, "c1");
  }

  /**
   * Force the path through a stop, either on-board or as an alight or board stop.
   */
  public static ViaConnection passThroughStop(int stop) {
    return new ViaConnection(stop, stop, RaptorConstants.ZERO, RaptorConstants.ZERO);
  }

  /**
   * Force the path through a stop and wait at least the givan {@code duration} before continuing.
   * To visit the stop, the path must board or alight transit at the stop.
   */
  public static ViaConnection stop(int stop, Duration duration) {
    return new ViaConnection(stop, stop, (int) duration.getSeconds(), RaptorConstants.ZERO);
  }

  /**
   * Force a path through a user provided transfer. This is meant for supporting a coordinate as a
   * via point. The path will alight from transit at the {@code fromStop} and board transit at the
   * {@code toStop}.
   */
  public static ViaConnection stop(int fromStop, int toStop, Duration duration, int c1) {
    return new ViaConnection(fromStop, toStop, (int) duration.getSeconds(), c1);
  }

  /**
   * Stop index where the connection starts.
   */
  public int fromStop() {
    return fromStop;
  }

  /**
   * Stop index where the connection ends. This can be the same as the {@code fromStop}.
   */
  public int toStop() {
    return toStop;
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
    return c1;
  }

  /**
   * The path must visit
   */
  public boolean allowPassThrough() {
    return durationInSeconds == RaptorConstants.ZERO;
  }

  public boolean isSameStop() {
    return fromStop == toStop;
  }

  /**
   * This method is used to chack that all connections are unique/provide an optimal path.
   * If this connection is better than the other connection, the other connection can be dropped.
   * <p>
   * This is the same as being pareto-optimal.
   */
  boolean isBetterThan(ViaConnection other) {
    if (fromStop != other.fromStop || toStop != other.toStop) {
      return false;
    }
    return durationInSeconds <= other.durationInSeconds && c1 <= other.c1;
  }

  /**
   * Only from and to stop is part of the equals/hashCode, duplicate connection between to stops
   * are not allowed.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ViaConnection that = (ViaConnection) o;
    return (
      fromStop == that.fromStop &&
      toStop == that.toStop &&
      durationInSeconds == that.durationInSeconds &&
      c1 == that.c1
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(fromStop, toStop, durationInSeconds, c1);
  }

  @Override
  public String toString() {
    return toString(Integer::toString);
  }

  public String toString(RaptorStopNameResolver stopNameResolver) {
    if (allowPassThrough()) {
      return "PassThrough(" + stopNameResolver.apply(fromStop) + ")";
    }
    var buf = new StringBuilder("Via(");

    if (durationInSeconds > RaptorConstants.ZERO) {
      buf.append(DurationUtils.durationToStr(durationInSeconds())).append(" ");
    }

    buf.append(stopNameResolver.apply(fromStop));

    if (toStop != fromStop) {
      buf.append("~").append(stopNameResolver.apply(toStop));
    }
    return buf.append(")").toString();
  }
}
