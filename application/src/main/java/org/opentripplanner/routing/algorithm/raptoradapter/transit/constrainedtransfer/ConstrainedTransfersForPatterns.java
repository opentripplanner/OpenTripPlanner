package org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer;

import java.util.List;
import java.util.Objects;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * This is a container for returning transfers from and to stop-positions indexed by
 * the route index.
 */
public class ConstrainedTransfersForPatterns {

  private final List<TransferForPatternByStopPos> transfersToStop;
  private final List<TransferForPatternByStopPos> transfersFromStop;

  public ConstrainedTransfersForPatterns(
    List<TransferForPatternByStopPos> transfersToStop,
    List<TransferForPatternByStopPos> transfersFromStop
  ) {
    this.transfersToStop = transfersToStop;
    this.transfersFromStop = transfersFromStop;
  }

  public TransferForPatternByStopPos toStop(int routeIndex) {
    return transfersToStop.get(routeIndex);
  }

  public TransferForPatternByStopPos fromStop(int routeIndex) {
    return transfersFromStop.get(routeIndex);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ConstrainedTransfersForPatterns that = (ConstrainedTransfersForPatterns) o;
    return (
      Objects.equals(transfersToStop, that.transfersToStop) &&
      Objects.equals(transfersFromStop, that.transfersFromStop)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(transfersToStop, transfersFromStop);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(ConstrainedTransfersForPatterns.class)
      .addCol("to", transfersToStop)
      .addCol("from", transfersFromStop)
      .toString();
  }
}
