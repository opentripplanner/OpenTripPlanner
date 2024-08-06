package org.opentripplanner.apis.transmodel.mapping.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.apis.transmodel.mapping.preferences.ScooterPreferencesMapper.mapScooterPreferences;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.apis.transmodel._support.TestDataFetcherDecorator;
import org.opentripplanner.routing.api.request.preference.ScooterPreferences;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;

class ScooterPreferencesMapperTest {

  static List<Arguments> mapScooterPreferencesTestCases() {
    return List.of(
      Arguments.of("walkReluctance", 10.0, "ScooterPreferences{reluctance: 10.0}"),
      Arguments.of("bikeSpeed", 10.0, "ScooterPreferences{speed: 10.0}"),
      Arguments.of(
        "bicycleOptimisationMethod",
        VehicleRoutingOptimizeType.TRIANGLE,
        "ScooterPreferences{optimizeType: TRIANGLE}"
      ),
      // No effect unless BicycleOptimize is TRIANGLE
      Arguments.of("triangleFactors.time", 0.17, "ScooterPreferences{}"),
      Arguments.of("triangleFactors.slope", 0.12, "ScooterPreferences{}"),
      Arguments.of("triangleFactors.safety", 0.13, "ScooterPreferences{}")
    );
  }

  @ParameterizedTest
  @MethodSource("mapScooterPreferencesTestCases")
  void testMapScooterPreferences(String field, Object value, String expected) {
    var preferences = ScooterPreferences.of();
    mapScooterPreferences(preferences, TestDataFetcherDecorator.of(field, value));
    assertEquals(expected, preferences.build().toString());
  }

  static List<Arguments> mapScooterPreferencesOptimizeTriangleTestCases() {
    return List.of(
      Arguments.of(
        "triangleFactors.time",
        0.17,
        "TimeSlopeSafetyTriangle[time=1.0, slope=0.0, safety=0.0]"
      ),
      Arguments.of(
        "triangleFactors.slope",
        0.12,
        "TimeSlopeSafetyTriangle[time=0.0, slope=1.0, safety=0.0]"
      ),
      Arguments.of(
        "triangleFactors.safety",
        0.13,
        "TimeSlopeSafetyTriangle[time=0.0, slope=0.0, safety=1.0]"
      )
    );
  }

  @ParameterizedTest
  @MethodSource("mapScooterPreferencesOptimizeTriangleTestCases")
  void testMapScooterPreferencesOptimizeTriangle(String field, Object value, String expected) {
    var preferences = ScooterPreferences.of().withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE);
    mapScooterPreferences(preferences, TestDataFetcherDecorator.of(field, value));
    assertEquals(
      "ScooterPreferences{optimizeType: TRIANGLE, optimizeTriangle: " + expected + "}",
      preferences.build().toString()
    );
  }
}
