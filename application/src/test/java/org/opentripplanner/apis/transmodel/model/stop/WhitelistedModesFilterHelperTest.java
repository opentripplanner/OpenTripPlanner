package org.opentripplanner.apis.transmodel.model.stop;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.apis.transmodel.model.EnumTypes.TRANSPORT_MODE;

import graphql.schema.GraphQLEnumValueDefinition;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.TransitMode;

class WhitelistedModesFilterHelperTest {

  @Test
  void getWhitelistedModes_filtersOutUnknown() {
    // When clients pass whiteListedModes including "unknown", the TRANSPORT_MODE enum maps
    // "unknown" to a String, not a TransitMode.
    var raw = TRANSPORT_MODE.getValues()
      .stream()
      .map(GraphQLEnumValueDefinition::getValue)
      .toList();

    // Verify the "unknown" does indeed resolve to a String and is contained in the raw values
    assertTrue(raw.contains("unknown"));

    var resolved = EstimatedCallHelper.getWhitelistedModes(raw);

    assertNotNull(resolved);
    // All remaining should be TransitMode enum values
    for (var mode : resolved) {
      assertEquals(TransitMode.class, mode.getClass());
    }
    // Only one single "unknown" value dropped
    assertEquals(raw.size() - 1, resolved.size());
  }

  @Test
  void getWhitelistedModes_withOnlyNull_returnsEmptyList() {
    var raw = new ArrayList<>();
    raw.add("unknown");
    raw.add(null);

    // Client passed a non-empty list, but all entries are invalid.
    // Returns empty list (= match nothing), not null (= no filter).
    var resolved = EstimatedCallHelper.getWhitelistedModes(raw);
    assertNotNull(resolved);
    assertEquals(0, resolved.size());
  }

  @Test
  void getWhitelistedModes_withNull_returnsNull() {
    assertNull(EstimatedCallHelper.getWhitelistedModes(null));
  }

  @Test
  void getWhitelistedModes_withEmptyList_returnsNull() {
    // Empty list should mean "no filter" (match everything), same as baseline behavior
    assertNull(EstimatedCallHelper.getWhitelistedModes(List.of()));
  }

  @Test
  void getWhitelistedModes_keepsValidModes() {
    var modes = List.of(TransitMode.BUS, TransitMode.RAIL);
    var resolved = EstimatedCallHelper.getWhitelistedModes(modes);
    assertNotNull(resolved);
    assertEquals(modes, List.copyOf(resolved));
  }
}
