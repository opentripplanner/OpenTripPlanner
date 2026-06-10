package org.opentripplanner.model.modes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.NarrowedTransitMode;
import org.opentripplanner.transit.model.basic.ReplacementRequirement;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

public class FilterFactoryTest {

  private static final SubMode LOCAL_BUS = SubMode.getOrBuildAndCacheForever("localBus");
  private static final SubMode REGIONAL_BUS = SubMode.getOrBuildAndCacheForever("regionalBus");

  @Test
  void oneMain() {
    var filter = FilterFactory.create(
      List.of(new NarrowedTransitMode(TransitMode.BUS, null, ReplacementRequirement.IGNORED))
    );
    assertEquals(AllowMainModeFilter.class, filter.getClass());
    assertEquals("AllowMainModeFilter{mainMode: BUS}", filter.toString());
  }

  @Test
  void multipleMains() {
    var filter = FilterFactory.create(
      List.of(
        new NarrowedTransitMode(TransitMode.BUS, null, ReplacementRequirement.IGNORED),
        new NarrowedTransitMode(TransitMode.RAIL, null, ReplacementRequirement.IGNORED)
      )
    );
    assertEquals("AllowMainModesFilter{mainModes: [RAIL, BUS]}", filter.toString());
  }

  @Test
  void oneSub() {
    var filter = FilterFactory.create(
      List.of(new NarrowedTransitMode(TransitMode.BUS, LOCAL_BUS, ReplacementRequirement.IGNORED))
    );
    assertEquals("AllowMainAndSubModeFilter{mainMode: BUS, subMode: localBus}", filter.toString());
  }

  @Test
  void multipleSubs() {
    var filter = FilterFactory.create(
      List.of(
        new NarrowedTransitMode(TransitMode.BUS, LOCAL_BUS, ReplacementRequirement.IGNORED),
        new NarrowedTransitMode(TransitMode.BUS, REGIONAL_BUS, ReplacementRequirement.IGNORED)
      )
    );
    assertEquals(
      "AllowMainAndSubModesFilter{mainMode: BUS, subModes: [localBus, regionalBus]}",
      filter.toString()
    );
  }

  @Test
  void dropsCoveredSub() {
    var filter = FilterFactory.create(
      List.of(
        new NarrowedTransitMode(TransitMode.BUS, null, ReplacementRequirement.IGNORED),
        new NarrowedTransitMode(TransitMode.BUS, LOCAL_BUS, ReplacementRequirement.IGNORED)
      )
    );
    assertEquals("AllowMainModeFilter{mainMode: BUS}", filter.toString());
  }

  @Test
  void oneReplacement() {
    var filter = FilterFactory.create(
      List.of(new NarrowedTransitMode(TransitMode.BUS, null, ReplacementRequirement.REQUIRED))
    );
    assertEquals(
      "AllowNarrowedTransitModeFilter{mode: BUS, isReplacement: REQUIRED}",
      filter.toString()
    );
  }

  @Test
  void multipleReplacements() {
    var filter = FilterFactory.create(
      List.of(
        new NarrowedTransitMode(TransitMode.BUS, null, ReplacementRequirement.REQUIRED),
        new NarrowedTransitMode(TransitMode.RAIL, null, ReplacementRequirement.FORBIDDEN)
      )
    );
    assertEquals(
      "FilterCollection[AllowNarrowedTransitModeFilter{mode: BUS, isReplacement: REQUIRED}, AllowNarrowedTransitModeFilter{mode: RAIL, isReplacement: FORBIDDEN}]",
      filter.toString()
    );
  }

  @Test
  void dropsCoveredReplacement() {
    var filter = FilterFactory.create(
      List.of(
        new NarrowedTransitMode(TransitMode.RAIL, null, ReplacementRequirement.IGNORED),
        new NarrowedTransitMode(TransitMode.RAIL, null, ReplacementRequirement.FORBIDDEN)
      )
    );
    assertEquals("AllowMainModeFilter{mainMode: RAIL}", filter.toString());
  }
}
