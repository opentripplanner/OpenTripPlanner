package org.opentripplanner.model.modes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.NarrowedTransitMode;
import org.opentripplanner.transit.model.basic.ReplacementRequirement;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

public class AllowNarrowedTransitModeFilterTest {

  private static final SubMode LOCAL_BUS = SubMode.getOrBuildAndCacheForever("localBus");
  private static final SubMode RAIL_REPLACEMENT_BUS = SubMode.getOrBuildAndCacheForever(
    "railReplacementBus"
  );

  private final AllowNarrowedTransitModeFilter fullFilter = new AllowNarrowedTransitModeFilter(
    new NarrowedTransitMode(TransitMode.BUS, LOCAL_BUS, ReplacementRequirement.REQUIRED)
  );
  private final AllowNarrowedTransitModeFilter nullSubmodeFilter =
    new AllowNarrowedTransitModeFilter(
      new NarrowedTransitMode(TransitMode.BUS, null, ReplacementRequirement.REQUIRED)
    );
  private final AllowNarrowedTransitModeFilter nullReplacementFilter =
    new AllowNarrowedTransitModeFilter(
      new NarrowedTransitMode(TransitMode.BUS, LOCAL_BUS, ReplacementRequirement.IGNORED)
    );

  @Test
  void match() {
    // filter.match will always be called with either netexSubmode == SubMode.UNKNOWN or gtfsExtendedType == null
    // depending on the data source type. Both can be true if the data is from a GTFS source and the extended
    // type just happens to be not set.
    assertTrue(fullFilter.match(TransitMode.BUS, LOCAL_BUS, null));
    assertTrue(fullFilter.match(TransitMode.BUS, SubMode.UNKNOWN, 714));
    assertFalse(fullFilter.match(TransitMode.TRAM, SubMode.UNKNOWN, 714));

    assertTrue(nullSubmodeFilter.match(TransitMode.BUS, SubMode.UNKNOWN, 714));
    assertFalse(nullSubmodeFilter.match(TransitMode.BUS, SubMode.UNKNOWN, 700));
    assertFalse(nullSubmodeFilter.match(TransitMode.BUS, LOCAL_BUS, null));
    assertFalse(nullSubmodeFilter.match(TransitMode.TRAM, LOCAL_BUS, null));

    assertTrue(nullSubmodeFilter.match(TransitMode.BUS, RAIL_REPLACEMENT_BUS, null));
    assertFalse(nullSubmodeFilter.match(TransitMode.TRAM, RAIL_REPLACEMENT_BUS, null));

    assertTrue(nullReplacementFilter.match(TransitMode.BUS, LOCAL_BUS, null));
    assertFalse(nullReplacementFilter.match(TransitMode.BUS, SubMode.UNKNOWN, null));
    assertFalse(nullReplacementFilter.match(TransitMode.TRAM, LOCAL_BUS, null));
  }
}
