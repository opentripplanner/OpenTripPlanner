package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.c2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.raptor.api.model.PathLegType.TRANSIT;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

class TransitStopArrivalC2Test {

  private static final int BOARD_SLACK = 80;

  private static final int ACCESS_TO_STOP = 100;
  private static final int ACCESS_DEPARTURE_TIME = 8 * 60 * 60;
  private static final int ACCESS_DURATION = 300;
  private static final TestAccessEgress ACCESS_WALK = TestAccessEgress.walk(
    ACCESS_TO_STOP,
    ACCESS_DURATION
  );
  private static final int ACCESS_C1 = ACCESS_WALK.c1();
  private static final AccessStopArrivalC2<RaptorTripSchedule> ACCESS_ARRIVAL =
    new AccessStopArrivalC2<>(ACCESS_DEPARTURE_TIME, ACCESS_WALK);
  private static final int TRANSIT_TO_STOP = 101;
  private static final int TRANSIT_BOARD_TIME = 9 * 60 * 60;
  private static final int TRANSIT_LEG_DURATION = 1200;
  private static final int TRANSIT_ALIGHT_TIME = TRANSIT_BOARD_TIME + TRANSIT_LEG_DURATION;
  private static final RaptorTripSchedule TRANSIT_TRIP = TestTripSchedule.schedule(pattern("T1", 0))
    .arrivals(TRANSIT_ALIGHT_TIME)
    .build();
  private static final int TRANSIT_TRAVEL_DURATION =
    ACCESS_DURATION + BOARD_SLACK + TRANSIT_LEG_DURATION;
  private static final int TRANSIT_C1 = 128000;
  private static final int TRANSIT_C2 = 8000;
  private static final int ROUND = 1;
  private final TransitStopArrivalC2<RaptorTripSchedule> subject = new TransitStopArrivalC2<>(
    ACCESS_ARRIVAL.timeShiftNewArrivalTime(TRANSIT_BOARD_TIME - BOARD_SLACK),
    TRANSIT_TO_STOP,
    TRANSIT_ALIGHT_TIME,
    ACCESS_ARRIVAL.c1() + TRANSIT_C1,
    TRANSIT_C2,
    TRANSIT_TRIP
  );

  @Test
  public void round() {
    assertEquals(ROUND, subject.round());
  }

  @Test
  public void stop() {
    assertEquals(TRANSIT_TO_STOP, subject.stop());
  }

  @Test
  public void arrivedBy() {
    assertEquals(TRANSIT, subject.arrivedBy());
    assertTrue(subject.arrivedBy(TRANSIT));
  }

  @Test
  public void boardStop() {
    assertEquals(ACCESS_TO_STOP, subject.boardStop());
  }

  @Test
  public void arrivalTime() {
    assertEquals(TRANSIT_ALIGHT_TIME, subject.arrivalTime());
  }

  @Test
  public void c1() {
    assertEquals(ACCESS_C1 + TRANSIT_C1, subject.c1());
  }

  @Test
  public void c2() {
    assertEquals(TRANSIT_C2, subject.c2());
  }

  @Test
  public void trip() {
    assertSame(TRANSIT_TRIP, subject.trip());
  }

  @Test
  public void travelDuration() {
    assertEquals(TRANSIT_TRAVEL_DURATION, subject.travelDuration());
  }

  @Test
  public void access() {
    assertSame(ACCESS_ARRIVAL.accessPath().access(), subject.previous().accessPath().access());
  }

  @Test
  public void testToString() {
    assertEquals(
      "Transit { round: 1, stop: 101, arrival: [9:20 C₁1_880 C₂8000], pattern: BUS T1 }",
      subject.toString()
    );
  }
}
