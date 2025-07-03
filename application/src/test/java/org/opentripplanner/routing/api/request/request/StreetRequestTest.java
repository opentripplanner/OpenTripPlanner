package org.opentripplanner.routing.api.request.request;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner._support.asserts.AssertEqualsAndHashCode;
import org.opentripplanner.routing.api.request.StreetMode;

class StreetRequestTest {

  private static final StreetMode STREET_MODE = StreetMode.BIKE_RENTAL;

  private final StreetRequest subject = new StreetRequest(STREET_MODE);

  @Test
  void mode() {
    assertEquals(STREET_MODE, subject.mode());
    assertEquals(StreetMode.WALK, StreetRequest.DEFAULT.mode());
  }

  @Test
  void testEqualsAndHashCode() {
    AssertEqualsAndHashCode.verify(subject)
      .sameAs(new StreetRequest(STREET_MODE))
      .differentFrom(StreetRequest.DEFAULT, new StreetRequest(StreetMode.CAR));
  }

  @Test
  void testToString() {
    assertEquals("(mode: BIKE_RENTAL)", subject.toString());
    assertEquals("()", StreetRequest.DEFAULT.toString());
  }
}
