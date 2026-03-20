package org.opentripplanner.model.modes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.NarrowedTransitMode;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

public class AllowNarrowedTransitModeFilterTest {

  private static final SubMode LOCAL_BUS = SubMode.getOrBuildAndCacheForever("localBus");

  private final AllowNarrowedTransitModeFilter fullFilter = new AllowNarrowedTransitModeFilter(
    new NarrowedTransitMode(TransitMode.BUS, LOCAL_BUS, Boolean.TRUE)
  );
  private final AllowNarrowedTransitModeFilter nullSubmodeFilter =
    new AllowNarrowedTransitModeFilter(
      new NarrowedTransitMode(TransitMode.BUS, null, Boolean.TRUE)
    );
  private final AllowNarrowedTransitModeFilter nullReplacementFilter =
    new AllowNarrowedTransitModeFilter(new NarrowedTransitMode(TransitMode.BUS, LOCAL_BUS, null));

  @Test
  void allows() {
    assertTrue(fullFilter.match(TransitMode.BUS, LOCAL_BUS, null));
    assertTrue(fullFilter.match(TransitMode.BUS, SubMode.UNKNOWN, 714));
    assertFalse(fullFilter.match(TransitMode.TRAM, LOCAL_BUS, 714));

    assertTrue(nullSubmodeFilter.match(TransitMode.BUS, LOCAL_BUS, 714));
    assertFalse(nullSubmodeFilter.match(TransitMode.BUS, LOCAL_BUS, 700));
    assertFalse(nullSubmodeFilter.match(TransitMode.BUS, LOCAL_BUS, null));
    assertFalse(nullSubmodeFilter.match(TransitMode.TRAM, LOCAL_BUS, null));

    assertTrue(nullReplacementFilter.match(TransitMode.BUS, LOCAL_BUS));
    assertFalse(nullReplacementFilter.match(TransitMode.BUS, SubMode.UNKNOWN));
    assertFalse(nullReplacementFilter.match(TransitMode.TRAM, LOCAL_BUS));
  }
}
