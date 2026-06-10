package org.opentripplanner.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.opentripplanner._support.asserts.AssertEqualsAndHashCode;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.street.geometry.WgsCoordinate;

class GenericLocationTest {

  private static final String LABEL = "A place";
  private static final FeedScopedId STOP_ID = new FeedScopedId("F", "Stop:1");
  private static final double LATITUDE = 20.0;
  private static final double LONGITUDE = 30.0;
  private final GenericLocation subject = GenericLocation.fromStopIdWithFallback(
    STOP_ID,
    LATITUDE,
    LONGITUDE,
    LABEL
  );

  private final GenericLocation other = GenericLocation.fromCoordinate(LATITUDE, LONGITUDE, LABEL);

  @Test
  void fromStopIdWithFallback() {
    var location = GenericLocation.fromStopIdWithFallback(STOP_ID, LATITUDE, LONGITUDE, LABEL);
    assertEquals(STOP_ID, location.stopId());
    assertEquals(new WgsCoordinate(LATITUDE, LONGITUDE), location.wgsCoordinate());
    assertEquals(LABEL, location.label());
  }

  @Test
  void fromStopId() {
    var location = GenericLocation.fromStopId(STOP_ID, LABEL);
    assertEquals(STOP_ID, location.stopId());
    assertNull(location.wgsCoordinate());
    assertEquals(LABEL, location.label());
  }

  @Test
  void fromCoordinate() {
    var location = GenericLocation.fromCoordinate(LATITUDE, LONGITUDE, LABEL);
    assertNull(location.stopId());
    assertEquals(new WgsCoordinate(LATITUDE, LONGITUDE), location.wgsCoordinate());
    assertEquals(LABEL, location.label());
  }

  @Test
  void testInvalid() {
    assertThrows(NullPointerException.class, () -> GenericLocation.fromStopId(null));
    assertThrows(NullPointerException.class, () ->
      GenericLocation.fromStopIdWithFallback(null, 0.0, 0.0, "label")
    );
  }

  @Test
  void testEquals() {
    var copy = GenericLocation.fromStopIdWithFallback(STOP_ID, LATITUDE, LONGITUDE, LABEL);
    AssertEqualsAndHashCode.verify(subject).sameAs(copy).differentFrom(other);
  }

  @Test
  void testToString() {
    assertEquals("A place F:Stop:1 (20.0, 30.0)", subject.toString());
  }
}
