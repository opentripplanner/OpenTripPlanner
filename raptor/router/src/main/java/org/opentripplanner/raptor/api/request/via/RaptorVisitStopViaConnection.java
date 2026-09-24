package org.opentripplanner.raptor.api.request.via;

import java.util.Objects;
import org.opentripplanner.raptor.spi.RaptorConstants;
import org.opentripplanner.raptor.spi.RaptorStopNameResolver;
import org.opentripplanner.utils.time.DurationUtils;

/**
 * See {@link ViaConnection}
 */
public final class RaptorVisitStopViaConnection extends ViaConnection {

  private final int minimumWaitTime;

  /// @param stop from and to stop
  RaptorVisitStopViaConnection(int stop, int minimumWaitTime) {
    super(stop);
    this.minimumWaitTime = minimumWaitTime;
  }

  public int minimumWaitTime() {
    return minimumWaitTime;
  }

  @Override
  public boolean leftDominanceExist(ViaConnection right) {
    if (!sameTypeAndStop(right, getClass())) {
      return true;
    }
    var o = (RaptorVisitStopViaConnection) right;
    return minimumWaitTime < o.minimumWaitTime;
  }

  @Override
  public boolean equals(Object other) {
    if (!super.sameTypeAndStop(other, getClass())) {
      return false;
    }
    var o = (RaptorVisitStopViaConnection) other;
    return minimumWaitTime == o.minimumWaitTime;
  }

  @Override
  public int hashCode() {
    return Objects.hash(fromStop(), minimumWaitTime);
  }

  @Override
  public final String toString(RaptorStopNameResolver stopNameResolver) {
    var buf = new StringBuilder("(stop ").append(stopNameResolver.apply(fromStop()));
    if (minimumWaitTime > RaptorConstants.ZERO) {
      buf.append(" [").append(DurationUtils.durationToStr(minimumWaitTime)).append(']');
    }
    return buf.append(')').toString();
  }
}
