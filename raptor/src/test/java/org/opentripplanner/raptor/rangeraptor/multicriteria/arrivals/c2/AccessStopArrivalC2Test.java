package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.c2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.raptor.api.model.PathLegType.ACCESS;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;

class AccessStopArrivalC2Test {

  private static final int ALIGHT_STOP = 100;
  private static final int DEPARTURE_TIME = 8 * 60 * 60;
  private static final int ACCESS_DURATION = 10 * 60;
  private static final int ALIGHT_TIME = DEPARTURE_TIME + ACCESS_DURATION;
  private static final TestAccessEgress WALK = TestAccessEgress.walk(ALIGHT_STOP, ACCESS_DURATION);
  private static final int C1 = WALK.c1();

  private final AccessStopArrivalC2<RaptorTripSchedule> subject = new AccessStopArrivalC2<>(
    DEPARTURE_TIME,
    WALK
  );

  @Test
  public void arrivedBy() {
    assertEquals(ACCESS, subject.arrivedBy());
    assertTrue(subject.arrivedBy(ACCESS));
  }

  @Test
  public void stop() {
    assertEquals(ALIGHT_STOP, subject.stop());
  }

  @Test
  public void arrivalTime() {
    assertEquals(ALIGHT_TIME, subject.arrivalTime());
  }

  @Test
  public void c1() {
    assertEquals(C1, subject.c1());
  }

  @Test
  public void c2() {
    // We will add a more meaning-full implementation later
    assertEquals(RaptorCostCalculator.ZERO_COST, subject.c2());
  }

  @Test
  public void round() {
    assertEquals(0, subject.round());
  }

  @Test
  public void travelDuration() {
    assertEquals(ACCESS_DURATION, subject.travelDuration());
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  public void equalsThrowsExceptionByDesign() {
    assertThrows(IllegalStateException.class, () -> subject.equals(null));
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  public void hashCodeThrowsExceptionByDesign() {
    assertThrows(IllegalStateException.class, subject::hashCode);
  }

  @Test
  public void testToString() {
    assertEquals(
      "Access { stop: 100, arrival: [8:10 C₁1_200 C₂0], path: Walk 10m C₁1_200 ~ 100 }",
      subject.toString()
    );
  }

  @Test
  public void timeShiftDefaultBehaviour() {
    final int dTime = 60;
    McStopArrival<RaptorTripSchedule> result = subject.timeShiftNewArrivalTime(ALIGHT_TIME + dTime);

    assertEquals(result.arrivalTime(), ALIGHT_TIME + dTime);
    assertEquals(subject.c1(), result.c1());
    assertEquals(subject.travelDuration(), result.travelDuration());
    assertEquals(subject.round(), result.round());
    assertEquals(subject.stop(), result.stop());
    assertSame(subject.accessPath().access(), result.accessPath().access());
    assertEquals(subject.arrivedBy(), result.arrivedBy());
  }

  @Test
  public void timeShiftNotAllowed() {
    var access = TestAccessEgress.free(ALIGHT_STOP).openingHoursClosed();
    var original = new AccessStopArrivalC2<>(DEPARTURE_TIME, access);
    assertThrows(IllegalStateException.class, () -> original.timeShiftNewArrivalTime(6000));
  }

  @Test
  public void timeShiftPartiallyAllowed() {
    final int dTime = 60;
    McStopArrival<RaptorTripSchedule> original, result;

    // Allow time-shift, but only by dTime (a free edge has zero duration)
    RaptorAccessEgress access = TestAccessEgress.free(ALIGHT_STOP).openingHours(
      0,
      ALIGHT_TIME + dTime
    );

    original = new AccessStopArrivalC2<>(DEPARTURE_TIME, access);

    result = original.timeShiftNewArrivalTime(ALIGHT_TIME + 7200);

    assertEquals(ALIGHT_TIME + dTime, result.arrivalTime());
  }
}
