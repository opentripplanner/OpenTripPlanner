package org.opentripplanner.model.modes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

class AllowMainAndSubModeFilterTest {

  private static final SubMode LOCAL_BUS = SubMode.getOrBuildAndCacheForever("localBus");

  private final AllowMainAndSubModeFilter subject = new AllowMainAndSubModeFilter(
    TransitMode.BUS,
    LOCAL_BUS
  );

  @Test
  void allows() {
    assertTrue(subject.match(TransitMode.BUS, LOCAL_BUS));
    assertFalse(subject.match(TransitMode.BUS, SubMode.UNKNOWN));
    assertFalse(subject.match(TransitMode.TRAM, LOCAL_BUS));
  }

  @Test
  void mainMode() {
    assertEquals(TransitMode.BUS, subject.mainMode());
  }

  @Test
  void subMode() {
    assertEquals(LOCAL_BUS, subject.subMode());
  }

  @Test
  void testHashCode() {
    assertEquals(
      subject.hashCode(),
      new AllowMainAndSubModeFilter(TransitMode.BUS, LOCAL_BUS).hashCode()
    );
    assertNotEquals(
      subject.hashCode(),
      new AllowMainAndSubModeFilter(TransitMode.TRAM, LOCAL_BUS).hashCode()
    );
    assertNotEquals(
      subject.hashCode(),
      new AllowMainAndSubModeFilter(TransitMode.BUS, SubMode.UNKNOWN).hashCode()
    );
  }

  @Test
  void testEquals() {
    assertEquals(subject, new AllowMainAndSubModeFilter(TransitMode.BUS, LOCAL_BUS));
    assertNotEquals(subject, new AllowMainAndSubModeFilter(TransitMode.TRAM, LOCAL_BUS));
    assertNotEquals(subject, new AllowMainAndSubModeFilter(TransitMode.BUS, SubMode.UNKNOWN));
  }

  @Test
  void testToString() {
    assertEquals("AllowMainAndSubModeFilter{mainMode: BUS, subMode: localBus}", subject.toString());
  }
}
