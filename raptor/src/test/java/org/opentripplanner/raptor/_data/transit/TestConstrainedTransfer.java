package org.opentripplanner.raptor._data.transit;

import static org.opentripplanner.raptor.api.model.RaptorConstants.NOT_SET;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorConstrainedTransfer;
import org.opentripplanner.raptor.api.model.RaptorTransferConstraint;
import org.opentripplanner.raptor.spi.RaptorBoardOrAlightEvent;
import org.opentripplanner.utils.tostring.ToStringBuilder;

class TestConstrainedTransfer
  implements RaptorConstrainedTransfer, RaptorBoardOrAlightEvent<TestTripSchedule> {

  private final TestTransferConstraint transferConstraint;
  private final TestTripSchedule sourceTrip;
  private final int sourceStopPos;
  private final TestTripSchedule targetTrip;
  private final int targetStopPos;
  private final int targetTripIndex;
  private final int targetTime;

  private int earliestBoardTime = NOT_SET;

  TestConstrainedTransfer(
    TestTransferConstraint transferConstraint,
    TestTripSchedule sourceTrip,
    int sourceStopPos,
    TestTripSchedule targetTrip,
    int targetStopPos,
    int targetTripIndex,
    int targetTime
  ) {
    this.transferConstraint = transferConstraint;
    this.sourceTrip = sourceTrip;
    this.sourceStopPos = sourceStopPos;
    this.targetTrip = targetTrip;
    this.targetTripIndex = targetTripIndex;
    this.targetStopPos = targetStopPos;
    this.targetTime = targetTime;
  }

  @Override
  public int tripIndex() {
    return targetTripIndex;
  }

  @Override
  public TestTripSchedule trip() {
    return targetTrip;
  }

  @Override
  public int stopPositionInPattern() {
    return targetStopPos;
  }

  @Override
  public int time() {
    return targetTime;
  }

  @Override
  public int earliestBoardTime() {
    return earliestBoardTime;
  }

  @Override
  public RaptorTransferConstraint transferConstraint() {
    return transferConstraint;
  }

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

  public boolean isFacilitated() {
    return transferConstraint.isStaySeated() || transferConstraint.isGuaranteed();
  }

  @Nullable
  @Override
  public RaptorTransferConstraint getTransferConstraint() {
    return transferConstraint;
  }

  TestTripSchedule getSourceTrip() {
    return sourceTrip;
  }

  int getSourceStopPos() {
    return sourceStopPos;
  }

  RaptorBoardOrAlightEvent<TestTripSchedule> boardingEvent(int earliestBoardingTime) {
    this.earliestBoardTime = earliestBoardingTime;
    return this;
  }

  public boolean match(
    TestTripSchedule sourceTrip,
    int sourceStopPos,
    TestTripSchedule targetTrip,
    int targetStopPos
  ) {
    return (
      this.sourceTrip.equals(sourceTrip) &&
      this.sourceStopPos == sourceStopPos &&
      this.targetTrip.equals(targetTrip) &&
      this.targetStopPos == targetStopPos
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TestConstrainedTransfer.class)
      .addObj("sourceTrip", sourceTrip)
      .addNum("sourceStopPos", sourceStopPos)
      .addObj("targetTrip", targetTrip)
      .addNum("targetTripIndex", targetTripIndex)
      .addNum("targetStopPos", targetStopPos)
      .addServiceTime("targetTime", targetTime)
      .toString();
  }
}
