package org.opentripplanner.framework.snapshot.domain.transfer;

import org.opentripplanner.framework.snapshot.domain.world.NewStopUsedByTripPattern;
import org.opentripplanner.framework.snapshot.event.DomainEventHandlerTransfers;

public class NewStopHandler implements DomainEventHandlerTransfers<NewStopUsedByTripPattern> {


  @Override
  public Class<NewStopUsedByTripPattern> eventType() {
    return NewStopUsedByTripPattern.class;
  }

  @Override
  public void handle(NewStopUsedByTripPattern event, TransferRepo transfers) {
    transfers.setNumberOfRecalculations(transfers.getNumberOfRecalculations() + 1);
  }
}
