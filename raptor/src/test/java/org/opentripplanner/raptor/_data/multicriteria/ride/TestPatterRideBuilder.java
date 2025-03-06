package org.opentripplanner.raptor._data.multicriteria.ride;

import static org.opentripplanner.raptor.api.model.RaptorConstants.NOT_SET;

import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrivalFactory;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.c1.StopArrivalFactoryC1;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.c2.StopArrivalFactoryC2;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.PatternRide;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.c1.PatternRideC1;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.c2.PatternRideC2;

/**
 * Build a valid pattern ride for unit-tests.
 */
public class TestPatterRideBuilder {

  public static final int C_2 = 200_000;
  private final boolean includeC2;
  private final McStopArrivalFactory<TestTripSchedule> stopArrivalFactory;

  private McStopArrival<TestTripSchedule> prevArrival;
  private int boardStopIndex = NOT_SET;
  private int boardPos = NOT_SET;
  private int boardTime = NOT_SET;
  private TestTripSchedule trip;
  private int relativeC1 = NOT_SET;
  private int boardC1 = NOT_SET;
  private int c2 = NOT_SET;

  public TestPatterRideBuilder(
    boolean includeC2,
    McStopArrivalFactory<TestTripSchedule> stopArrivalFactory
  ) {
    this.includeC2 = includeC2;
    this.stopArrivalFactory = stopArrivalFactory;
  }

  public static TestPatterRideBuilder ofC1() {
    return new TestPatterRideBuilder(false, new StopArrivalFactoryC1<>());
  }

  public static TestPatterRideBuilder ofC2() {
    return new TestPatterRideBuilder(true, new StopArrivalFactoryC2<>());
  }

  public TestPatterRideBuilder withBoardPos(int boardPos) {
    this.boardPos = boardPos;
    this.boardTime = trip.departure(0);
    this.boardStopIndex = trip.pattern().stopIndex(boardPos);
    return this;
  }

  public PatternRide<TestTripSchedule> build() {
    if (trip == null) {
      trip = TestTripSchedule.schedule("10:03 11:00").build();
    }
    if (boardPos == NOT_SET) {
      withBoardPos(0);
    }
    if (prevArrival == null) {
      int departureTime = boardTime - 180;
      prevArrival = stopArrivalFactory.createAccessStopArrival(
        departureTime,
        TestAccessEgress.walk(boardStopIndex, 30)
      );
    }
    if (boardC1 == NOT_SET) {
      boardC1 = prevArrival.c1() + 150;
    }
    if (relativeC1 == NOT_SET) {
      this.relativeC1 = 100_000 - trip.arrival(trip.pattern().numberOfStopsInPattern() - 1);
    }
    if (includeC2 && c2 == NOT_SET) {
      c2 = C_2;
    }

    return includeC2
      ? new PatternRideC2<>(
        prevArrival,
        boardStopIndex,
        boardPos,
        boardTime,
        boardC1,
        relativeC1,
        c2,
        trip.tripSortIndex(),
        trip
      )
      : new PatternRideC1<>(
        prevArrival,
        boardStopIndex,
        boardPos,
        boardTime,
        boardC1,
        relativeC1,
        trip.tripSortIndex(),
        trip
      );
  }
}
