package org.opentripplanner.model.modes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.network.SubMode;
import org.opentripplanner.transit.model.network.TransitMode;

class FilterMergerTest {

  private static final SubMode LOCAL_BUS = SubMode.getOrBuildAndCashForever("localBus");
  private static final SubMode EXPRESS_BUS = SubMode.getOrBuildAndCashForever("expressBus");
  private static final SubMode NIGHT_BUS = SubMode.getOrBuildAndCashForever("nightBus");

  @Test
  void mergeEmptySet() {
    var resultSet = AllowTransitModeFilter.merge(List.of());
    assertEquals(1, resultSet.size());
    assertInstanceOf(AllowAllModesFilter.class, resultSet.iterator().next());
  }

  @Test
  void mergeOneMainModesSet() {
    // Add a MainMode filter
    var filters = AllowTransitModeFilter.ofMainModes(TransitMode.BUS);

    // Add one extra sub-mode filter, this should be removed because it include a subset of the
    // MainMode filter for BUS
    filters.add(new AllowMainAndSubModeFilter(TransitMode.BUS, NIGHT_BUS));

    var resultSet = AllowTransitModeFilter.merge(filters);
    assertEquals(1, resultSet.size());
    assertInstanceOf(AllowMainModeFilter.class, resultSet.iterator().next());
  }

  @Test
  void mergeAllMainModesSet() {
    // Add ALL MainModes as separate filters
    var filters = new ArrayList<AllowTransitModeFilter>(
      AllowTransitModeFilter.ofMainModes(TransitMode.values())
    );

    // Add one extra sub-mode filter, this should be removed because it include a subset of the
    // MainMode filter for BUS
    filters.add(new AllowMainAndSubModeFilter(TransitMode.BUS, NIGHT_BUS));

    var resultSet = AllowTransitModeFilter.merge(filters);
    assertEquals(1, resultSet.size());
    assertInstanceOf(AllowAllModesFilter.class, resultSet.iterator().next());
  }

  @Test
  void mergeAllMainModesSetFilter() {
    // Add ALL MainModes as separate filters
    var filters = List.of(
      new AllowAllModesFilter(),
      // Add one extra main-mode filter. This should be removed, because it is included in ALL
      AllowTransitModeFilter.of(TransitMode.BUS, null)
    );

    var resultSet = AllowTransitModeFilter.merge(filters);

    assertEquals(1, resultSet.size());
    assertInstanceOf(AllowAllModesFilter.class, resultSet.iterator().next());
  }

  @Test
  void mergeAllMainModesSetExceptOne() {
    // Add all MainModes except AIRPLANE as separate filters
    var filters = AllowTransitModeFilter.ofMainModes(
      TransitMode.transitModesExceptAirplane().toArray(TransitMode[]::new)
    );

    var resultSet = AllowTransitModeFilter.merge(filters);
    assertEquals(1, resultSet.size());

    // The cast wil throw an exception is not the correct type is returned
    var filter = (AllowMainModesFilter) resultSet.iterator().next();

    for (TransitMode mode : TransitMode.values()) {
      assertEquals(mode != TransitMode.AIRPLANE, filter.allows(mode, SubMode.UNKNOWN));
    }
  }

  @Test
  void mergeOneSubModeFilter() {
    // Add ALL MainModes as separate filters
    var filters = new ArrayList<AllowTransitModeFilter>();

    var filter = AllowTransitModeFilter.of(TransitMode.BUS, LOCAL_BUS.name());
    filters.add(filter);

    var resultSet = AllowTransitModeFilter.merge(filters);
    assertEquals(1, resultSet.size());

    // Assert we still have the same filter
    assertSame(filter, resultSet.iterator().next());
  }

  @Test
  void mergeTwoSubModeFilter() {
    // Add ALL MainModes as separate filters
    var filters = new ArrayList<AllowTransitModeFilter>();

    var filter1 = AllowTransitModeFilter.of(TransitMode.BUS, LOCAL_BUS.name());
    filters.add(filter1);

    var filter2 = AllowTransitModeFilter.of(TransitMode.BUS, NIGHT_BUS.name());
    filters.add(filter2);

    var resultSet = List.copyOf(AllowTransitModeFilter.merge(filters));
    assertEquals(2, resultSet.size());

    var r1 = resultSet.get(0);
    var r2 = resultSet.get(1);

    if (r1 == filter2) {
      // Swap result
      var temp = r2;
      r2 = r1;
      r1 = temp;
    }
    assertSame(filter1, r1);
    assertSame(filter2, r2);
  }

  @Test
  void mergeThreeSubModeFilterIntoOneFilterWithBitSet() {
    // Add ALL MainModes as separate filters
    var filters = new ArrayList<AllowTransitModeFilter>(
      List.of(
        AllowTransitModeFilter.of(TransitMode.BUS, LOCAL_BUS.name()),
        AllowTransitModeFilter.of(TransitMode.BUS, EXPRESS_BUS.name()),
        AllowTransitModeFilter.of(TransitMode.BUS, NIGHT_BUS.name())
      )
    );

    var resultSet = AllowTransitModeFilter.merge(filters);
    assertEquals(1, resultSet.size());

    var result = (AllowMainAndSubModesFilter) resultSet.iterator().next();

    assertTrue(result.allows(TransitMode.BUS, LOCAL_BUS));
    assertFalse(result.allows(TransitMode.TRAM, LOCAL_BUS));
    assertFalse(result.allows(TransitMode.BUS, SubMode.UNKNOWN));
  }
}
