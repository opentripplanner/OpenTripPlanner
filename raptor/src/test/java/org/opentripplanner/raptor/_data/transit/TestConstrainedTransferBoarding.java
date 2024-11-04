package org.opentripplanner.raptor._data.transit;

import java.util.function.Consumer;
import org.opentripplanner.raptor.api.model.RaptorTransferConstraint;
import org.opentripplanner.raptor.spi.RaptorBoardOrAlightEvent;

record TestConstrainedTransferBoarding(
  RaptorTransferConstraint transferConstraint,
  int tripIndex,
  TestTripSchedule trip,
  int stopPositionInPattern,
  int time,
  int earliestBoardTime
)
  implements RaptorBoardOrAlightEvent<TestTripSchedule> {
  @Override
  public boolean empty() {
    return false;
  }

  @Override
  public void boardWithFallback(
    Consumer<RaptorBoardOrAlightEvent<TestTripSchedule>> boardCallback,
    Consumer<RaptorBoardOrAlightEvent<TestTripSchedule>> alternativeBoardingFallback
  ) {
    if (empty()) {
      alternativeBoardingFallback.accept(this);
    } else if (!transferConstraint.isNotAllowed()) {
      boardCallback.accept(this);
    }
  }
}
