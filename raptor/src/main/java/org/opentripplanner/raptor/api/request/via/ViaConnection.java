package org.opentripplanner.raptor.api.request.via;

import org.opentripplanner.raptor.spi.RaptorStopNameResolver;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;

/// A via-connection is used to define one of the physical locations in a via location Raptor must
/// visit. At least one connection in a {@link RaptorViaLocation} must be used. A connection can be
/// a single stop or a stop and a transfer to another stop. The last is useful if you want to use
/// the connection to visit something other than a stop, like a street location. This is not an
/// alternative to transfers. Raptor supports several use-cases through via-connections:
///
///
/// #4 Route via a pass-through-stop
///
/// Raptor will allow a path to go through a pass-through-stop. The stop can be visited on-board
/// transit, or at the alight- or board-stop. The from-stop and to-stop must be the same (there is
/// no to-stop defined), and the minimum-wait-time must be zero.
///
///
/// #4 Route via a single stop with a minimum-wait-time
///
/// Raptor will allow a path to go through a single stop, the path must either alight or board at
/// the given stop. The from-stop and to-stop is the same (there is no to-stop defined). A
/// minimum-wait-time can be applied at the given stop.
///
///
/// #4 Route via a coordinate (transfer via connection)
///
/// To route through a coordinate you need to find all nearby stops, then find all access and egress
/// paths to and from the street location. Raptor does not know/see the actual via street location,
/// it only uses the connection from a stop to another, the total time it takes and the total cost.
/// From an algorithmic point of view this is just a transfer from a stop to another. To compute
/// transfers you must combine all access and egress paths to and from nearby stops (around the
/// street location/coordinate). You must generate a via transfer connection with two "legs" in it.
/// One leg going from the 'from-stop' to the street location, and one leg going back to the
/// 'to-stop'. If you have 10 stops around the via street location, then you must combine all ten
///  access paths and egress paths (in total 100 possible transfers).
///
/// The min-wait-time in the {@link RaptorViaLocation} is added to the transfers
/// {@code durationInSeconds}. The calculation of `c1` should include the walk time, but not
/// the min-wait-time (assuming all connections have the same minimum wait time).
public abstract sealed class ViaConnection
  permits
    RaptorPassThroughViaConnection, RaptorTransferViaConnection, RaptorVisitStopViaConnection {

  private final int fromStop;

  public ViaConnection(int fromStop) {
    this.fromStop = fromStop;
  }

  static ParetoComparator<ViaConnection> paretoComparator() {
    return (l, r) -> l.leftDominanceExist(r);
  }

  /**
   * Stop index where the connection starts. If only one stop is involved, then this is the
   * stop where the path continues from as well. Note! The {@code toStop()} method is only defined
   * for {@link RaptorTransferViaConnection}s.
   */
  public final int fromStop() {
    return fromStop;
  }

  /// This method is used to create a {@link ParetoComparator}, see {@link #paretoComparator()}.
  ///
  /// **Notes**
  /// - If the connection is connecting diffrent stops, they by definition dominates each other.
  /// - Only parameters having a direct effect on one of the search ciriteria should be included.
  ///   For example _minimum wait time_ should be included in the comparason, because it has an
  ///   effect on the arrivel time.
  abstract boolean leftDominanceExist(ViaConnection right);

  @Override
  public final String toString() {
    return toString(Integer::toString);
  }

  public abstract String toString(RaptorStopNameResolver stopNameResolver);

  /// Use this to test if the {@code other} value is of type {@code expectedType} and has the same
  /// {@code fromStop}, if not, return {@code false}.
  final boolean sameTypeAndStop(Object other, Class<? extends ViaConnection> expectedType) {
    if (other == null || expectedType != other.getClass()) {
      return false;
    }
    var that = (ViaConnection) other;
    return fromStop() == that.fromStop();
  }
}
