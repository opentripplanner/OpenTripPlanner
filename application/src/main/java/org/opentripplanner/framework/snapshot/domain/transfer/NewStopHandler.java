package org.opentripplanner.framework.snapshot.domain.transfer;

import org.opentripplanner.framework.snapshot.domain.world.NewStopUsedByTripPattern;
import org.opentripplanner.framework.snapshot.domain.world.TransitWorld;
import org.opentripplanner.framework.snapshot.event.DomainEventHandler;

public class NewStopHandler implements DomainEventHandler<NewStopUsedByTripPattern> {


  @Override
  public Class<NewStopUsedByTripPattern> eventType() {
    return NewStopUsedByTripPattern.class;
  }

  @Override
  public void handle(NewStopUsedByTripPattern event, TransitWorld transitWorld) {
    TransferRepo transfers = transitWorld.transfers();
    transfers.setNumberOfRecalculations(transfers.getNumberOfRecalculations() + 1);
  }
}
