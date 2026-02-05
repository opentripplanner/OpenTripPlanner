package org.opentripplanner.framework.snapshot.persistence.transfer;

import jakarta.ws.rs.NotSupportedException;
import org.opentripplanner.framework.snapshot.domain.transfer.TransferRepo;

public class ImmutableTransferRepo implements TransferRepo {

  private final int numberOfRecalculations;

  public ImmutableTransferRepo(int numberOfRecalculations) {
    this.numberOfRecalculations = numberOfRecalculations;
  }

  public int getNumberOfRecalculations() {
    return numberOfRecalculations;
  }

  @Override
  public void setNumberOfRecalculations(int number) {
    throw new NotSupportedException("read only access, no writes allowed");
  }
}
