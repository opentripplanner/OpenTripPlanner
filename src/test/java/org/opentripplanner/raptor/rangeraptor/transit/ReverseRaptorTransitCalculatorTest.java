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
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.spi.IntIterator;

public class ReverseRaptorTransitCalculatorTest {

  private static final int BIG_TIME = hm2time(100, 0);

  private int latestArrivalTime = hm2time(8, 0);
  private int searchWindowSizeInSeconds = 2 * 60 * 60;
  private int earliestAcceptableDepartureTime = hm2time(16, 0);
  private int iterationStep = 60;

  @Test
  public void exceedsTimeLimit() {
    earliestAcceptableDepartureTime = 1200;
    var subject = create();

    assertFalse(subject.exceedsTimeLimit(200_000));
    assertFalse(subject.exceedsTimeLimit(1200));
    assertTrue(subject.exceedsTimeLimit(1199));

    earliestAcceptableDepartureTime = hm2time(16, 0);

    assertEquals(
      "The departure time exceeds the time limit, depart to early: 16:00:00.",
      create().exceedsTimeLimitReason()
    );

    earliestAcceptableDepartureTime = RaptorConstants.TIME_NOT_SET;
    subject = create();
    assertFalse(subject.exceedsTimeLimit(-BIG_TIME));
    assertFalse(subject.exceedsTimeLimit(BIG_TIME));
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
  public void rangeRaptorMinutes() {
    latestArrivalTime = 500;
    searchWindowSizeInSeconds = 200;
    iterationStep = 100;

    assertIntIterator(create().rangeRaptorMinutes(), 400, 500);
  }

  @Test
  public void patternStopIterator() {
    assertIntIterator(create().patternStopIterator(2), 1, 0);
  }

  @Test
  public void getTransfers() {
    var subject = create();
    var transitData = new TestTransitData()
      .withTransfer(STOP_A, TestTransfer.transfer(STOP_B, D1m));

    // Expect transfer from stop A to stop B (reversed)
    var transfersFromStopB = subject.getTransfers(transitData, STOP_B);
    assertTrue(transfersFromStopB.hasNext());
    assertEquals(STOP_A, transfersFromStopB.next().stop());

    // No transfer form stop A expected
    assertFalse(subject.getTransfers(transitData, STOP_A).hasNext());
  }

  private RaptorTransitCalculator<TestTripSchedule> create() {
    return new ReverseRaptorTransitCalculator<>(
      latestArrivalTime,
      searchWindowSizeInSeconds,
      earliestAcceptableDepartureTime,
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
