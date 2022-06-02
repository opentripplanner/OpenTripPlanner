package org.opentripplanner.model.modes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.network.SubMode;
import org.opentripplanner.transit.model.network.TransitMode;

class AllowAllModesFilterTest {

  private final AllowTransitModeFilter subject = AllowTransitModeFilter.ALLOWED_ALL_TRANSIT_MODES;

  @Test
  void allows() {
    assertTrue(subject.allows(TransitMode.BUS, SubMode.UNKNOWN));
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
    assertEquals(subject.getClass().getSimpleName() + "{}", subject.toString());
  }
}
