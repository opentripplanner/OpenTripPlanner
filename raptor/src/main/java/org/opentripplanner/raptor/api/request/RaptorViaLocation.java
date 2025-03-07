package org.opentripplanner.raptor.api.request;

import java.time.Duration;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
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

  private static final Duration MAX_WAIT_TIME = Duration.ofHours(24);
  private static final Duration MIN_WAIT_TIME = Duration.ZERO;

  private final String label;
  private final boolean passThroughSearch;
  private final int minimumWaitTime;
  private final List<RaptorViaConnection> connections;

  private RaptorViaLocation(
    String label,
    boolean passThroughSearch,
    Duration minimumWaitTime,
    List<BuilderStopAndTransfer> connections
  ) {
    this.label = label;
    this.passThroughSearch = passThroughSearch;
    this.minimumWaitTime = IntUtils.requireInRange(
      (int) minimumWaitTime.toSeconds(),
      (int) MIN_WAIT_TIME.toSeconds(),
      (int) MAX_WAIT_TIME.toSeconds(),
      "minimumWaitTime"
    );
    this.connections = validateConnections(connections);
  }

  /**
   * Force the path through a set of stops, either on-board or as an alight or board stop.
   */
  public static PassThroughBuilder passThrough(@Nullable String label) {
    return new PassThroughBuilder(label);
  }

  /**
   * Force the path through one of the listed connections. To visit a stop, the path must board or
   * alight transit at the given stop, on-board visits do not count, see
   * {@link #passThrough(String)}.
   */
  public static ViaVisitBuilder via(@Nullable String label) {
    return via(label, MIN_WAIT_TIME);
  }

  /**
   * Force the path through one of the listed connections, and wait the given minimum-wait-time
   * before continuing. To visit a stop, the path must board or alight transit at the given stop,
   * on-board visits do not count, see {@link #passThrough(String)}.
   */
  public static ViaVisitBuilder via(@Nullable String label, Duration minimumWaitTime) {
    return new ViaVisitBuilder(label, minimumWaitTime);
  }

  @Nullable
  public String label() {
    return label;
  }

  public boolean isPassThroughSearch() {
    return passThroughSearch;
  }

  public int minimumWaitTime() {
    return minimumWaitTime;
  }

  public List<RaptorViaConnection> connections() {
    return connections;
  }

  /**
   * This is a convenient accessor method used inside Raptor. It converts the list stops to a
   * bit-set. Add other access methods if needed.
   */
  public BitSet asBitSet() {
    return connections
      .stream()
      .mapToInt(RaptorViaConnection::fromStop)
      .collect(BitSet::new, BitSet::set, BitSet::or);
  }

  @Override
  public boolean equals(Object o) {
    throw new UnsupportedOperationException("No need to compare " + getClass());
  }

  @Override
  public int hashCode() {
    throw new UnsupportedOperationException("No need for hashCode of " + getClass());
  }

  @Override
  public String toString() {
    return toString(Integer::toString);
  }

  public String toString(RaptorStopNameResolver stopNameResolver) {
    var buf = new StringBuilder(getClass().getSimpleName()).append('{');
    buf.append(isPassThroughSearch() ? "pass-through " : "via ");
    if (label != null) {
      buf.append(label).append(" ");
    }
    if (minimumWaitTime > MIN_WAIT_TIME.toSeconds()) {
      buf.append("wait ").append(DurationUtils.durationToStr(minimumWaitTime)).append(" ");
    }
    buf
      .append(connections.size() <= 10 ? ": " : "(10/" + connections.size() + "): ")
      .append(connections.stream().limit(10).map(it -> it.toString(stopNameResolver)).toList());
    return buf.append("}").toString();
  }

  private List<RaptorViaConnection> validateConnections(List<BuilderStopAndTransfer> connections) {
    if (connections.isEmpty()) {
      throw new IllegalArgumentException("At least one connection is required.");
    }
    var list = connections
      .stream()
      .map(it -> RaptorViaConnection.of(this, it.fromStop, it.transfer))
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

  public abstract static sealed class AbstractBuilder<T extends AbstractBuilder> {

    protected final String label;
    protected final List<BuilderStopAndTransfer> connections = new ArrayList<>();

    public AbstractBuilder(String label) {
      this.label = label;
    }

    T addConnection(int stop, @Nullable RaptorTransfer transfer) {
      this.connections.add(new BuilderStopAndTransfer(stop, transfer));
      return (T) this;
    }
  }

  public static final class ViaVisitBuilder extends AbstractBuilder<ViaVisitBuilder> {

    private final Duration minimumWaitTime;

    public ViaVisitBuilder(String label, Duration minimumWaitTime) {
      super(label);
      this.minimumWaitTime = minimumWaitTime;
    }

    public ViaVisitBuilder addViaStop(int stop) {
      return addConnection(stop, null);
    }

    public ViaVisitBuilder addViaTransfer(int fromStop, RaptorTransfer transfer) {
      return addConnection(fromStop, transfer);
    }

    public RaptorViaLocation build() {
      return new RaptorViaLocation(label, false, minimumWaitTime, connections);
    }
  }

  public static final class PassThroughBuilder extends AbstractBuilder<PassThroughBuilder> {

    public PassThroughBuilder(String label) {
      super(label);
    }

    public PassThroughBuilder addPassThroughStop(int stop) {
      this.connections.add(new BuilderStopAndTransfer(stop, null));
      return this;
    }

    public PassThroughBuilder addPassThroughStops(int... stops) {
      IntStream.of(stops).forEach(this::addPassThroughStop);
      return this;
    }

    public RaptorViaLocation build() {
      return new RaptorViaLocation(label, true, Duration.ZERO, connections);
    }
  }

  /**
   * Use internally to store connection data, before creating the connection objects. If is
   * needed to create the bidirectional relationship between {@link RaptorViaLocation} and
   * {@link RaptorViaConnection}.
   */
  private static record BuilderStopAndTransfer(int fromStop, @Nullable RaptorTransfer transfer) {}
}
