package org.opentripplanner.apis.transmodel.mapping;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.apis.transmodel.model.plan.ViaLocationInputType.FIELD_COORDINATE;
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
import org.opentripplanner.apis.transmodel.model.framework.CoordinateInputType;
import org.opentripplanner.framework.geometry.WgsCoordinate;

class TripViaLocationMapperTest {

  private static final Duration D1m = Duration.ofMinutes(1);
  private static final String LABEL = "TestLabel";
  private static final Duration MIN_WAIT_TIME = Duration.ofMinutes(5);
  private static final List<String> LIST_IDS_INPUT = List.of("F:ID1", "F:ID2");
  private static final String EXPECTED_IDS_AS_STRING = "[F:ID1, F:ID2]";
  private static final String REASON_EMPTY_IDS_ALLOWED_PASS_THROUGH =
    """
    Unfortunately the 'placeIds' is not required. Making it required would be a breaking change,
    so wee just ignore it."
    """;

  @BeforeEach
  void setup() {
    TransitIdMapper.clearFixedFeedId();
  }

  @Test
  void testMapToVisitViaLocations() {
    Map<String, Object> input = Map.ofEntries(
      entry(FIELD_VISIT, visitInput(LABEL, MIN_WAIT_TIME, LIST_IDS_INPUT, null))
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
    Map<String, Object> input = mapOf(FIELD_VISIT, mapOf(FIELD_STOP_LOCATION_IDS, List.of("F:1")));
    var result = TripViaLocationMapper.mapToViaLocations(List.of(input));

    var via = result.getFirst();

    assertNull(via.label());
    assertEquals(Duration.ZERO, via.minimumWaitTime());
    assertEquals("[F:1]", via.stopLocationIds().toString());
    assertFalse(via.isPassThroughLocation());
  }

  @Test
  void testMapToVisitViaLocationsWithoutIdsOrCoordinates() {
    Map<String, Object> input = mapOf(FIELD_VISIT, mapOf(FIELD_STOP_LOCATION_IDS, null));
    var ex = assertThrows(IllegalArgumentException.class, () ->
      TripViaLocationMapper.mapToViaLocations(List.of(input))
    );
    assertEquals(
      "A via location must have at least one stop location or a coordinate.",
      ex.getMessage()
    );
  }

  @Test
  void testMapToVisitViaLocationsWithAnEmptyListOfIds() {
    Map<String, Object> input = mapOf(FIELD_VISIT, mapOf(FIELD_STOP_LOCATION_IDS, List.of()));
    var ex = assertThrows(IllegalArgumentException.class, () ->
      TripViaLocationMapper.mapToViaLocations(List.of(input))
    );
    assertEquals(
      "A via location must have at least one stop location or a coordinate.",
      ex.getMessage()
    );
  }

  @Test
  void tetMapToPassThrough() {
    Map<String, Object> input = mapOf(FIELD_PASS_THROUGH, passThroughInput(LABEL, LIST_IDS_INPUT));
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
    Map<String, Object> input = mapOf(
      FIELD_PASS_THROUGH,
      mapOf(FIELD_STOP_LOCATION_IDS, List.of("F:1"))
    );
    var result = TripViaLocationMapper.mapToViaLocations(List.of(input));
    var via = result.getFirst();

    assertNull(via.label());
    assertEquals("[F:1]", via.stopLocationIds().toString());
    assertTrue(via.isPassThroughLocation());
  }

  @Test
  void testMapToPassThroughWithAnEmptyListOfIds() {
    Map<String, Object> input = mapOf(
      FIELD_PASS_THROUGH,
      mapOf(FIELD_STOP_LOCATION_IDS, List.of())
    );
    var ex = assertThrows(IllegalArgumentException.class, () ->
      TripViaLocationMapper.mapToViaLocations(List.of(input))
    );
    assertEquals(
      "A pass-through via-location must have at least one stop location.",
      ex.getMessage()
    );
  }

  @Test
  void testOneOf() {
    Map<String, Object> input = Map.ofEntries(
      entry(FIELD_VISIT, visitInput("A", D1m, List.of("F:99"), null)),
      entry(FIELD_PASS_THROUGH, passThroughInput(LABEL, LIST_IDS_INPUT))
    );
    var ex = assertThrows(IllegalArgumentException.class, () ->
      TripViaLocationMapper.mapToViaLocations(List.of(input))
    );
    assertEquals(
      "Only one entry in 'via @oneOf' is allowed. Set: 'visit', 'passThrough'",
      ex.getMessage()
    );

    ex = assertThrows(IllegalArgumentException.class, () ->
      TripViaLocationMapper.mapToViaLocations(List.of(Map.of()))
    );
    assertEquals(
      "No entries in 'via @oneOf'. One of 'visit', 'passThrough' must be set.",
      ex.getMessage()
    );
  }

  @Test
  void testToLegacyPassThroughLocations() {
    Map<String, Object> input = Map.of("name", LABEL, "placeIds", LIST_IDS_INPUT);
    var result = TripViaLocationMapper.toLegacyPassThroughLocations(List.of(input));
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
  void testToLegacyPassThroughLocationsWithBareMinimum() {
    Map<String, Object> input = mapOf("placeIds", LIST_IDS_INPUT);
    var result = TripViaLocationMapper.toLegacyPassThroughLocations(List.of(input));
    var via = result.getFirst();

    assertNull(via.label());
    assertEquals(EXPECTED_IDS_AS_STRING, via.stopLocationIds().toString());
    assertTrue(via.isPassThroughLocation());
    assertEquals("PassThroughViaLocation{stopLocationIds: [F:ID1, F:ID2]}", via.toString());
  }

  @Test
  void testToLegacyPassThroughLocationsWithoutIds() {
    var result = TripViaLocationMapper.toLegacyPassThroughLocations(
      List.of(mapOf("placeIds", null))
    );
    assertTrue(result.isEmpty(), REASON_EMPTY_IDS_ALLOWED_PASS_THROUGH);
  }

  @Test
  void testToLegacyPassThroughLocationsWithEmptyList() {
    Map<String, Object> input = Map.ofEntries(entry("name", LABEL), entry("placeIds", List.of()));
    var result = TripViaLocationMapper.toLegacyPassThroughLocations(List.of(input));
    assertTrue(result.isEmpty(), REASON_EMPTY_IDS_ALLOWED_PASS_THROUGH);
  }

  private Map<String, Object> visitInput(
    String label,
    Duration minWaitTime,
    List<String> ids,
    WgsCoordinate coordinate
  ) {
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
    if (coordinate != null) {
      map.put(FIELD_COORDINATE, CoordinateInputType.mapForTest(coordinate));
    }
    return map;
  }

  private Map<String, Object> passThroughInput(String label, List<String> ids) {
    return visitInput(label, null, ids, null);
  }

  /**
   * Create a new HashMap with the {@code key} and {@code value}, the value may be {@code null}.
   * The {@link Map#of(Object, Object)} does not support {@code null} values.
   */
  private static Map<String, Object> mapOf(String key, Object value) {
    var map = new HashMap<String, Object>();
    map.put(key, value);
    return map;
  }
}
