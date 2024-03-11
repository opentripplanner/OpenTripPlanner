package org.opentripplanner.raptor.rangeraptor.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.framework.time.TimeUtils.hm2time;
import static org.opentripplanner.raptor._data.RaptorTestConstants.D1m;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_A;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_B;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.spi.IntIterator;

public class ForwardRaptorTransitCalculatorTest {

  private static final int BIG_TIME = hm2time(100, 0);
  private int earliestDepartureTime = hm2time(8, 0);
  private int searchWindowSizeInSeconds = 2 * 60 * 60;
  private int latestAcceptableArrivalTime = hm2time(16, 0);
  private int iterationStep = 60;

  @Test
  public void exceedsTimeLimit() {
    latestAcceptableArrivalTime = 1200;
    var subject = create();

    assertFalse(subject.exceedsTimeLimit(latestAcceptableArrivalTime));
    assertTrue(subject.exceedsTimeLimit(latestAcceptableArrivalTime + 1));

    latestAcceptableArrivalTime = hm2time(16, 0);

    assertEquals(
      "The arrival time exceeds the time limit, arrive to late: 16:00:00.",
      create().exceedsTimeLimitReason()
    );

    latestAcceptableArrivalTime = RaptorConstants.TIME_NOT_SET;
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

  @Test
  void timeMinusPenalty() {
    var subject = create();
    var walk200s = TestAccessEgress.walk(15, 200);
    int time = 1000;
    int penalty = 300;
    int expectedTimeWithoutPenalty = time - penalty;

    assertEquals(time, subject.timeMinusPenalty(time, walk200s));
    assertEquals(
      expectedTimeWithoutPenalty,
      subject.timeMinusPenalty(time, walk200s.withTimePenalty(penalty))
    );
  }

  private RaptorTransitCalculator<TestTripSchedule> create() {
    return new ForwardRaptorTransitCalculator<>(
      earliestDepartureTime,
      searchWindowSizeInSeconds,
      latestAcceptableArrivalTime,
      iterationStep
      //c2 -> c2 == desiredC2
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
