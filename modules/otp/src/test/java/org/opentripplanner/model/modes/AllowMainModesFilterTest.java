package org.opentripplanner.model.modes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

class AllowMainModesFilterTest {

  final AllowMainModeFilter filter1 = new AllowMainModeFilter(TransitMode.BUS);
  final AllowMainModeFilter filter2 = new AllowMainModeFilter(TransitMode.TRAM);
  final AllowMainModeFilter filter3 = new AllowMainModeFilter(TransitMode.SUBWAY);
  final AllowMainModeFilter duplicate = new AllowMainModeFilter(TransitMode.SUBWAY);

  final List<AllowMainModeFilter> filters = List.of(filter1, filter2, filter3);
  final List<AllowMainModeFilter> filtersWDuplicte = List.of(filter1, filter2, filter3, duplicate);

  final AllowMainModesFilter subject = new AllowMainModesFilter(filtersWDuplicte);

  @Test
  void allows() {
    assertTrue(subject.match(TransitMode.BUS, SubMode.UNKNOWN));
    assertFalse(subject.match(TransitMode.RAIL, SubMode.UNKNOWN));
  }

  @Test
  void testHashCode() {
    assertEquals(subject.hashCode(), new AllowMainModesFilter(filters).hashCode());
    assertNotEquals(
      subject.hashCode(),
      new AllowMainModesFilter(List.of(filter1, filter2)).hashCode()
    );
  }

  @Test
  void testEquals() {
    assertEquals(subject, new AllowMainModesFilter(filters));
    assertNotEquals(subject, new AllowMainModesFilter(List.of(filter1, filter2)));
  }

  @Test
  void testToString() {
    assertEquals("AllowMainModesFilter{mainModes: [SUBWAY, BUS, TRAM]}", subject.toString());
  }
}
