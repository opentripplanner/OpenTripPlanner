package org.opentripplanner.raptor.api.request;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorStopNameResolver;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.utils.lang.IntUtils;
import org.opentripplanner.utils.time.DurationUtils;

/**
 * Defines a via location which Raptor will force the path through. The concrete location is
 * called a connection. A location must have at least one connection, but can have more than
 * on alternative. Raptor will force the path through one of the connections. So, if there
 * are two connections, stop A and B, then Raptor will force the path through A or B. If the
 * path goes through A, it may or may not go through B.
 */
public final class RaptorViaLocation {

  private static final int MAX_WAIT_TIME_LIMIT = (int) Duration.ofHours(24).toSeconds();

  private final String label;
  private final boolean allowPassThrough;
  private final int minimumWaitTime;
  private final List<RaptorViaConnection> connections;

  private RaptorViaLocation(
    String label,
    boolean allowPassThrough,
    Duration minimumWaitTime,
    List<StopAndTransfer> connections
  ) {
    this.label = label;
    this.allowPassThrough = allowPassThrough;
    this.minimumWaitTime =
      IntUtils.requireInRange(
        (int) minimumWaitTime.toSeconds(),
        RaptorConstants.ZERO,
        MAX_WAIT_TIME_LIMIT,
        "minimumWaitTime"
      );
    this.connections = validateConnections(connections);

    if (allowPassThrough && this.minimumWaitTime > RaptorConstants.ZERO) {
      throw new IllegalArgumentException("Pass-through and min-wait-time is not allowed.");
    }
  }

  /**
   * Force the path through a set of stops, either on-board or as an alight or board stop.
   */
  public static Builder allowPassThrough(@Nullable String label) {
    return new Builder(label, true, Duration.ZERO);
  }

  /**
   * Force the path through one of the listed connections. To visit a stop, the path must board or
   * alight transit at the given stop, on-board visits do not count, see
   * {@link #allowPassThrough(String)}.
   */
  public static Builder via(@Nullable String label) {
    return new Builder(label, false, Duration.ZERO);
  }

  /**
   * Force the path through one of the listed connections, and wait the given minimum-wait-time
   * before continuing. To visit a stop, the path must board or alight transit at the given stop,
   * on-board visits do not count, see {@link #allowPassThrough(String)}.
   */
  public static Builder via(@Nullable String label, Duration minimumWaitTime) {
    return new Builder(label, false, minimumWaitTime);
  }

  @Nullable
  public String label() {
    return label;
  }

  public boolean allowPassThrough() {
    return allowPassThrough;
  }

  public int minimumWaitTime() {
    return minimumWaitTime;
  }

  public List<RaptorViaConnection> connections() {
    return connections;
  }

  public String toString() {
    return toString(Integer::toString);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RaptorViaLocation that = (RaptorViaLocation) o;
    return (
      allowPassThrough == that.allowPassThrough &&
      minimumWaitTime == that.minimumWaitTime &&
      Objects.equals(label, that.label) &&
      Objects.equals(connections, that.connections)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(label, allowPassThrough, minimumWaitTime, connections);
  }

  public String toString(RaptorStopNameResolver stopNameResolver) {
    var buf = new StringBuilder("Via{");
    if (label != null) {
      buf.append("label: ").append(label).append(", ");
    }
    if (allowPassThrough) {
      buf.append("allowPassThrough, ");
    }
    if (minimumWaitTime > RaptorConstants.ZERO) {
      buf.append("minWaitTime: ").append(DurationUtils.durationToStr(minimumWaitTime)).append(", ");
    }
    buf
      .append("connections: ")
      .append(connections.stream().map(it -> it.toString(stopNameResolver)).toList());
    return buf.append("}").toString();
  }

  private List<RaptorViaConnection> validateConnections(List<StopAndTransfer> connections) {
    if (connections.isEmpty()) {
      throw new IllegalArgumentException("At least one connection is required.");
    }
    var list = connections
      .stream()
      .map(it -> new RaptorViaConnection(this, it.fromStop, it.transfer))
      .toList();

    // Compare all pairs to check for duplicates and non-optimal connections
    for (int i = 0; i < list.size(); ++i) {
      var a = list.get(i);
      for (int j = i + 1; j < list.size(); ++j) {
        var b = list.get(j);
        if (a.isBetterOrEqual(b) || b.isBetterOrEqual(a)) {
          throw new IllegalArgumentException(
            "All connection need to be pareto-optimal: (" + a + ") <-> (" + b + ")"
          );
        }
      }
    }
    return list;
  }

  public static final class Builder {

    private final String label;
    private final boolean allowPassThrough;
    private final Duration minimumWaitTime;
    private final List<StopAndTransfer> connections = new ArrayList<>();

    public Builder(String label, boolean allowPassThrough, Duration minimumWaitTime) {
      this.label = label;
      this.allowPassThrough = allowPassThrough;
      this.minimumWaitTime = minimumWaitTime;
    }

    public Builder addViaStop(int stop) {
      this.connections.add(new StopAndTransfer(stop, null));
      return this;
    }

    public Builder addViaTransfer(int fromStop, RaptorTransfer transfer) {
      this.connections.add(new StopAndTransfer(fromStop, transfer));
      return this;
    }

    public RaptorViaLocation build() {
      return new RaptorViaLocation(label, allowPassThrough, minimumWaitTime, connections);
    }
  }

  /**
   * Use internally to store connection data, before creating the connection objects. If is
   * needed to create the bidirectional relationship between {@link RaptorViaLocation} and
   * {@link RaptorViaConnection}.
   */
  private record StopAndTransfer(int fromStop, @Nullable RaptorTransfer transfer) {}
}
