package org.opentripplanner.model.modes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

class PathTailFilterFactoryTest {

  private static final SubMode LOCAL_BUS = SubMode.getOrBuildAndCacheForever("localBus");
  private static final SubMode EXPRESS_BUS = SubMode.getOrBuildAndCacheForever("expressBus");
  private static final SubMode NIGHT_BUS = SubMode.getOrBuildAndCacheForever("nightBus");

  @Test
  void createEmptySet() {
    assertThrows(IllegalArgumentException.class, () -> AllowTransitModeFilter.of(List.of()));
  }

  @Test
  void createOneMainModesSet() {
    var modes = List.of(
      new MainAndSubMode(TransitMode.BUS),
      // Add one extra sub-mode filter, this should be removed because it include a subset of the
      // MainMode filter for BUS
      new MainAndSubMode(TransitMode.BUS, NIGHT_BUS)
    );

    var result = (AllowMainModeFilter) AllowTransitModeFilter.of(modes);
    assertEquals(TransitMode.BUS, result.mainMode());
  }

  @Test
  void createAllMainModesSet() {
    var modes = new ArrayList<>(MainAndSubMode.all());

    // Add one extra sub-mode filter, this should be removed because it is a subset of all
    modes.add(new MainAndSubMode(TransitMode.BUS, NIGHT_BUS));

    var result = AllowTransitModeFilter.of(modes);
    assertInstanceOf(AllowAllModesFilter.class, result);
  }

  @Test
  void createAllMainModesSetExceptOne() {
    // Add all MainModes except AIRPLANE as separate filters
    var modes = TransitMode.transitModesExceptAirplane().stream().map(MainAndSubMode::new).toList();

    var result = (AllowMainModesFilter) AllowTransitModeFilter.of(modes);

    for (TransitMode mode : TransitMode.values()) {
      assertEquals(mode != TransitMode.AIRPLANE, result.match(mode, SubMode.UNKNOWN));
    }
  }

  @Test
  void createOneSubModeFilter() {
    // Add ALL MainModes as separate filters
    var modes = List.of(new MainAndSubMode(TransitMode.BUS, LOCAL_BUS));

    var result = (AllowMainAndSubModeFilter) AllowTransitModeFilter.of(modes);

    assertEquals(TransitMode.BUS, result.mainMode());
    assertEquals(LOCAL_BUS, result.subMode());
  }

  @Test
  void createTwoSubModeFilter() {
    var modes = List.of(
      new MainAndSubMode(TransitMode.BUS, LOCAL_BUS),
      new MainAndSubMode(TransitMode.BUS, NIGHT_BUS)
    );

    var result = AllowTransitModeFilter.of(modes);

    assertTrue(result.match(TransitMode.BUS, LOCAL_BUS));
    assertTrue(result.match(TransitMode.BUS, NIGHT_BUS));
    assertFalse(result.match(TransitMode.BUS, SubMode.UNKNOWN));
  }

  @Test
  void createThreeSubModeFilterIntoOneFilterWithBitSet() {
    // Add ALL MainModes as separate filters
    var modes = List.of(
      new MainAndSubMode(TransitMode.BUS, LOCAL_BUS),
      new MainAndSubMode(TransitMode.BUS, EXPRESS_BUS),
      new MainAndSubMode(TransitMode.BUS, NIGHT_BUS)
    );

    var result = (AllowMainAndSubModesFilter) AllowTransitModeFilter.of(modes);

    assertTrue(result.match(TransitMode.BUS, LOCAL_BUS));
    assertFalse(result.match(TransitMode.TRAM, LOCAL_BUS));
    assertFalse(result.match(TransitMode.BUS, SubMode.UNKNOWN));
  }
}
