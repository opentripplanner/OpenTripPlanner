package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.ViaLocationMapper.FIELD_LABEL;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.ViaLocationMapper.FIELD_MINIMUM_WAIT_TIME;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.ViaLocationMapper.FIELD_PASS_THROUGH;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.ViaLocationMapper.FIELD_STOP_LOCATION_IDS;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.ViaLocationMapper.FIELD_VISIT;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.ViaLocationMapper.mapToViaLocations;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ViaLocationMapperTest {

  public static final String LABEL = "TestLabel";
  public static final Duration MIN_WAIT_TIME = Duration.ofMinutes(5);
  public static final List<String> LIST_IDS_INPUT = List.of("F:ID1", "F:ID2");
  public static final String EXPECTED_IDS_AS_STRING = "[F:ID1, F:ID2]";

  @Test
  void mapToVisitViaLocations() {
    Map<String, Map<String, Object>> args = Map.of(
      FIELD_VISIT,
      Map.ofEntries(
        entry(FIELD_LABEL, LABEL),
        entry(FIELD_MINIMUM_WAIT_TIME, MIN_WAIT_TIME),
        entry(FIELD_STOP_LOCATION_IDS, LIST_IDS_INPUT)
      )
    );

    var inputs = List.of(args);
    var result = mapToViaLocations(inputs);

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
  void mapToVisitViaLocationsWithBareMinimum() {
    Map<String, Map<String, Object>> args = Map.of(
      FIELD_VISIT,
      Map.of(FIELD_STOP_LOCATION_IDS, List.of("F:1"))
    );
    var inputs = List.of(args);
    var result = mapToViaLocations(inputs);

    var via = result.getFirst();

    assertNull(via.label());
    assertEquals(Duration.ZERO, via.minimumWaitTime());
    assertEquals("[F:1]", via.stopLocationIds().toString());
    assertFalse(via.isPassThroughLocation());
  }

  @Test
  void mapToPassThrough() {
    final Map<String, Map<String, Object>> args = Map.of(
      FIELD_PASS_THROUGH,
      Map.ofEntries(entry(FIELD_LABEL, LABEL), entry(FIELD_STOP_LOCATION_IDS, LIST_IDS_INPUT))
    );
    var inputs = List.of(args);
    var result = mapToViaLocations(inputs);
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
  void mapToPassThroughWithBareMinimum() {
    Map<String, Map<String, Object>> args = Map.of(
      FIELD_PASS_THROUGH,
      Map.of(FIELD_STOP_LOCATION_IDS, List.of("F:1"))
    );
    var inputs = List.of(args);
    var result = mapToViaLocations(inputs);
    var via = result.getFirst();

    assertNull(via.label());
    assertEquals("[F:1]", via.stopLocationIds().toString());
    assertTrue(via.isPassThroughLocation());
  }
}
