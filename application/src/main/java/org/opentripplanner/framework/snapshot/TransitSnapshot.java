package org.opentripplanner.framework.snapshot;

public class TransitSnapshot {

  private final TransferRepo transferRepo;
  private final TimetableRepo timetableRepo;

  public TransitSnapshot(TransferRepo transferRepo, TimetableRepo timetableRepo) {
    this.transferRepo = transferRepo;
    this.timetableRepo = timetableRepo;
  }

  public TransferRepo getTransferRepo() {
    return transferRepo;
  }

  public TimetableRepo getTimetableRepo() {
    return timetableRepo;
  }
}
