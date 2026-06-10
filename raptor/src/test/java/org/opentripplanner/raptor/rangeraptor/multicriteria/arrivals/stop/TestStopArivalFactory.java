package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.stream.IntStream;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTripPattern;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.PatternRideC2;

public class TestStopArivalFactory {

  private static final int ANY = 17_345;
  private static final int BOARD_SLACK = 60;

  private final Queue<Integer> stops;
  private float legCoeficient;
  private float trasitLegCoeficient;
  private int depatureTime;
  private int duration;
  private int c1;
  private int c2;
  private boolean arriveOnBoard;

  private final StopArrivalFactoryC2<TestTripSchedule> factory = new StopArrivalFactoryC2<>();

  private TestStopArivalFactory(
    int depatureTime,
    int arrivalTime,
    int nRounds,
    int c1,
    int c2,
    boolean arriveOnBoard
  ) {
    this.depatureTime = depatureTime;
    this.duration = arrivalTime - depatureTime;
    this.c1 = c1;
    this.c2 = c2;
    this.arriveOnBoard = arriveOnBoard;

    int nLegs;
    if (nRounds == 0) {
      nLegs = 1;
      this.trasitLegCoeficient = -1.0f;
    } else {
      nLegs = 1 + nRounds + (arriveOnBoard ? 0 : 1);
      this.trasitLegCoeficient = 1.0f / nRounds;
    }
    this.stops = new ArrayDeque<>(IntStream.range(1, nLegs + 1).boxed().toList());
    this.legCoeficient = 1.0f / nLegs;
  }

  public static McStopArrival<TestTripSchedule> arrivalPath(
    int depatureTime,
    int arrivalTime,
    int nRounds,
    int c1,
    int c2,
    boolean arriveOnBoard
  ) {
    var factory = new TestStopArivalFactory(
      depatureTime,
      arrivalTime,
      nRounds,
      c1,
      c2,
      arriveOnBoard
    );
    return factory.arrivalPath(nRounds);
  }

  private McStopArrival<TestTripSchedule> arrivalPath(int nRounds) {
    var previous = accessArrival();
    if (nRounds > 0) {
      for (int r = 1; r <= nRounds; ++r) {
        previous = transitArrival(previous, r == nRounds, arriveOnBoard);
      }
      if (!arriveOnBoard) {
        previous = transferArrival(previous);
      }
    }
    return previous;
  }

  private McStopArrival<TestTripSchedule> accessArrival() {
    var accessPath = TestAccessEgress.walk(stops.poll(), scaleLeg(duration), scaleLeg(c1));
    return factory.createAccessStopArrival(depatureTime, accessPath);
  }

  private McStopArrival<TestTripSchedule> transitArrival(
    McStopArrival<TestTripSchedule> previous,
    boolean lastTransitLeg,
    boolean arriveOnBoard
  ) {
    boolean lastLeg = arriveOnBoard && lastTransitLeg;
    int fromStop = previous.stop();
    int toStop = stops.poll();
    int depatureTime = previous.arrivalTime() + BOARD_SLACK;
    int arrivalTime = lastLeg ? arrivalTime() : previous.arrivalTime() + scaleLeg(duration);
    int c1 = lastLeg ? this.c1 : previous.c1() + scaleLeg(this.c1);
    int c2 = lastTransitLeg ? this.c2 : previous.c2() + scaleTransitLeg(this.c2);
    var trip = TestTripSchedule.schedule()
      .times(depatureTime, arrivalTime)
      .pattern(TestTripPattern.pattern("Line " + fromStop, fromStop, toStop))
      .build();
    var ride = new PatternRideC2<>(
      previous,
      fromStop,
      0,
      trip.departure(0),
      ANY,
      ANY,
      c2,
      trip.tripSortIndex(),
      trip
    );
    return factory.createTransitStopArrival(ride, toStop, trip.arrival(1), c1);
  }

  private McStopArrival<TestTripSchedule> transferArrival(
    McStopArrival<TestTripSchedule> previousArrival
  ) {
    int arrivalTime = arrivalTime();
    int transferDuration = arrivalTime - previousArrival.arrivalTime();
    int c1 = this.c1 - previousArrival.c1();
    var transfer = TestTransfer.transfer(stops.poll(), transferDuration, c1);

    return factory.createTransferStopArrival(previousArrival, transfer, arrivalTime);
  }

  private int arrivalTime() {
    return depatureTime + duration;
  }

  private int scaleLeg(int total) {
    return scale(total, legCoeficient);
  }

  private int scaleTransitLeg(int total) {
    return scale(total, trasitLegCoeficient);
  }

  private static int scale(int total, float coeficient) {
    return Math.round(total * coeficient);
  }

  /**
   * This Test data factory is complex - so we have a test on it to verify that it produces
   * the exact test data we want.
   */
  public static class Test {

    @org.junit.jupiter.api.Test
    public void testThisFactory() {
      assertEquals(
        "Access [0:03 Rₙ0 C₁5 C₂0] (Walk 2m C₁5 ~ 1)",
        arrivalPath(60, 180, 0, 500, ANY, false).toString()
      );
      assertEquals(
        "Transit [0:04 Rₙ1 C₁5 C₂800] (BUS Line 1 ~ 2)",
        arrivalPath(60, 240, 1, 500, 800, true).toString()
      );
      assertEquals(
        "Transfer [0:04 Rₙ1 C₁5 C₂80] (On-Street 1m ~ 3)",
        arrivalPath(60, 240, 1, 500, 80, false).toString()
      );
      assertEquals(
        "Transit [0:05 Rₙ2 C₁5 C₂80] (BUS Line 2 ~ 3)",
        arrivalPath(120, 300, 2, 500, 80, true).toString()
      );
      assertEquals(
        "Transfer [0:05 Rₙ2 C₁5 C₂80] (On-Street 45s ~ 4)",
        arrivalPath(120, 300, 2, 500, 80, false).toString()
      );
    }
  }
}
