package org.opentripplanner.raptor.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class RaptorTripScheduleStopPositionTest {

  private static final int ROUTE_INDEX = 7;
  private static final int TRIP_SCHEDULE_INDEX = 3;
  private static final int STOP_POSITION_IN_PATTERN = 5;
  private static final int OTHER_VALUE = 99;

  private final RaptorTripScheduleStopPosition subject = new RaptorTripScheduleStopPosition(
    ROUTE_INDEX,
    TRIP_SCHEDULE_INDEX,
    STOP_POSITION_IN_PATTERN
  );

  @Test
  void routeIndex() {
    assertEquals(ROUTE_INDEX, subject.routeIndex());
  }

  @Test
  void tripScheduleIndex() {
    assertEquals(TRIP_SCHEDULE_INDEX, subject.tripScheduleIndex());
  }

  @Test
  void stopPositionInPattern() {
    assertEquals(STOP_POSITION_IN_PATTERN, subject.stopPositionInPattern());
  }

  @Test
  void testEqualsAndHashCode() {
    var same = new RaptorTripScheduleStopPosition(
      ROUTE_INDEX,
      TRIP_SCHEDULE_INDEX,
      STOP_POSITION_IN_PATTERN
    );
    var other1 = new RaptorTripScheduleStopPosition(
      OTHER_VALUE,
      TRIP_SCHEDULE_INDEX,
      STOP_POSITION_IN_PATTERN
    );
    var other2 = new RaptorTripScheduleStopPosition(
      ROUTE_INDEX,
      OTHER_VALUE,
      STOP_POSITION_IN_PATTERN
    );
    var other3 = new RaptorTripScheduleStopPosition(ROUTE_INDEX, TRIP_SCHEDULE_INDEX, OTHER_VALUE);

    // TODO: Move AssertEqualsAndHashCode into utils and use it here
    assertEquals(same, subject);
    assertEquals(same.hashCode(), subject.hashCode());
    assertNotEquals(other1, subject);
    assertNotEquals(other1.hashCode(), subject.hashCode());
    assertNotEquals(other2, subject);
    assertNotEquals(other2.hashCode(), subject.hashCode());
    assertNotEquals(other3, subject);
    assertNotEquals(other3.hashCode(), subject.hashCode());
  }

  @Test
  void testToString() {
    assertEquals("[route: 7, trip: 3, pos: 5]", subject.toString());
  }
}
