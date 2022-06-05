package org.opentripplanner.model.modes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.network.MainAndSubMode;
import org.opentripplanner.transit.model.network.SubMode;
import org.opentripplanner.transit.model.network.TransitMode;

class FilterFactoryTest {

  private static final SubMode LOCAL_BUS = SubMode.getOrBuildAndCashForever("localBus");
  private static final SubMode EXPRESS_BUS = SubMode.getOrBuildAndCashForever("expressBus");
  private static final SubMode NIGHT_BUS = SubMode.getOrBuildAndCashForever("nightBus");

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

    var resultSet = AllowTransitModeFilter.of(modes);
    assertEquals(1, resultSet.size());
    assertInstanceOf(AllowMainModeFilter.class, resultSet.iterator().next());
  }

  @Test
  void createAllMainModesSet() {
    var modes = new ArrayList<>(MainAndSubMode.all());

    // Add one extra sub-mode filter, this should be removed because it is a subset of all
    modes.add(new MainAndSubMode(TransitMode.BUS, NIGHT_BUS));

    var resultSet = AllowTransitModeFilter.of(modes);
    assertEquals(1, resultSet.size());
    assertInstanceOf(AllowAllModesFilter.class, resultSet.iterator().next());
  }

  @Test
  void createAllMainModesSetExceptOne() {
    // Add all MainModes except AIRPLANE as separate filters
    var modes = TransitMode.transitModesExceptAirplane().stream().map(MainAndSubMode::new).toList();

    var resultSet = AllowTransitModeFilter.of(modes);
    assertEquals(1, resultSet.size());

    // The cast wil throw an exception is not the correct type is returned
    var filter = (AllowMainModesFilter) resultSet.iterator().next();

    for (TransitMode mode : TransitMode.values()) {
      assertEquals(mode != TransitMode.AIRPLANE, filter.allows(mode, SubMode.UNKNOWN));
    }
  }

  @Test
  void createOneSubModeFilter() {
    // Add ALL MainModes as separate filters
    var modes = List.of(new MainAndSubMode(TransitMode.BUS, LOCAL_BUS));

    var resultSet = AllowTransitModeFilter.of(modes);
    assertEquals(1, resultSet.size());
    var filter = (AllowMainAndSubModeFilter) resultSet.iterator().next();
    assertEquals(TransitMode.BUS, filter.mainMode());
    assertEquals(LOCAL_BUS, filter.subMode());
  }

  @Test
  void createTwoSubModeFilter() {
    var modes = List.of(
      new MainAndSubMode(TransitMode.BUS, LOCAL_BUS),
      new MainAndSubMode(TransitMode.BUS, NIGHT_BUS)
    );

    var resultSet = List.copyOf(AllowTransitModeFilter.of(modes));
    assertEquals(2, resultSet.size());

    assertTrue(allows(resultSet, TransitMode.BUS, LOCAL_BUS));
    assertTrue(allows(resultSet, TransitMode.BUS, NIGHT_BUS));
    assertFalse(allows(resultSet, TransitMode.BUS, SubMode.UNKNOWN));
  }

  @Test
  void createThreeSubModeFilterIntoOneFilterWithBitSet() {
    // Add ALL MainModes as separate filters
    var modes = List.of(
      new MainAndSubMode(TransitMode.BUS, LOCAL_BUS),
      new MainAndSubMode(TransitMode.BUS, EXPRESS_BUS),
      new MainAndSubMode(TransitMode.BUS, NIGHT_BUS)
    );

    var resultSet = AllowTransitModeFilter.of(modes);
    assertEquals(1, resultSet.size());

    var result = (AllowMainAndSubModesFilter) resultSet.iterator().next();

    assertTrue(result.allows(TransitMode.BUS, LOCAL_BUS));
    assertFalse(result.allows(TransitMode.TRAM, LOCAL_BUS));
    assertFalse(result.allows(TransitMode.BUS, SubMode.UNKNOWN));
  }

  private static boolean allows(
    Collection<AllowTransitModeFilter> filters,
    TransitMode mainMode,
    SubMode subMode
  ) {
    for (AllowTransitModeFilter it : filters) {
      if (it.allows(mainMode, subMode)) {
        return true;
      }
    }
    return false;
  }
}
