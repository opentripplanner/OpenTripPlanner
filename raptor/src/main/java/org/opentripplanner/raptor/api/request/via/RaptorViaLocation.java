package org.opentripplanner.raptor.api.request.via;

import java.time.Duration;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.spi.RaptorStopNameResolver;
import org.opentripplanner.raptor.util.paretoset.ParetoDominance;
import org.opentripplanner.utils.lang.IntUtils;

/**
 * Defines a via location which Raptor will force the path through. The concrete location is
 * called a connection. A location must have at least one connection, but can have more than
 * one alternative. Raptor will force the path through one of the connections. So, if there
 * are two connections, stop A and B, then Raptor will force the path through A or B. If the
 * path goes through A, it may or may not go through B.
 */
public final class RaptorViaLocation {

  private static final Duration MAX_WAIT_TIME = Duration.ofHours(24);
  private static final Duration MIN_WAIT_TIME = Duration.ZERO;

  private final String label;
  private final List<ViaConnection> connections;

  RaptorViaLocation(String label, List<ViaConnection> connections) {
    this.label = label;
    this.connections = connections;

    if (connections.isEmpty()) {
      throw new IllegalArgumentException("At least one connection must exist!");
    }
  }

  /**
   * Force the path through a set of stops, either on-board or as an alight or board stop.
   */
  public static PassThroughLocationBuilder passThrough(@Nullable String label) {
    return new PassThroughLocationBuilder(label);
  }

  /**
   * Force the path through one of the listed connections. To visit a stop, the path must board or
   * alight transit at the given stop, on-board visits do not count, see
   * {@link #passThrough(String)}.
   */
  public static ViaVisitLocationBuilder viaVisit(@Nullable String label) {
    return viaVisit(label, MIN_WAIT_TIME);
  }

  /**
   * Force the path through one of the listed connections, and wait the given minimum-wait-time
   * before continuing. To visit a stop, the path must board or alight transit at the given stop,
   * on-board visits do not count, see {@link #passThrough(String)}.
   */
  public static ViaVisitLocationBuilder viaVisit(@Nullable String label, Duration minimumWaitTime) {
    return new ViaVisitLocationBuilder(label, minimumWaitTime);
  }

  @Nullable
  public String label() {
    return label;
  }

  private boolean isPassThroughSearch() {
    return connections.stream().anyMatch(it -> it instanceof RaptorPassThroughViaConnection);
  }

  public List<ViaConnection> connections() {
    return connections;
  }

  /**
   * This is a convenient accessor method used inside Raptor. It converts the list of stops to a
   * bit-set. Add other access methods if needed.
   */
  public BitSet asBitSet() {
    return connections
      .stream()
      .mapToInt(ViaConnection::fromStop)
      .collect(BitSet::new, BitSet::set, BitSet::or);
  }

  /// Compare all pairs to check for duplicates and non-optimal connections.
  public void validateDuplicateConnections() {
    var comparator = ViaConnection.paretoComparator();
    // Group by fromStop to reduce the Order from O(N*N) to O(S*n*n), where:
    // (N is # connections, S is # from-stops, n is # of connections per stop)
    // N is ~= S * n, so this S times faster. If S==N, then n=1 and the order is O(N)
    var byFromStop = connections.stream().collect(Collectors.groupingBy(ViaConnection::fromStop));
    for (var list : byFromStop.values()) {
      for (int i = 0; i < list.size(); ++i) {
        var x = list.get(i);
        for (int j = i + 1; j < list.size(); ++j) {
          var y = list.get(j);
          // If NOT both values dominate each other, at least one should be dropped
          var dominance = comparator.compare(x, y);
          if (dominance != ParetoDominance.MUTUAL) {
            throw new IllegalArgumentException(
              "All connection need to be pareto-optimal: %s %s %s".formatted(
                x,
                dominance.symbol(),
                y
              )
            );
          }
        }
      }
    }
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
    buf.append(isPassThroughSearch() ? "pass-through " : "via-visit ");
    if (label != null) {
      buf.append(label).append(" ");
    }
    buf
      .append(connections.size() <= 10 ? ": " : "(10/" + connections.size() + "): ")
      .append(
        connections
          .stream()
          .limit(10)
          .map(it -> it.toString(stopNameResolver))
          .toList()
      );
    return buf.append("}").toString();
  }

  static int validateMinimumWaitTime(Duration minimumWaitTime) {
    return IntUtils.requireInRange(
      (int) minimumWaitTime.toSeconds(),
      (int) MIN_WAIT_TIME.toSeconds(),
      (int) MAX_WAIT_TIME.toSeconds(),
      "minimumWaitTime"
    );
  }
}
