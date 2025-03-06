package org.opentripplanner.ext.flex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FlexPathDurationsTest {

  private static final int ACCESS_DURATION_SEC = 1;
  private static final int TRIP_DURATION_SEC = 60;
  private static final int EGRESS_DURATION_SEC = 301;
  private static final int OFFSET = 3600;
  private static final int ACCESS_OFFSET = ACCESS_DURATION_SEC - OFFSET;
  private static final int EGRESS_OFFSET = -EGRESS_DURATION_SEC - OFFSET;

  private final FlexPathDurations subject = new FlexPathDurations(
    ACCESS_DURATION_SEC,
    TRIP_DURATION_SEC,
    EGRESS_DURATION_SEC,
    OFFSET
  );

  @Test
  void constructor() {
    assertThrows(IllegalArgumentException.class, () ->
      new FlexPathDurations(-1, TRIP_DURATION_SEC, EGRESS_DURATION_SEC, OFFSET)
    );
    assertThrows(IllegalArgumentException.class, () ->
      new FlexPathDurations(ACCESS_DURATION_SEC, -1, EGRESS_DURATION_SEC, OFFSET)
    );
    assertThrows(IllegalArgumentException.class, () ->
      new FlexPathDurations(ACCESS_DURATION_SEC, TRIP_DURATION_SEC, -1, OFFSET)
    );
  }

  @Test
  void testToString() {
    assertEquals("(1s + 1m + 5m1s, offset: 1h)", subject.toString());
    assertEquals("(0s + 1m + 0s)", new FlexPathDurations(0, 60, 0, 0).toString());
  }

  @Test
  void access() {
    assertEquals(ACCESS_DURATION_SEC, subject.access());
  }

  @Test
  void trip() {
    assertEquals(TRIP_DURATION_SEC, subject.trip());
  }

  @Test
  void egress() {
    assertEquals(EGRESS_DURATION_SEC, subject.egress());
  }

  @Test
  void mapToFlexTripDepartureTime() {
    assertEquals(300 + ACCESS_OFFSET, subject.mapToFlexTripDepartureTime(300));
  }

  @Test
  void mapToRouterDepartureTime() {
    assertEquals(300, subject.mapToRouterDepartureTime(300 + ACCESS_OFFSET));
  }

  @Test
  void mapToFlexTripArrivalTime() {
    assertEquals(300 + EGRESS_OFFSET, subject.mapToFlexTripArrivalTime(300));
  }

  @Test
  void mapToRouterArrivalTime() {
    assertEquals(300, subject.mapToRouterArrivalTime(300 + EGRESS_OFFSET));
  }
}
