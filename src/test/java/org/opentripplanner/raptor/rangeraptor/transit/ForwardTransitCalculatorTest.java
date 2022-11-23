package org.opentripplanner.raptor.rangeraptor.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.framework.time.TimeUtils.hm2time;
import static org.opentripplanner.raptor._data.RaptorTestConstants.D1m;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_A;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_B;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.spi.IntIterator;

public class ForwardTransitCalculatorTest {

  private int earliestDepartureTime = hm2time(8, 0);
  private int searchWindowSizeInSeconds = 2 * 60 * 60;
  private int latestAcceptableArrivalTime = hm2time(16, 0);
  private int iterationStep = 60;

  @Test
  public void exceedsTimeLimit() {
    latestAcceptableArrivalTime = 1200;
    var subject = create();

    assertFalse(subject.exceedsTimeLimit(0));
    assertFalse(subject.exceedsTimeLimit(1200));
    assertTrue(subject.exceedsTimeLimit(1201));

    latestAcceptableArrivalTime = hm2time(16, 0);

    assertEquals(
      "The arrival time exceeds the time limit, arrive to late: 16:00:00.",
      create().exceedsTimeLimitReason()
    );

    latestAcceptableArrivalTime = TransitCalculator.TIME_NOT_SET;
    subject = create();
    assertFalse(subject.exceedsTimeLimit(0));
    assertFalse(subject.exceedsTimeLimit(2_000_000_000));
  }

  @Test
  public void oneIterationOnly() {
    var subject = create();

    assertFalse(subject.oneIterationOnly());

    searchWindowSizeInSeconds = 0;
    subject = create();

    assertTrue(subject.oneIterationOnly());
  }

  @Test
  public void latestArrivalTime() {
    var s = TestTripSchedule.schedule().arrivals(500).build();
    assertEquals(500, create().stopArrivalTime(s, 0, 0));
  }

  @Test
  public void rangeRaptorMinutes() {
    earliestDepartureTime = 500;
    searchWindowSizeInSeconds = 200;
    iterationStep = 100;

    assertIntIterator(create().rangeRaptorMinutes(), 600, 500);
  }

  @Test
  public void patternStopIterator() {
    assertIntIterator(create().patternStopIterator(2), 0, 1);
  }

  @Test
  public void getTransfers() {
    var subject = create();
    var transitData = new TestTransitData()
      .withTransfer(STOP_A, TestTransfer.transfer(STOP_B, D1m));

    // Expect transfer from stop A to stop B
    var transfersFromStopA = subject.getTransfers(transitData, STOP_A);
    assertTrue(transfersFromStopA.hasNext());
    assertEquals(STOP_B, transfersFromStopA.next().stop());

    // No transfer for stop B expected
    assertFalse(subject.getTransfers(transitData, STOP_B).hasNext());
  }

  private TransitCalculator<TestTripSchedule> create() {
    return new ForwardTransitCalculator<>(
      earliestDepartureTime,
      searchWindowSizeInSeconds,
      latestAcceptableArrivalTime,
      iterationStep
    );
  }

  private void assertIntIterator(IntIterator it, int... values) {
    for (int v : values) {
      assertTrue(it.hasNext());
      assertEquals(v, it.next());
    }
    assertFalse(it.hasNext());
  }
}
