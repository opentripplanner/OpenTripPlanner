package org.opentripplanner.model.modes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.GeographicalScope;
import org.opentripplanner.transit.model.basic.NarrowedTransitMode;
import org.opentripplanner.transit.model.basic.ReplacementRequirement;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

public class FilterFactoryTest {

  private static final SubMode LOCAL_BUS = SubMode.getOrBuildAndCacheForever("localBus");
  private static final SubMode REGIONAL_BUS = SubMode.getOrBuildAndCacheForever("regionalBus");

  @Test
  void oneMain() {
    var filter = FilterFactory.create(List.of(NarrowedTransitMode.of(TransitMode.BUS).build()));
    assertEquals(AllowMainModeFilter.class, filter.getClass());
    assertEquals("AllowMainModeFilter{mainMode: BUS}", filter.toString());
  }

  @Test
  void multipleMains() {
    var filter = FilterFactory.create(
      List.of(
        NarrowedTransitMode.of(TransitMode.BUS).build(),
        NarrowedTransitMode.of(TransitMode.RAIL).build()
      )
    );
    assertEquals("AllowMainModesFilter{mainModes: [RAIL, BUS]}", filter.toString());
  }

  @Test
  void oneSub() {
    var filter = FilterFactory.create(
      List.of(NarrowedTransitMode.of(TransitMode.BUS).withSubMode(LOCAL_BUS).build())
    );
    assertEquals("AllowMainAndSubModeFilter{mainMode: BUS, subMode: localBus}", filter.toString());
  }

  @Test
  void multipleSubs() {
    var filter = FilterFactory.create(
      List.of(
        NarrowedTransitMode.of(TransitMode.BUS).withSubMode(LOCAL_BUS).build(),
        NarrowedTransitMode.of(TransitMode.BUS).withSubMode(REGIONAL_BUS).build()
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
        NarrowedTransitMode.of(TransitMode.BUS).build(),
        NarrowedTransitMode.of(TransitMode.BUS).withSubMode(LOCAL_BUS).build()
      )
    );
    assertEquals("AllowMainModeFilter{mainMode: BUS}", filter.toString());
  }

  @Test
  void oneReplacement() {
    var filter = FilterFactory.create(
      List.of(
        NarrowedTransitMode.of(TransitMode.BUS)
          .withReplacement(ReplacementRequirement.REQUIRED)
          .build()
      )
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
        NarrowedTransitMode.of(TransitMode.BUS)
          .withReplacement(ReplacementRequirement.REQUIRED)
          .build(),
        NarrowedTransitMode.of(TransitMode.RAIL)
          .withReplacement(ReplacementRequirement.FORBIDDEN)
          .build()
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
        NarrowedTransitMode.of(TransitMode.RAIL).build(),
        NarrowedTransitMode.of(TransitMode.RAIL)
          .withReplacement(ReplacementRequirement.FORBIDDEN)
          .build()
      )
    );
    assertEquals("AllowMainModeFilter{mainMode: RAIL}", filter.toString());
  }

  @Test
  void multipleGeographicalScopes() {
    var filter = FilterFactory.create(
      List.of(
        NarrowedTransitMode.of(TransitMode.RAIL)
          .withGeographicalScope(GeographicalScope.SUBURBAN)
          .build(),
        NarrowedTransitMode.of(TransitMode.RAIL)
          .withGeographicalScope(GeographicalScope.REGIONAL)
          .build(),
        NarrowedTransitMode.of(TransitMode.BUS).build()
      )
    );
    assertEquals(
      "FilterCollection[AllowMainModeFilter{mainMode: BUS}, AllowNarrowedTransitModesFilter{mainMode: RAIL, filters: [AllowNarrowedTransitModeFilter{mode: RAIL, isReplacement: IGNORED, geographicalScope: SUBURBAN}, AllowNarrowedTransitModeFilter{mode: RAIL, isReplacement: IGNORED, geographicalScope: REGIONAL}]}]",
      filter.toString()
    );
  }

  @Test
  void dropsCoveredGeographicalScope() {
    var filter = FilterFactory.create(
      List.of(
        NarrowedTransitMode.of(TransitMode.RAIL).build(),
        NarrowedTransitMode.of(TransitMode.RAIL)
          .withGeographicalScope(GeographicalScope.LONG_DISTANCE)
          .build()
      )
    );
    assertEquals("AllowMainModeFilter{mainMode: RAIL}", filter.toString());
  }

  @Test
  void excludedGeographicalScopes() {
    var filter = FilterFactory.create(
      List.of(
        NarrowedTransitMode.of(TransitMode.RAIL)
          .withExcludedGeographicalScopes(List.of(GeographicalScope.LONG_DISTANCE))
          .build()
      )
    );
    assertEquals(
      "AllowNarrowedTransitModeFilter{mode: RAIL, isReplacement: IGNORED, excludedGeographicalScopes: [LONG_DISTANCE]}",
      filter.toString()
    );
  }

  @Test
  void removesDuplicateNarrowedModes() {
    var filter = FilterFactory.create(
      List.of(
        NarrowedTransitMode.of(TransitMode.RAIL)
          .withGeographicalScope(GeographicalScope.SUBURBAN)
          .build(),
        NarrowedTransitMode.of(TransitMode.RAIL)
          .withGeographicalScope(GeographicalScope.SUBURBAN)
          .build()
      )
    );
    // A single filter, not an AllowNarrowedTransitModesFilter with two identical filters
    assertEquals(
      "AllowNarrowedTransitModeFilter{mode: RAIL, isReplacement: IGNORED, geographicalScope: SUBURBAN}",
      filter.toString()
    );
  }
}
