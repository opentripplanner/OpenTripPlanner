package org.opentripplanner.framework.snapshot.persistence.repository.transfer;

import org.opentripplanner.framework.snapshot.domain.world.NewStopUsedByTripPattern;
import org.opentripplanner.framework.snapshot.domain.transfer.TransferRepo;

public class MutableTransferRepo implements TransferRepo {

  private int numberOfRecalculations;

  public MutableTransferRepo(int numberOfRecalculations) {
    this.numberOfRecalculations = numberOfRecalculations;
  }

  public static MutableTransferRepo from(ImmutableTransferRepo transferRepo) {
    return new MutableTransferRepo(transferRepo.getNumberOfRecalculations());
  }

  public ImmutableTransferRepo freeze() {
    return new ImmutableTransferRepo(numberOfRecalculations);
  }

  public void recalculateTransfers(NewStopUsedByTripPattern event) {
    numberOfRecalculations += 1;
  }

  @Override
  public int getNumberOfRecalculations() {
    return numberOfRecalculations;
  }

  @Override
  public void setNumberOfRecalculations(int number) {
    numberOfRecalculations = number;
  }
}
