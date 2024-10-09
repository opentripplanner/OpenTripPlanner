package org.opentripplanner.apis.transmodel.mapping.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.apis.transmodel.mapping.preferences.BikePreferencesMapper.mapBikePreferences;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.apis.transmodel._support.TestDataFetcherDecorator;
import org.opentripplanner.routing.api.request.preference.BikePreferences;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;

class BikePreferencesMapperTest {

  static List<Arguments> mapBikePreferencesTestCases() {
    return List.of(
      Arguments.of(
        "walkReluctance",
        10.0,
        "BikePreferences{reluctance: 10.0, walking: VehicleWalkingPreferences{reluctance: 27.0}}"
      ),
      Arguments.of("bikeSpeed", 10.0, "BikePreferences{speed: 10.0}"),
      Arguments.of(
        "bicycleOptimisationMethod",
        VehicleRoutingOptimizeType.TRIANGLE,
        "BikePreferences{optimizeType: TRIANGLE}"
      ),
      // No effect unless BicycleOptimize is TRIANGLE
      Arguments.of("triangleFactors.time", 0.17, "BikePreferences{}"),
      Arguments.of("triangleFactors.slope", 0.12, "BikePreferences{}"),
      Arguments.of("triangleFactors.safety", 0.13, "BikePreferences{}")
    );
  }

  @ParameterizedTest
  @MethodSource("mapBikePreferencesTestCases")
  void testMapBikePreferences(String field, Object value, String expected) {
    var preferences = BikePreferences.of();
    mapBikePreferences(preferences, TestDataFetcherDecorator.of(field, value));
    assertEquals(expected, preferences.build().toString());
  }

  static List<Arguments> mapBikePreferencesOptimizeTriangleTestCases() {
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
  @MethodSource("mapBikePreferencesOptimizeTriangleTestCases")
  void testMapBikePreferencesOptimizeTriangle(String field, Object value, String expected) {
    var preferences = BikePreferences.of().withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE);
    mapBikePreferences(preferences, TestDataFetcherDecorator.of(field, value));
    assertEquals(
      "BikePreferences{optimizeType: TRIANGLE, optimizeTriangle: " + expected + "}",
      preferences.build().toString()
    );
  }
}
