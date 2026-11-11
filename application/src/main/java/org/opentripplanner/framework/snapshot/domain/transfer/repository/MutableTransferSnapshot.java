package org.opentripplanner.framework.snapshot.domain.transfer.repository;

public class MutableTransferSnapshot {

  private int numberOfRecalculations;

  public MutableTransferSnapshot(int numberOfRecalculations) {
    this.numberOfRecalculations = numberOfRecalculations;
  }

  public int getNumberOfRecalculations() {
    return numberOfRecalculations;
  }

  public void setNumberOfRecalculations(int numberOfRecalculations) {
    this.numberOfRecalculations = numberOfRecalculations;
  }
}
