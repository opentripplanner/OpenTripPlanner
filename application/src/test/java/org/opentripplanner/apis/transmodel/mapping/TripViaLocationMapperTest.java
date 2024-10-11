package org.opentripplanner.apis.transmodel.mapping;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.apis.transmodel.model.plan.ViaLocationInputType.FIELD_LABEL;
import static org.opentripplanner.apis.transmodel.model.plan.ViaLocationInputType.FIELD_MINIMUM_WAIT_TIME;
import static org.opentripplanner.apis.transmodel.model.plan.ViaLocationInputType.FIELD_PASS_THROUGH;
import static org.opentripplanner.apis.transmodel.model.plan.ViaLocationInputType.FIELD_STOP_LOCATION_IDS;
import static org.opentripplanner.apis.transmodel.model.plan.ViaLocationInputType.FIELD_VISIT;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TripViaLocationMapperTest {

  public static final String LABEL = "TestLabel";
  public static final Duration MIN_WAIT_TIME = Duration.ofMinutes(5);
  public static final List<String> LIST_IDS_INPUT = List.of("F:ID1", "F:ID2");
  public static final String EXPECTED_IDS_AS_STRING = "[F:ID1, F:ID2]";

  @BeforeEach
  void setup() {
    TransitIdMapper.clearFixedFeedId();
  }

  @Test
  void testMapToVisitViaLocations() {
    Map<String, Object> input = Map.ofEntries(
      entry(FIELD_VISIT, visitInput(LABEL, MIN_WAIT_TIME, LIST_IDS_INPUT))
    );
    var result = TripViaLocationMapper.mapToViaLocations(List.of(input));

    var via = result.getFirst();

    assertEquals(LABEL, via.label());
    assertEquals(MIN_WAIT_TIME, via.minimumWaitTime());
    assertEquals(EXPECTED_IDS_AS_STRING, via.stopLocationIds().toString());
    assertFalse(via.isPassThroughLocation());
    assertEquals(
      "[VisitViaLocation{label: TestLabel, minimumWaitTime: 5m, stopLocationIds: [F:ID1, F:ID2], coordinates: []}]",
      result.toString()
    );
  }

  @Test
  void testMapToVisitViaLocationsWithBareMinimum() {
    Map<String, Object> input = Map.of(
      FIELD_VISIT,
      Map.of(FIELD_STOP_LOCATION_IDS, List.of("F:1"))
    );
    var result = TripViaLocationMapper.mapToViaLocations(List.of(input));

    var via = result.getFirst();

    assertNull(via.label());
    assertEquals(Duration.ZERO, via.minimumWaitTime());
    assertEquals("[F:1]", via.stopLocationIds().toString());
    assertFalse(via.isPassThroughLocation());
  }

  @Test
  void tetMapToPassThrough() {
    Map<String, Object> input = Map.of(FIELD_PASS_THROUGH, passThroughInput(LABEL, LIST_IDS_INPUT));
    var result = TripViaLocationMapper.mapToViaLocations(List.of(input));
    var via = result.getFirst();

    assertEquals(LABEL, via.label());
    assertEquals(EXPECTED_IDS_AS_STRING, via.stopLocationIds().toString());
    assertTrue(via.isPassThroughLocation());
    assertEquals(
      "PassThroughViaLocation{label: TestLabel, stopLocationIds: [F:ID1, F:ID2]}",
      via.toString()
    );
  }

  @Test
  void tetMapToPassThroughWithBareMinimum() {
    Map<String, Object> input = Map.of(
      FIELD_PASS_THROUGH,
      Map.of(FIELD_STOP_LOCATION_IDS, List.of("F:1"))
    );
    var result = TripViaLocationMapper.mapToViaLocations(List.of(input));
    var via = result.getFirst();

    assertNull(via.label());
    assertEquals("[F:1]", via.stopLocationIds().toString());
    assertTrue(via.isPassThroughLocation());
  }

  @Test
  void testOneOf() {
    Map<String, Object> input = Map.ofEntries(
      entry(FIELD_VISIT, visitInput("A", Duration.ofMinutes(1), List.of("F:99"))),
      entry(FIELD_PASS_THROUGH, passThroughInput(LABEL, LIST_IDS_INPUT))
    );
    var ex = assertThrows(
      IllegalArgumentException.class,
      () -> TripViaLocationMapper.mapToViaLocations(List.of(input))
    );
    assertEquals(
      "Only one entry in 'via @oneOf' is allowed. Set: 'visit', 'passThrough'",
      ex.getMessage()
    );

    ex =
      assertThrows(
        IllegalArgumentException.class,
        () -> TripViaLocationMapper.mapToViaLocations(List.of(Map.of()))
      );
    assertEquals(
      "No entries in 'via @oneOf'. One of 'visit', 'passThrough' must be set.",
      ex.getMessage()
    );
  }

  private Map<String, Object> visitInput(String label, Duration minWaitTime, List<String> ids) {
    var map = new HashMap<String, Object>();
    if (label != null) {
      map.put(FIELD_LABEL, label);
    }
    if (minWaitTime != null) {
      map.put(FIELD_MINIMUM_WAIT_TIME, minWaitTime);
    }
    if (ids != null) {
      map.put(FIELD_STOP_LOCATION_IDS, ids);
    }
    return map;
  }

  private Map<String, Object> passThroughInput(String label, List<String> ids) {
    return visitInput(label, null, ids);
  }
}
