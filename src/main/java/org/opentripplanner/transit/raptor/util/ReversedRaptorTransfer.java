package org.opentripplanner.transit.raptor.util;

import org.opentripplanner.transit.raptor.api.transit.AbstractRaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;

public class ReversedRaptorTransfer extends AbstractRaptorTransfer {

  public ReversedRaptorTransfer(int fromStopIndex, RaptorTransfer transfer) {
    super(fromStopIndex, transfer.durationInSeconds(), transfer.generalizedCost());
  }
}
