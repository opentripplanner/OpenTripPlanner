package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

public class AccessStopArrivalTest {

  private static final int ALIGHT_STOP = 100;
  private static final int DEPARTURE_TIME = 8 * 60 * 60;
  private static final int ACCESS_DURATION = 10 * 60;
  private static final int ALIGHT_TIME = DEPARTURE_TIME + ACCESS_DURATION;
  private static final TestAccessEgress WALK = TestAccessEgress.walk(ALIGHT_STOP, ACCESS_DURATION);
  private static final int COST = WALK.generalizedCost();

  private final AccessStopArrival<RaptorTripSchedule> subject = new AccessStopArrival<>(
    DEPARTURE_TIME,
    WALK
  );

  @Test
  public void arrivedByAccessLeg() {
    assertTrue(subject.arrivedByAccess());
    assertFalse(subject.arrivedByTransit());
    assertFalse(subject.arrivedByTransfer());
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
  public void cost() {
    assertEquals(COST, subject.cost());
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
      "Access { stop: 100, duration: 10m, arrival-time: 8:10 $1200 }",
      subject.toString()
    );
  }

  @Test
  public void timeShiftDefaultBehaviour() {
    final int dTime = 60;
    AbstractStopArrival<RaptorTripSchedule> result = subject.timeShiftNewArrivalTime(
      ALIGHT_TIME + dTime
    );

    assertEquals(result.arrivalTime(), ALIGHT_TIME + dTime);
    assertEquals(subject.cost(), result.cost());
    assertEquals(subject.travelDuration(), result.travelDuration());
    assertEquals(subject.round(), result.round());
    assertEquals(subject.stop(), result.stop());
    assertSame(subject.accessPath().access(), result.accessPath().access());
    assertEquals(subject.arrivedByAccess(), result.arrivedByAccess());
  }

  @Test
  public void timeShiftNotAllowed() {
    AbstractStopArrival<RaptorTripSchedule> original, result;
    RaptorAccessEgress access = access(-1);

    original = new AccessStopArrival<>(DEPARTURE_TIME, access);

    final int dTime = 60;
    result = original.timeShiftNewArrivalTime(ALIGHT_TIME + dTime);

    assertSame(original, result);
  }

  @Test
  public void timeShiftPartiallyAllowed() {
    final int dTime = 60;
    AbstractStopArrival<RaptorTripSchedule> original, result;

    // Allow time-shift, but only by dTime
    RaptorAccessEgress access = access(ALIGHT_TIME + dTime);

    original = new AccessStopArrival<>(DEPARTURE_TIME, access);

    result = original.timeShiftNewArrivalTime(ALIGHT_TIME + 7200);

    assertEquals(ALIGHT_TIME + dTime, result.arrivalTime());
  }

  private static RaptorAccessEgress access(final int latestArrivalTime) {
    return new RaptorAccessEgress() {
      @Override
      public int stop() {
        return 0;
      }

      @Override
      public boolean stopReachedOnBoard() {
        throw new NotImplementedException();
      }

      @Override
      public int generalizedCost() {
        return 0;
      }

      @Override
      public int durationInSeconds() {
        return 0;
      }

      @Override
      public int earliestDepartureTime(int t) {
        return t;
      }

      @Override
      public int latestArrivalTime(int requestedArrivalTime) {
        return latestArrivalTime;
      }

      @Override
      public boolean hasOpeningHours() {
        return true;
      }
    };
  }
}
