package org.opentripplanner.raptor.api.request.via;

import java.util.Objects;
import org.opentripplanner.raptor.api.model.RaptorValueType;
import org.opentripplanner.raptor.spi.RaptorStopNameResolver;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.utils.time.DurationUtils;

/**
 * See {@link ViaConnection}
 */
public final class RaptorTransferViaConnection extends ViaConnection {

  private final int minimumWaitTime;
  private final RaptorTransfer transfer;

  RaptorTransferViaConnection(int fromStop, int minimumWaitTime, RaptorTransfer transfer) {
    super(fromStop);
    this.transfer = Objects.requireNonNull(transfer);
    this.minimumWaitTime = minimumWaitTime;
  }

  public RaptorTransfer transfer() {
    return transfer;
  }

  /**
   * Stop index where the connection ends - only transfers have this.
   */
  public int toStop() {
    return transfer.stop();
  }

  public int durationInSeconds() {
    return minimumWaitTime + transfer.durationInSeconds();
  }

  int c1() {
    return transfer.c1();
  }

  @Override
  public boolean leftDominanceExist(ViaConnection right) {
    if (!super.sameTypeAndStop(right, getClass())) {
      return true;
    }
    var r = (RaptorTransferViaConnection) right;
    return toStop() != r.toStop() || durationInSeconds() < r.durationInSeconds() || c1() < r.c1();
  }

  @Override
  public boolean equals(Object other) {
    if (!super.sameTypeAndStop(other, getClass())) {
      return false;
    }
    var o = (RaptorTransferViaConnection) other;
    return toStop() == o.toStop() && durationInSeconds() == o.durationInSeconds() && c1() == o.c1();
  }

  @Override
  public int hashCode() {
    return Objects.hash(fromStop(), toStop(), durationInSeconds(), c1());
  }

  @Override
  public String toString(RaptorStopNameResolver stopNameResolver) {
    return new StringBuilder("(transfer ")
      .append(stopNameResolver.apply(fromStop()))
      .append(" ~ ")
      .append(stopNameResolver.apply(toStop()))
      .append(" [")
      .append(DurationUtils.durationToStr(durationInSeconds()))
      .append(" ")
      .append(RaptorValueType.C1.format(c1()))
      .append("])")
      .toString();
  }
}
