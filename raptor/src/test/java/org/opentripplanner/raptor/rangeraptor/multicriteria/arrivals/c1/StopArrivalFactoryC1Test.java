package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.c1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTripPattern;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.view.PatternRideView;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.c1.PatternRideC1;
import org.opentripplanner.utils.time.DurationUtils;
import org.opentripplanner.utils.time.TimeUtils;

public class StopArrivalFactoryC1Test {

  private static final int STOP_A = 7;
  private static final int STOP_B = 4;
  private static final int STOP_C = 12;
  private static final int ANY = 17543;
  private static final int ORIGIN_DEPARTURE_TIME = TimeUtils.time("10:00");
  private static final int ACCESS_DURATION = 30;
  private static final int TRANSFER_DURATION = 30;

  // We add a alight slack of 1 minute here (see trip below)
  private static final int STOP_ARRIVAL_TRANSIT_TIME = TimeUtils.time("10:11");
  private static final int STOP_ARRIVAL_TRANSIT_C1 = 63000;
  private static final TestAccessEgress ACCESS = TestAccessEgress.walk(STOP_A, ACCESS_DURATION);

  private static final TestTripSchedule TRIP = TestTripSchedule.schedule("10:03 10:10")
    .pattern(TestTripPattern.pattern("Line A", STOP_A, STOP_B))
    .build();

  private static final TestTransfer TRANSFER = TestTransfer.transfer(STOP_C, TRANSFER_DURATION);

  private final StopArrivalFactoryC1<TestTripSchedule> subject = new StopArrivalFactoryC1<>();

  private McStopArrival<TestTripSchedule> accessArrival() {
    return subject.createAccessStopArrival(ORIGIN_DEPARTURE_TIME, ACCESS);
  }

  private McStopArrival<TestTripSchedule> transitArrival() {
    PatternRideView<TestTripSchedule, McStopArrival<TestTripSchedule>> ride = new PatternRideC1<>(
      accessArrival(),
      STOP_A,
      0,
      TRIP.departure(0),
      ANY,
      ANY,
      TRIP.tripSortIndex(),
      TRIP
    );

    return subject.createTransitStopArrival(
      ride,
      TRIP.pattern().stopIndex(1),
      STOP_ARRIVAL_TRANSIT_TIME,
      STOP_ARRIVAL_TRANSIT_C1
    );
  }

  private McStopArrival<TestTripSchedule> transferArrival(
    McStopArrival<TestTripSchedule> previousArrival
  ) {
    return subject.createTransferStopArrival(
      previousArrival,
      TRANSFER,
      previousArrival.arrivalTime() + TRANSFER_DURATION
    );
  }

  @Test
  public void testCreateAccessStopArrival() {
    var stopArrival = accessArrival();

    assertEquals(STOP_A, stopArrival.stop());
    assertEquals(ACCESS.c1(), stopArrival.c1());
    assertEquals(ACCESS_DURATION, stopArrival.travelDuration());
    assertEquals(ORIGIN_DEPARTURE_TIME + ACCESS_DURATION, stopArrival.arrivalTime());

    // c2 not supported
    assertEquals(RaptorConstants.NOT_SET, stopArrival.c2());
  }

  @Test
  public void testCreateTransitStopArrival() {
    var stopArrival = transitArrival();

    assertEquals(STOP_B, stopArrival.stop());
    assertEquals(STOP_ARRIVAL_TRANSIT_C1, stopArrival.c1());
    assertEquals(STOP_ARRIVAL_TRANSIT_TIME, stopArrival.arrivalTime());
    assertEquals(STOP_ARRIVAL_TRANSIT_TIME - ORIGIN_DEPARTURE_TIME, stopArrival.travelDuration());

    // c2 not supported
    assertEquals(RaptorConstants.NOT_SET, stopArrival.c2());
  }

  @Test
  public void testCreateTransferStopArrival() {
    var prevArrival = transitArrival();
    var stopArrival = transferArrival(prevArrival);

    assertEquals(STOP_C, stopArrival.stop());
    assertEquals(prevArrival.c1() + TRANSFER.c1(), stopArrival.c1());
    assertEquals(
      prevArrival.arrivalTime() + TRANSFER.durationInSeconds(),
      stopArrival.arrivalTime()
    );
    assertEquals(DurationUtils.durationInSeconds("11m30s"), stopArrival.travelDuration());

    // c2 not supported
    assertEquals(RaptorConstants.NOT_SET, stopArrival.c2());
  }
}
