package org.opentripplanner.raptor.rangeraptor.destination;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop.McStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop.StopArrivalFactoryC1;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.PatternRideC1;
import org.opentripplanner.raptor.rangeraptor.transit.AccessWithPenalty;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;

/**
 * The time-shifted departure is the time the access is moved to, as late as possible, to fit the
 * first transit leg.
 */
class TimeShiftedDepartureTimeTest implements RaptorTestConstants {

  private static final int ITERATION_TIME = T00_10;
  private static final int BOARD_SLACK = 60;
  private static final int TRANSFER_SLACK = 30;
  private static final int ACCESS_DURATION = 300;
  private static final int BOARD_TIME = 1200;
  private static final int ANY = 9_999;

  private static final StopArrivalFactoryC1<TestTripSchedule> FACTORY =
    new StopArrivalFactoryC1<>();

  private static final TestTripSchedule TRIP_1 = TestTripSchedule.schedule()
    .pattern("L1", STOP_B, STOP_C)
    .times("0:20 0:30")
    .build();
  private static final TestTripSchedule TRIP_2 = TestTripSchedule.schedule()
    .pattern("L2", STOP_C, STOP_D)
    .times("0:32 0:40")
    .build();

  private static final RaptorSlackProvider SLACK = new RaptorSlackProvider() {
    @Override
    public int transferSlack() {
      return TRANSFER_SLACK;
    }

    @Override
    public int boardSlack(int slackIndex) {
      return BOARD_SLACK;
    }

    @Override
    public int alightSlack(int slackIndex) {
      return 0;
    }
  };

  private final TimeShiftedDepartureTime<TestTripSchedule> subject = new TimeShiftedDepartureTime<>(
    SLACK
  );

  @Test
  void accessIsShiftedAsLateAsPossibleToFitTheFirstTransit() {
    var access = TestAccessEgress.walk(STOP_B, ACCESS_DURATION);

    var arrival = ride(FACTORY.createAccessStopArrival(ITERATION_TIME, access), TRIP_1);

    assertEquals(BOARD_TIME - BOARD_SLACK - ACCESS_DURATION, calculate(arrival));
  }

  @Test
  void transferAfterAccessIsSubtracted() {
    var access = TestAccessEgress.walk(STOP_A, ACCESS_DURATION);
    int transferDuration = 120;
    var accessArrival = FACTORY.createAccessStopArrival(ITERATION_TIME, access);
    var transfer = FACTORY.createTransferStopArrival(
      accessArrival,
      TestTransfer.transfer(STOP_B, transferDuration),
      ITERATION_TIME + ACCESS_DURATION + transferDuration
    );

    var arrival = ride(transfer, TRIP_1);

    assertEquals(BOARD_TIME - BOARD_SLACK - transferDuration - ACCESS_DURATION, calculate(arrival));
  }

  @Test
  void onlyTheFirstTransitLegIsUsed() {
    var access = TestAccessEgress.walk(STOP_B, ACCESS_DURATION);
    var first = ride(FACTORY.createAccessStopArrival(ITERATION_TIME, access), TRIP_1);
    var transfer = FACTORY.createTransferStopArrival(first, TestTransfer.transfer(STOP_C, 60), ANY);

    var arrival = ride(transfer, TRIP_2, 0);

    assertEquals(BOARD_TIME - BOARD_SLACK - ACCESS_DURATION, calculate(arrival));
  }

  @Test
  void accessWithRidesAddsTransferSlack() {
    var access = TestAccessEgress.flex(STOP_B, ACCESS_DURATION);

    var arrival = ride(FACTORY.createAccessStopArrival(ITERATION_TIME, access), TRIP_1);

    assertEquals(BOARD_TIME - BOARD_SLACK - TRANSFER_SLACK - ACCESS_DURATION, calculate(arrival));
  }

  @Test
  void timePenaltyIsIncludedInTheDepartureTime() {
    int penalty = 120;
    // Raptor decorates the access with the time-penalty
    var access = new AccessWithPenalty(
      TestAccessEgress.walk(STOP_B, ACCESS_DURATION).withTimePenalty(penalty)
    );

    var arrival = ride(FACTORY.createAccessStopArrival(ITERATION_TIME, access), TRIP_1);

    assertEquals(BOARD_TIME - BOARD_SLACK - ACCESS_DURATION - penalty, calculate(arrival));
  }

  @Test
  void openingHoursLimitsTheLatestDeparture() {
    int closesAtDeparture = 700;
    var access = TestAccessEgress.walk(STOP_B, ACCESS_DURATION).openingHours(0, closesAtDeparture);

    var arrival = ride(FACTORY.createAccessStopArrival(ITERATION_TIME, access), TRIP_1);

    assertEquals(closesAtDeparture, calculate(arrival));
  }

  @Test
  void accessWithoutTransitDepartsAsEarlyAsPossibleAfterTheIterationTime() {
    var access = TestAccessEgress.walk(STOP_B, ACCESS_DURATION);
    var accessArrival = FACTORY.createAccessStopArrival(ITERATION_TIME, access);

    assertEquals(ITERATION_TIME, calculate(accessArrival));

    var delayed = FACTORY.createAccessStopArrival(
      ITERATION_TIME,
      TestAccessEgress.walk(STOP_B, ACCESS_DURATION).openingHours(ITERATION_TIME + 120, T01_00)
    );
    assertEquals(ITERATION_TIME + 120, calculate(delayed));
  }

  private int calculate(ArrivalView<TestTripSchedule> lastStopArrival) {
    return subject.calculate(lastStopArrival, ITERATION_TIME);
  }

  private static McStopArrival<TestTripSchedule> ride(
    McStopArrival<TestTripSchedule> previous,
    TestTripSchedule trip
  ) {
    return ride(previous, trip, 0);
  }

  private static McStopArrival<TestTripSchedule> ride(
    McStopArrival<TestTripSchedule> previous,
    TestTripSchedule trip,
    int boardStopPosition
  ) {
    return FACTORY.createTransitStopArrival(
      new PatternRideC1<>(
        previous,
        boardStopPosition,
        trip.departure(boardStopPosition),
        ANY,
        ANY,
        ANY,
        trip
      ),
      STOP_C,
      trip.arrival(1),
      ANY
    );
  }
}
