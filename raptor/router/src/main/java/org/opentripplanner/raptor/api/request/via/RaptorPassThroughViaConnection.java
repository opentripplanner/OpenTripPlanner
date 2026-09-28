package org.opentripplanner.raptor.api.request.via;

import java.util.Objects;
import org.opentripplanner.raptor.spi.RaptorStopNameResolver;

/**
 * See {@link ViaConnection}
 */
public final class RaptorPassThroughViaConnection extends ViaConnection {

  /// @param stop from and to stop
  RaptorPassThroughViaConnection(int stop) {
    super(stop);
  }

  @Override
  public boolean leftDominanceExist(ViaConnection right) {
    return !equals(right);
  }

  @Override
  public boolean equals(Object other) {
    return super.sameTypeAndStop(other, getClass());
  }

  @Override
  public int hashCode() {
    return Objects.hash(fromStop());
  }

  public final String toString(RaptorStopNameResolver stopNameResolver) {
    return new StringBuilder()
      .append("(stop ")
      .append(stopNameResolver.apply(fromStop()))
      .append(')')
      .toString();
  }
}
