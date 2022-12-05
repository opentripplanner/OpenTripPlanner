package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import org.opentripplanner.raptor.spi.RaptorTransfer;

public record DefaultRaptorTransfer(
  int stop,
  int durationInSeconds,
  int generalizedCost,
  Transfer transfer
)
  implements RaptorTransfer {
  public static DefaultRaptorTransfer reverseOf(int fromStopIndex, RaptorTransfer transfer) {
    return new DefaultRaptorTransfer(
      fromStopIndex,
      transfer.durationInSeconds(),
      transfer.generalizedCost(),
      transfer instanceof DefaultRaptorTransfer drt ? drt.transfer : null
    );
  }
}
