package org.opentripplanner.raptorlegacy._data.transit;

import org.opentripplanner.model.transfer.TransferPoint;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * @deprecated This was earlier part of Raptor and should not be used outside the Raptor
 *             module. Use the OTP model entities instead.
 */
@Deprecated
public class TestTransferPoint implements TransferPoint {

  private final int stop;
  // Wer need this because trip pattern can pass through same stop more than once
  private final int stopPosition;
  private final TestTripSchedule schedule;
  private final boolean applyToAllTrips;

  public TestTransferPoint(
    int stop,
    int stopPosition,
    TestTripSchedule schedule,
    boolean applyToAllTrips
  ) {
    this.stop = stop;
    this.stopPosition = stopPosition;
    this.schedule = schedule;
    this.applyToAllTrips = applyToAllTrips;
  }

  @Override
  public boolean appliesToAllTrips() {
    return applyToAllTrips;
  }

  @Override
  public int getSpecificityRanking() {
    return 2;
  }

  public boolean matches(TestTripSchedule schedule, int stop, int stopPosition) {
    return this.schedule == schedule && this.stop == stop && this.stopPosition == stopPosition;
  }

  @Override
  public String toString() {
    return ToStringBuilder.ofEmbeddedType()
      .addNum("stop", stop)
      .addObj("trip", schedule.pattern().debugInfo())
      .toString();
  }
}
