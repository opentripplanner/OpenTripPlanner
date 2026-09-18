package org.opentripplanner.raptor.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class RaptorTripScheduleReferenceTest {

  private static final int ROUTE_INDEX = 3;
  private static final int TRIP_INDEX = 7;

  private final RaptorTripScheduleReference subject = new RaptorTripScheduleReference(
    ROUTE_INDEX,
    TRIP_INDEX
  );

  @Test
  void routeIndex() {
    assertEquals(ROUTE_INDEX, subject.routeIndex());
  }

  @Test
  void tripScheduleIndex() {
    assertEquals(TRIP_INDEX, subject.tripScheduleIndex());
  }

  @Test
  void testEquals() {
    assertEquals(subject, new RaptorTripScheduleReference(ROUTE_INDEX, TRIP_INDEX));
    assertNotEquals(subject, new RaptorTripScheduleReference(ROUTE_INDEX + 1, TRIP_INDEX));
    assertNotEquals(subject, new RaptorTripScheduleReference(ROUTE_INDEX, TRIP_INDEX + 1));
  }

  @Test
  void testHashCode() {
    assertEquals(
      subject.hashCode(),
      new RaptorTripScheduleReference(ROUTE_INDEX, TRIP_INDEX).hashCode()
    );
    assertNotEquals(
      subject.hashCode(),
      new RaptorTripScheduleReference(ROUTE_INDEX + 1, TRIP_INDEX).hashCode()
    );
  }

  @Test
  void testToString() {
    assertEquals("(route: 3, trip: 7)", subject.toString());
  }
}
