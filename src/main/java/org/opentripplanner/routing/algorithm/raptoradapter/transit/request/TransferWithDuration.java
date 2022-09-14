package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import org.opentripplanner.routing.algorithm.raptoradapter.transit.Transfer;
import org.opentripplanner.transit.raptor.api.transit.AbstractRaptorTransfer;

public class TransferWithDuration extends AbstractRaptorTransfer {

  private final Transfer transfer;

  public TransferWithDuration(Transfer transfer, int durationSeconds, int cost) {
    super(transfer.getToStop(), durationSeconds, cost);
    this.transfer = transfer;
  }

  public Transfer transfer() {
    return transfer;
  }

  @Override
  public boolean hasOpeningHours() {
    return false;
  }

  @Override
  public String toString() {
    return asString();
  }
}
