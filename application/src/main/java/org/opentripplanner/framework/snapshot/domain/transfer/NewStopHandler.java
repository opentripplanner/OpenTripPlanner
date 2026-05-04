package org.opentripplanner.framework.snapshot.domain.transfer;

import java.util.function.Supplier;
import org.opentripplanner.framework.snapshot.domain.NewStopUsedByTripPattern;
import org.opentripplanner.framework.snapshot.domain.transfer.repository.MutableTransferSnapshot;
import org.opentripplanner.framework.snapshot.event.EventHandler;

public class NewStopHandler implements EventHandler<NewStopUsedByTripPattern> {

  private final Supplier<MutableTransferSnapshot> mutableTransferSnapshotSupplier;

  public NewStopHandler(Supplier<MutableTransferSnapshot> mutableTransferSnapshotSupplier) {
    this.mutableTransferSnapshotSupplier = mutableTransferSnapshotSupplier;
  }


  @Override
  public Class<NewStopUsedByTripPattern> eventType() {
    return NewStopUsedByTripPattern.class;
  }

  @Override
  public void handle(NewStopUsedByTripPattern event) {
    MutableTransferSnapshot transfers = mutableTransferSnapshotSupplier.get();
    transfers.setNumberOfRecalculations(transfers.getNumberOfRecalculations() + 1);
  }
}
