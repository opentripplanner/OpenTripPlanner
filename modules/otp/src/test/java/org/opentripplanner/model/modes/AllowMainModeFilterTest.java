package org.opentripplanner.model.modes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

class AllowMainModeFilterTest {

  private final AllowMainModeFilter subject = new AllowMainModeFilter(TransitMode.BUS);

  @Test
  void mainMode() {
    assertEquals(TransitMode.BUS, subject.mainMode());
  }

  @Test
  void allows() {
    assertTrue(subject.match(TransitMode.BUS, null));
    assertTrue(subject.match(TransitMode.BUS, SubMode.UNKNOWN));
    assertFalse(subject.match(TransitMode.TRAM, null));
  }

  @Test
  void testHashCode() {
    assertEquals(subject.hashCode(), new AllowMainModeFilter(TransitMode.BUS).hashCode());
    assertNotEquals(subject.hashCode(), new AllowMainModeFilter(TransitMode.TRAM).hashCode());
  }

  @Test
  void testEquals() {
    assertEquals(subject, new AllowMainModeFilter(TransitMode.BUS));
    assertNotEquals(subject, new AllowMainModeFilter(TransitMode.TRAM));
  }

  @Test
  void testToString() {
    assertEquals("AllowMainModeFilter{mainMode: BUS}", subject.toString());
  }
}
