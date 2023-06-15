package org.opentripplanner.raptor.spi;

import java.util.function.Consumer;
import javax.annotation.Nonnull;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorTransferConstraint;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

record EmptyBoardOrAlightEvent<T extends RaptorTripSchedule>(int earliestBoardTime)
  implements RaptorBoardOrAlightEvent<T> {
  @Override
  public int tripIndex() {
    return RaptorConstants.NOT_FOUND;
  }

  @Override
  public T trip() {
    return null;
  }

  @Override
  public int stopPositionInPattern() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int time() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public RaptorTransferConstraint transferConstraint() {
    return RaptorTransferConstraint.REGULAR_TRANSFER;
  }

  @Override
  public boolean empty() {
    return true;
  }

  @Override
  public void boardWithFallback(
    Consumer<RaptorBoardOrAlightEvent<T>> boardCallback,
    Consumer<RaptorBoardOrAlightEvent<T>> alternativeBoardingFallback
  ) {
    alternativeBoardingFallback.accept(this);
  }
}
