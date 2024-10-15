package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.c1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.raptor.api.model.PathLegType.TRANSFER;
import static org.opentripplanner.raptor.api.model.PathLegType.TRANSIT;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

class TransferStopArrivalTest {

  private static final int BOARD_SLACK = 80;

  private static final int ACCESS_TO_STOP = 100;
  private static final int ACCESS_DEPARTURE_TIME = 8 * 60 * 60;
  private static final int ACCESS_DURATION = 300;
  private static final TestAccessEgress ACCESS_WALK = TestAccessEgress.walk(
    ACCESS_TO_STOP,
    ACCESS_DURATION
  );
  private static final int ACCESS_C1 = ACCESS_WALK.c1();

  private static final int TRANSIT_TO_STOP = 101;
  private static final int TRANSIT_BOARD_TIME = 9 * 60 * 60;
  private static final int TRANSIT_LEG_DURATION = 1200;
  private static final int TRANSIT_ALIGHT_TIME = TRANSIT_BOARD_TIME + TRANSIT_LEG_DURATION;
  private static final int TRANSIT_C1 = 128000;
  private static final RaptorTripSchedule TRANSIT_TRIP = null;
  private static final int ROUND = 1;

  private static final int TRANSFER_TO_STOP = 102;
  private static final int TRANSFER_LEG_DURATION = 360;
  private static final int TRANSFER_ALIGHT_TIME = TRANSIT_ALIGHT_TIME + TRANSFER_LEG_DURATION;
  private static final TestTransfer TRANSFER_WALK = TestTransfer.transfer(
    TRANSFER_TO_STOP,
    TRANSFER_LEG_DURATION
  );
  private static final int TRANSFER_C1 = TRANSFER_WALK.c1();

  private static final int EXPECTED_C1 = ACCESS_C1 + TRANSIT_C1 + TRANSFER_C1;

  private static final AccessStopArrival<RaptorTripSchedule> ACCESS_ARRIVAL = new AccessStopArrival<>(
    ACCESS_DEPARTURE_TIME,
    ACCESS_WALK
  );

  private static final TransitStopArrival<RaptorTripSchedule> TRANSIT_ARRIVAL = new TransitStopArrival<>(
    ACCESS_ARRIVAL.timeShiftNewArrivalTime(TRANSIT_BOARD_TIME - BOARD_SLACK),
    TRANSIT_TO_STOP,
    TRANSIT_ALIGHT_TIME,
    ACCESS_ARRIVAL.c1() + TRANSIT_C1,
    TRANSIT_TRIP
  );

  private final TransferStopArrival<RaptorTripSchedule> subject = new TransferStopArrival<>(
    TRANSIT_ARRIVAL,
    TRANSFER_WALK,
    TRANSFER_ALIGHT_TIME
  );

  @Test
  public void arrivedByTransfer() {
    assertTrue(subject.arrivedBy(TRANSFER));
    assertFalse(subject.arrivedBy(TRANSIT));
  }

  @Test
  public void stop() {
    assertEquals(TRANSFER_TO_STOP, subject.stop());
  }

  @Test
  public void arrivalTime() {
    assertEquals(TRANSFER_ALIGHT_TIME, subject.arrivalTime());
  }

  @Test
  public void c1() {
    assertEquals(EXPECTED_C1, subject.c1());
  }

  @Test
  public void c2() {
    assertEquals(RaptorConstants.NOT_SET, subject.c2());
  }

  @Test
  public void travelDuration() {
    assertEquals(
      ACCESS_DURATION + BOARD_SLACK + TRANSIT_LEG_DURATION + TRANSFER_LEG_DURATION,
      subject.travelDuration()
    );
  }

  @Test
  public void round() {
    assertEquals(ROUND, subject.round());
  }

  @Test
  public void previous() {
    assertSame(TRANSIT_ARRIVAL, subject.previous());
  }

  @Test
  public void testToString() {
    assertEquals(
      "Walk { round: 1, stop: 102, arrival: [9:26 C₁2_600], path: On-Street 6m ~ 102 }",
      subject.toString()
    );
  }
}
