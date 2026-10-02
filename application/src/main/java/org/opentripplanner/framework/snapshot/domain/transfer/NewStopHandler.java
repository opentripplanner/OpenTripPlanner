package org.opentripplanner.framework.snapshot.domain.transfer;

import org.opentripplanner.framework.snapshot.domain.NewStopUsedByTripPattern;
import org.opentripplanner.framework.snapshot.domain.transfer.repository.MutableTransferSnapshot;
import org.opentripplanner.framework.snapshot.event.EventHandler;

public class NewStopHandler
  implements EventHandler<NewStopUsedByTripPattern, MutableTransferSnapshot> {

  @Override
  public Class<NewStopUsedByTripPattern> eventType() {
    return NewStopUsedByTripPattern.class;
  }

  @Override
  public void handle(NewStopUsedByTripPattern event, MutableTransferSnapshot transfers) {
    transfers.setNumberOfRecalculations(transfers.getNumberOfRecalculations() + 1);
  }
}
