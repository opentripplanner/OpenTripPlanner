package org.opentripplanner.framework.snapshot.domain.transfer;

import org.opentripplanner.framework.snapshot.domain.NewStopUsedByTripPattern;
import org.opentripplanner.framework.snapshot.domain.transfer.repository.MutableTransferSnapshot;
import org.opentripplanner.framework.snapshot.event.EventHandlerTransfer;

public class NewStopHandler implements EventHandlerTransfer<NewStopUsedByTripPattern> {

  @Override
  public Class<NewStopUsedByTripPattern> eventType() {
    return NewStopUsedByTripPattern.class;
  }

  @Override
  public void handle(NewStopUsedByTripPattern event, MutableTransferSnapshot transfers) {
    transfers.setNumberOfRecalculations(transfers.getNumberOfRecalculations() + 1);
  }
}
