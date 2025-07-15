package org.opentripplanner.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.GenericLocation.UNKNOWN;

import org.junit.jupiter.api.Test;
import org.opentripplanner._support.asserts.AssertEqualsAndHashCode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class GenericLocationTest {

  private static final String LABEL = "A place";
  private static final FeedScopedId STOP_ID = new FeedScopedId("F", "Stop:1");
  private static final double LATITUDE = 20.0;
  private static final double LONGITUDE = 30.0;
  private final GenericLocation subject = new GenericLocation(LABEL, STOP_ID, LATITUDE, LONGITUDE);

  @Test
  void fromStopId() {
    assertEquals(STOP_ID, subject.stopId);
  }

  @Test
  void getCoordinate() {
    assertEquals(STOP_ID, subject.stopId);
  }

  @Test
  void isSpecified() {
    assertTrue(subject.isSpecified());
    assertFalse(UNKNOWN.isSpecified());
  }

  @Test
  void testEquals() {
    var copy = new GenericLocation(LABEL, STOP_ID, LATITUDE, LONGITUDE);
    AssertEqualsAndHashCode.verify(subject).sameAs(copy).differentFrom(UNKNOWN);
  }

  @Test
  void testToString() {
    assertEquals("Unknown location", UNKNOWN.toString());
    assertEquals("A place F:Stop:1 (20.0, 30.0)", subject.toString());
  }
}
