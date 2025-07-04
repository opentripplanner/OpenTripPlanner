package org.opentripplanner.model.modes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

class AllowAllModesFilterTest {

  private final AllowTransitModeFilter subject = new AllowAllModesFilter();

  @Test
  void allows() {
    assertTrue(subject.match(TransitMode.BUS, SubMode.UNKNOWN));
  }

  @Test
  void testHashCode() {
    assertEquals(new AllowAllModesFilter().hashCode(), subject.hashCode());
  }

  @Test
  void testEquals() {
    assertEquals(new AllowAllModesFilter(), subject);
  }

  @Test
  void testToString() {
    assertEquals("AllowAllModesFilter", subject.toString());
  }
}
