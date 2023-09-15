package org.opentripplanner.raptor.rangeraptor.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.framework.time.TimeUtils.hm2time;
import static org.opentripplanner.raptor._data.RaptorTestConstants.D1m;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_A;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_B;

import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.lang.IntBox;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.PathLegType;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.spi.IntIterator;

public class ForwardRaptorTransitCalculatorTest {

  private int earliestDepartureTime = hm2time(8, 0);
  private int searchWindowSizeInSeconds = 2 * 60 * 60;
  private int latestAcceptableArrivalTime = hm2time(16, 0);
  private int iterationStep = 60;
  private int desiredC2 = 0;

  @Test
  public void exceedsTimeLimit() {
    latestAcceptableArrivalTime = 1200;
    var subject = create();

    assertTrue(subject.rejectDestinationArrival(new TestArrivalView(desiredC2, 0)).isEmpty());
    assertTrue(
      subject
        .rejectDestinationArrival(new TestArrivalView(desiredC2, latestAcceptableArrivalTime))
        .isEmpty()
    );
    assertFalse(
      subject
        .rejectDestinationArrival(new TestArrivalView(desiredC2, latestAcceptableArrivalTime + 1))
        .isEmpty()
    );

    latestAcceptableArrivalTime = hm2time(16, 0);
    subject = create();
    var errors = subject.rejectDestinationArrival(
      new TestArrivalView(desiredC2, latestAcceptableArrivalTime + 1)
    );
    assertEquals(1, errors.size());
    assertEquals(
      "The arrival time exceeds the time limit, arrive to late: 16:00:00.",
      errors.stream().findFirst().get()
    );

    latestAcceptableArrivalTime = RaptorConstants.TIME_NOT_SET;
    subject = create();
    assertTrue(subject.rejectDestinationArrival(new TestArrivalView(desiredC2, 0)).isEmpty());
    assertTrue(
      subject.rejectDestinationArrival(new TestArrivalView(desiredC2, 2_000_000_000)).isEmpty()
    );
  }

  @Test
  public void rejectC2AtDestination() {
    desiredC2 = 1;
    var subject = create();

    var errors = subject.rejectDestinationArrival(
      new TestArrivalView(desiredC2, latestAcceptableArrivalTime)
    );
    assertTrue(errors.isEmpty());

    errors =
      subject.rejectDestinationArrival(
        new TestArrivalView(desiredC2 + 1, latestAcceptableArrivalTime)
      );
    assertEquals(1, errors.size());
    assertEquals("C2 value rejected: 2.", errors.stream().findFirst().get());
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

  private RaptorTransitCalculator<TestTripSchedule> create() {
    return new ForwardRaptorTransitCalculator<>(
      earliestDepartureTime,
      searchWindowSizeInSeconds,
      latestAcceptableArrivalTime,
      iterationStep,
      c2 -> c2 == desiredC2
    );
  }

  private void assertIntIterator(IntIterator it, int... values) {
    for (int v : values) {
      assertTrue(it.hasNext());
      assertEquals(v, it.next());
    }
    assertFalse(it.hasNext());
  }

  public record TestArrivalView(int c2, int arrivalTime) implements ArrivalView<TestTripSchedule> {
    @Override
    public int stop() {
      return c2;
    }

    @Override
    public int round() {
      return 0;
    }

    @Override
    public int arrivalTime() {
      return arrivalTime;
    }

    @Override
    public int c1() {
      return 0;
    }

    @Override
    public int c2() {
      return c2;
    }

    @Nullable
    @Override
    public ArrivalView previous() {
      return null;
    }

    @Override
    public PathLegType arrivedBy() {
      return null;
    }

    @Override
    public boolean arrivedOnBoard() {
      return false;
    }
  }
}
