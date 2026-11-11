package org.opentripplanner.raptor.rangeraptor.destination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.stoparrival.BasicPathTestCase.C1_CALCULATOR;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop.McStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop.StopArrivalFactoryC1;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.PatternRideC1;
import org.opentripplanner.raptor.rangeraptor.transit.AccessWithPenalty;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;

/**
 * {@link TimeShiftedDepartureTime} must return the departure time of the path built from the same
 * arrival chain. The arrival chains here are created the way McRaptor does it: an access arrival
 * directly before a boarding is time-shifted by the routing strategy, an access followed by a
 * transfer is not. The expected value is always read from the path, not calculated in the test.
 */
class TimeShiftedDepartureTimeVersusPathTest implements RaptorTestConstants {

  private static final int ITERATION_TIME = T00_10;
  private static final int BOARD_SLACK = 60;
  private static final int TRANSFER_SLACK = 30;
  private static final int ACCESS_DURATION = 300;
  private static final int ANY = 9_999;

  private static final StopArrivalFactoryC1<TestTripSchedule> FACTORY =
    new StopArrivalFactoryC1<>();

  private static final TestTripSchedule TRIP = TestTripSchedule.schedule()
    .pattern("L1", STOP_B, STOP_C)
    .times("0:20 0:30")
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
  void accessBeforeBoarding() {
    var access = TestAccessEgress.walk(STOP_B, ACCESS_DURATION);

    assertSameDepartureTime(ride(access));
  }

  @Test
  void accessWithOpeningHoursBeforeBoarding() {
    var access = TestAccessEgress.walk(STOP_B, ACCESS_DURATION).openingHours(0, 700);

    assertSameDepartureTime(ride(access));
  }

  @Test
  void accessWithTimePenaltyBeforeBoarding() {
    var access = new AccessWithPenalty(
      TestAccessEgress.walk(STOP_B, ACCESS_DURATION).withTimePenalty(120)
    );

    assertSameDepartureTime(ride(access));
  }

  @Test
  void flexAccessBeforeBoarding() {
    var access = TestAccessEgress.flex(STOP_B, ACCESS_DURATION);

    assertSameDepartureTime(ride(access));
  }

  @Test
  void accessFollowedByTransferBeforeBoarding() {
    var access = TestAccessEgress.walk(STOP_A, ACCESS_DURATION);

    assertSameDepartureTime(rideAfterTransfer(access, 120));
  }

  @Test
  void flexAccessFollowedByTransferBeforeBoarding() {
    var access = TestAccessEgress.flex(STOP_A, ACCESS_DURATION);

    assertSameDepartureTime(rideAfterTransfer(access, 120));
  }

  @Test
  void accessWithOpeningHoursFollowedByTransferBeforeBoarding() {
    var access = TestAccessEgress.walk(STOP_A, ACCESS_DURATION).openingHours(0, 600);

    assertSameDepartureTime(rideAfterTransfer(access, 120));
  }

  private void assertSameDepartureTime(ArrivalView<TestTripSchedule> lastStopArrival) {
    var egress = TestAccessEgress.walk(STOP_C, 60);
    var destination = new DestinationArrival<>(
      egress,
      lastStopArrival,
      ITERATION_TIME,
      ITERATION_TIME,
      TRIP.arrival(1) + 60,
      0,
      0
    );
    var path = mapToPath(destination);

    assertEquals(
      path.startTimeInclusivePenalty(),
      subject.calculate(lastStopArrival, ITERATION_TIME),
      "Calculated departure time differs from the path.\n" +
        path.toStringDetailed(RaptorTestConstants::stopIndexToName)
    );
  }

  private static RaptorPath<TestTripSchedule> mapToPath(DestinationArrival<TestTripSchedule> a) {
    return new ForwardPathMapper<TestTripSchedule>(
      SLACK,
      C1_CALCULATOR,
      RaptorTestConstants::stopIndexToName,
      null
    ).mapToPath(a);
  }

  /** Access directly before the boarding, time-shifted like MultiCriteriaRoutingStrategy does. */
  private static McStopArrival<TestTripSchedule> ride(RaptorAccessEgress access) {
    var accessArrival = FACTORY.createAccessStopArrival(ITERATION_TIME, access);
    int latestArrivalTime = TRIP.departure(0) - BOARD_SLACK;
    return transit(accessArrival.timeShiftNewArrivalTime(latestArrivalTime));
  }

  /** Access, transfer, boarding: the access is NOT time-shifted by McRaptor in this case. */
  private static McStopArrival<TestTripSchedule> rideAfterTransfer(
    RaptorAccessEgress access,
    int transferDuration
  ) {
    var accessArrival = FACTORY.createAccessStopArrival(ITERATION_TIME, access);
    var transfer = FACTORY.createTransferStopArrival(
      accessArrival,
      TestTransfer.transfer(STOP_B, transferDuration),
      accessArrival.arrivalTime() + transferDuration
    );
    return transit(transfer);
  }

  private static McStopArrival<TestTripSchedule> transit(McStopArrival<TestTripSchedule> previous) {
    return FACTORY.createTransitStopArrival(
      new PatternRideC1<>(previous, 0, TRIP.departure(0), ANY, ANY, ANY, TRIP),
      STOP_C,
      TRIP.arrival(1),
      ANY
    );
  }
}
