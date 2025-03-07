package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.apis.gtfs.SchemaObjectMappersForTests.mapCoordinate;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.ViaLocationMapper.mapToViaLocations;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ViaLocationMapperTest {

  private static final String FIELD_STOP_LOCATION_IDS = "stopLocationIds";
  private static final String FIELD_LABEL = "label";
  private static final String FIELD_MINIMUM_WAIT_TIME = "minimumWaitTime";
  private static final String FIELD_VISIT = "visit";
  private static final String FIELD_PASS_THROUGH = "passThrough";
  private static final String FIELD_COORDINATE = "coordinate";
  private static final String LABEL_FIRST = "TestLabel1";
  private static final String LABEL_THIRD = "TestLabel3";
  private static final Duration MIN_WAIT_TIME_FIRST = Duration.ofMinutes(5);
  private static final Duration MIN_WAIT_TIME_THIRD = Duration.ofMinutes(10);
  private static final List<String> LIST_IDS_INPUT_FIRST = List.of("F:ID1", "F:ID2");
  private static final List<String> LIST_IDS_INPUT_THIRD = List.of("F:ID3", "F:ID4");
  private static final double SECOND_LAT = 30.5;
  private static final double SECOND_LON = 40.2;
  private static final double THIRD_LAT = 35.5;
  private static final double THIRD_LON = 45.5;
  private static final Map<String, Double> COORDINATE_INPUT_SECOND = mapCoordinate(
    SECOND_LAT,
    SECOND_LON
  );
  private static final Map<String, Double> COORDINATE_INPUT_THIRD = mapCoordinate(
    THIRD_LAT,
    THIRD_LON
  );
  private static final String EXPECTED_IDS_AS_STRING_FIRST = "[F:ID1, F:ID2]";
  private static final String EXPECTED_IDS_AS_STRING_THIRD = "[F:ID3, F:ID4]";

  @Test
  void mapToVisitViaLocations() {
    List<Map<String, Object>> args = List.of(
      Map.of(
        FIELD_VISIT,
        Map.ofEntries(
          entry(FIELD_LABEL, LABEL_FIRST),
          entry(FIELD_MINIMUM_WAIT_TIME, MIN_WAIT_TIME_FIRST),
          entry(FIELD_STOP_LOCATION_IDS, LIST_IDS_INPUT_FIRST)
        )
      ),
      Map.of(FIELD_VISIT, Map.ofEntries(entry(FIELD_COORDINATE, COORDINATE_INPUT_SECOND))),
      Map.of(
        FIELD_VISIT,
        Map.ofEntries(
          entry(FIELD_LABEL, LABEL_THIRD),
          entry(FIELD_MINIMUM_WAIT_TIME, MIN_WAIT_TIME_THIRD),
          entry(FIELD_STOP_LOCATION_IDS, LIST_IDS_INPUT_THIRD),
          entry(FIELD_COORDINATE, COORDINATE_INPUT_THIRD)
        )
      )
    );

    var result = mapToViaLocations(args);

    var firstVia = result.getFirst();

    assertEquals(LABEL_FIRST, firstVia.label());
    assertEquals(MIN_WAIT_TIME_FIRST, firstVia.minimumWaitTime());
    assertEquals(EXPECTED_IDS_AS_STRING_FIRST, firstVia.stopLocationIds().toString());
    assertFalse(firstVia.isPassThroughLocation());

    var secondVia = result.get(1);

    assertThat(secondVia.coordinates()).hasSize(1);
    assertEquals(SECOND_LAT, secondVia.coordinates().get(0).latitude());
    assertEquals(SECOND_LON, secondVia.coordinates().get(0).longitude());
    assertFalse(secondVia.isPassThroughLocation());

    var thirdVia = result.get(2);

    assertEquals(LABEL_THIRD, thirdVia.label());
    assertEquals(MIN_WAIT_TIME_THIRD, thirdVia.minimumWaitTime());
    assertEquals(EXPECTED_IDS_AS_STRING_THIRD, thirdVia.stopLocationIds().toString());
    assertThat(thirdVia.coordinates()).hasSize(1);
    assertEquals(THIRD_LAT, thirdVia.coordinates().get(0).latitude());
    assertEquals(THIRD_LON, thirdVia.coordinates().get(0).longitude());
    assertFalse(thirdVia.isPassThroughLocation());

    assertEquals(
      "[" +
      "VisitViaLocation{label: TestLabel1, minimumWaitTime: 5m, stopLocationIds: [F:ID1, F:ID2], coordinates: []}, " +
      "VisitViaLocation{coordinates: [(30.5, 40.2)]}, " +
      "VisitViaLocation{label: TestLabel3, minimumWaitTime: 10m, stopLocationIds: [F:ID3, F:ID4], coordinates: [(35.5, 45.5)]}" +
      "]",
      result.toString()
    );
  }

  @Test
  void mapToVisitViaLocationsWithBareMinimum() {
    Map<String, Object> args = Map.of(FIELD_VISIT, Map.of(FIELD_STOP_LOCATION_IDS, List.of("F:1")));
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
    final Map<String, Object> args = Map.of(
      FIELD_PASS_THROUGH,
      Map.ofEntries(
        entry(FIELD_LABEL, LABEL_FIRST),
        entry(FIELD_STOP_LOCATION_IDS, LIST_IDS_INPUT_FIRST)
      )
    );
    var inputs = List.of(args);
    var result = mapToViaLocations(inputs);
    var via = result.getFirst();

    assertEquals(LABEL_FIRST, via.label());
    assertEquals(EXPECTED_IDS_AS_STRING_FIRST, via.stopLocationIds().toString());
    assertTrue(via.isPassThroughLocation());
    assertEquals(
      "PassThroughViaLocation{label: TestLabel1, stopLocationIds: [F:ID1, F:ID2]}",
      via.toString()
    );
  }

  @Test
  void mapToPassThroughWithBareMinimum() {
    Map<String, Object> args = Map.of(
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
