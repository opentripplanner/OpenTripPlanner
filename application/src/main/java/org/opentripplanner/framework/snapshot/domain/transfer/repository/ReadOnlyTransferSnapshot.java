package org.opentripplanner.framework.snapshot.domain.transfer.repository;

public class ReadOnlyTransferSnapshot {

  private final int numberOfRecalculations;

  public ReadOnlyTransferSnapshot(int numberOfRecalculations) {
    this.numberOfRecalculations = numberOfRecalculations;
  }

  public int getNumberOfRecalculations() {
    return numberOfRecalculations;
  }
}
