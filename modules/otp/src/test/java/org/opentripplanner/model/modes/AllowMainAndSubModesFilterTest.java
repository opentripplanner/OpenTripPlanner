package org.opentripplanner.model.modes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

class AllowMainAndSubModesFilterTest {

  final SubMode SUBMODE_LOCAL = SubMode.getOrBuildAndCacheForever("local");
  final SubMode SUBMODE_REGIONAL = SubMode.getOrBuildAndCacheForever("regional");
  final SubMode SUBMODE_EXPRESS = SubMode.getOrBuildAndCacheForever("express");
  final SubMode SUBMODE_NIGHT = SubMode.getOrBuildAndCacheForever("night");

  final AllowMainAndSubModeFilter filter1 = new AllowMainAndSubModeFilter(
    TransitMode.BUS,
    SUBMODE_LOCAL
  );
  final AllowMainAndSubModeFilter filter2 = new AllowMainAndSubModeFilter(
    TransitMode.BUS,
    SUBMODE_REGIONAL
  );
  final AllowMainAndSubModeFilter filter3 = new AllowMainAndSubModeFilter(
    TransitMode.BUS,
    SUBMODE_EXPRESS
  );

  final List<AllowMainAndSubModeFilter> filters = List.of(filter1, filter2, filter3);

  final AllowMainAndSubModesFilter subject = new AllowMainAndSubModesFilter(filters);

  @Test
  void allows() {
    assertTrue(subject.match(TransitMode.BUS, SUBMODE_LOCAL));
    assertTrue(subject.match(TransitMode.BUS, SUBMODE_REGIONAL));
    assertTrue(subject.match(TransitMode.BUS, SUBMODE_EXPRESS));
    assertFalse(subject.match(TransitMode.BUS, SUBMODE_NIGHT));
    assertFalse(subject.match(TransitMode.BUS, SubMode.of("other")));
  }

  @Test
  void testHashCode() {
    assertEquals(subject.hashCode(), new AllowMainAndSubModesFilter(filters).hashCode());
    assertNotEquals(
      subject.hashCode(),
      new AllowMainAndSubModesFilter(List.of(filter1, filter2)).hashCode()
    );
  }

  @Test
  void testEquals() {
    assertEquals(subject, new AllowMainAndSubModesFilter(filters));
    assertNotEquals(subject, new AllowMainAndSubModesFilter(List.of(filter1, filter2)));
  }

  @Test
  void testToString() {
    assertEquals(
      "AllowMainAndSubModesFilter{mainMode: BUS, subModes: [express, local, regional]}",
      subject.toString()
    );
  }
}
