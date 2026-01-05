package org.opentripplanner.apis.transmodel.mapping.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.apis.transmodel.mapping.preferences.BikePreferencesMapper.mapBikePreferences;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.transmodel._support.TestDataFetcherDecorator;
import org.opentripplanner.routing.api.request.preference.BikePreferences;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;

class BikePreferencesMapperTest {

  @Test
  void testBikeSpeed_WrapperTakesPrecedenceOverDeprecated() {
    var preferences = BikePreferences.of();
    var callWith = TestDataFetcherDecorator.of(
      Map.of(
        "bikeSpeed",
        10.0, // Deprecated - ignored because wrapper exists
        "bikePreferences",
        Map.of("speed", 7.0) // Wrapper wins
      )
    );
    mapBikePreferences(preferences, callWith);
    assertEquals(7.0, preferences.build().speed());
  }

  @Test
  void testBikeSpeed_NewNestedFieldWorks() {
    var preferences = BikePreferences.of();
    var callWith = TestDataFetcherDecorator.of("bikePreferences", Map.of("speed", 8.0));
    mapBikePreferences(preferences, callWith);
    assertEquals(8.0, preferences.build().speed());
  }

  @Test
  void testBikeSpeed_DefaultUsedWhenNeitherProvided() {
    var preferences = BikePreferences.of();
    var callWith = TestDataFetcherDecorator.of(Map.of());
    mapBikePreferences(preferences, callWith);
    assertEquals(BikePreferences.DEFAULT.speed(), preferences.build().speed());
  }

  @Test
  void testBikeReluctance_NewNestedFieldWorks() {
    var preferences = BikePreferences.of();
    var callWith = TestDataFetcherDecorator.of("bikePreferences", Map.of("reluctance", 3.5));
    mapBikePreferences(preferences, callWith);
    assertEquals(3.5, preferences.build().reluctance());
    assertEquals(9.5, preferences.build().walking().reluctance());
  }

  @Test
  void testBikeReluctance_DefaultUsedWhenNoneProvided() {
    var preferences = BikePreferences.of();
    var callWith = TestDataFetcherDecorator.of(Map.of());
    mapBikePreferences(preferences, callWith);
    assertEquals(BikePreferences.DEFAULT.reluctance(), preferences.build().reluctance());
  }

  @Test
  void testBikeOptimization_WrapperTakesPrecedenceOverDeprecated() {
    var preferences = BikePreferences.of();
    var callWith = TestDataFetcherDecorator.of(
      Map.of(
        "bicycleOptimisationMethod",
        VehicleRoutingOptimizeType.FLAT_STREETS, // Deprecated - ignored because wrapper exists
        "bikePreferences",
        Map.of("optimisationMethod", VehicleRoutingOptimizeType.TRIANGLE) // Wrapper wins
      )
    );
    mapBikePreferences(preferences, callWith);
    assertEquals(VehicleRoutingOptimizeType.TRIANGLE, preferences.build().optimizeType());
  }

  @Test
  void testBikeOptimization_NewNestedFieldWorks() {
    var preferences = BikePreferences.of();
    var callWith = TestDataFetcherDecorator.of(
      "bikePreferences",
      Map.of("optimisationMethod", VehicleRoutingOptimizeType.SAFEST_STREETS)
    );
    mapBikePreferences(preferences, callWith);
    assertEquals(VehicleRoutingOptimizeType.SAFEST_STREETS, preferences.build().optimizeType());
  }

  @Test
  void testBikeOptimization_DefaultUsedWhenNeitherProvided() {
    var preferences = BikePreferences.of();
    var callWith = TestDataFetcherDecorator.of(Map.of());
    mapBikePreferences(preferences, callWith);
    assertEquals(BikePreferences.DEFAULT.optimizeType(), preferences.build().optimizeType());
  }

  @Test
  void testTriangleFactors_WrapperTakesPrecedenceOverDeprecated() {
    var preferences = BikePreferences.of();
    preferences.withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE);
    var callWith = TestDataFetcherDecorator.of(
      Map.of(
        "triangleFactors",
        Map.of("time", 0.5, "slope", 0.3, "safety", 0.2), // Deprecated - ignored because wrapper exists
        "bikePreferences",
        Map.of("triangleFactors", Map.of("time", 0.3, "slope", 0.4, "safety", 0.3)) // Wrapper wins
      )
    );
    mapBikePreferences(preferences, callWith);
    var result = preferences.build();
    assertEquals(0.3, result.optimizeTriangle().time());
    assertEquals(0.4, result.optimizeTriangle().slope());
    assertEquals(0.3, result.optimizeTriangle().safety());
  }

  @Test
  void testTriangleFactors_NewNestedFieldWorks() {
    var preferences = BikePreferences.of();
    preferences.withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE);
    var callWith = TestDataFetcherDecorator.of(
      "bikePreferences",
      Map.of("triangleFactors", Map.of("time", 0.4, "slope", 0.3, "safety", 0.3))
    );
    mapBikePreferences(preferences, callWith);
    var result = preferences.build();
    assertEquals(0.4, result.optimizeTriangle().time());
    assertEquals(0.3, result.optimizeTriangle().slope());
    assertEquals(0.3, result.optimizeTriangle().safety());
  }

  @Test
  void testBike_AllNewFieldsTogether() {
    var preferences = BikePreferences.of();
    preferences.withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE);
    var callWith = TestDataFetcherDecorator.of(
      "bikePreferences",
      Map.of(
        "speed",
        8.5,
        "reluctance",
        3.0,
        "optimisationMethod",
        VehicleRoutingOptimizeType.TRIANGLE,
        "triangleFactors",
        Map.of("time", 0.5, "slope", 0.25, "safety", 0.25)
      )
    );
    mapBikePreferences(preferences, callWith);
    var result = preferences.build();
    assertEquals(8.5, result.speed());
    assertEquals(3.0, result.reluctance());
    assertEquals(VehicleRoutingOptimizeType.TRIANGLE, result.optimizeType());
    assertEquals(0.5, result.optimizeTriangle().time());
  }

  @Test
  void testBike_MixedDeprecatedAndNew() {
    var preferences = BikePreferences.of();
    var callWith = TestDataFetcherDecorator.of(
      Map.of(
        "bikeSpeed",
        10.0, // Deprecated - ignored because wrapper exists
        "bikePreferences",
        Map.of(
          "speed",
          7.0, // Wrapper wins
          "reluctance",
          3.5, // Wrapper wins
          "optimisationMethod",
          VehicleRoutingOptimizeType.TRIANGLE // Wrapper wins
        )
      )
    );
    mapBikePreferences(preferences, callWith);
    var result = preferences.build();
    assertEquals(7.0, result.speed()); // Wrapper wins
    assertEquals(3.5, result.reluctance()); // Wrapper wins
    assertEquals(VehicleRoutingOptimizeType.TRIANGLE, result.optimizeType()); // Wrapper wins
  }

  @Test
  void testBike_WrapperIgnoresAllDeprecatedFields() {
    var preferences = BikePreferences.of();
    preferences.withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE);
    var callWith = TestDataFetcherDecorator.of(
      Map.of(
        // All deprecated fields - should be ignored
        "bikeSpeed",
        11.0,
        "bicycleOptimisationMethod",
        VehicleRoutingOptimizeType.SAFEST_STREETS,
        "triangleFactors",
        Map.of("time", 0.6, "slope", 0.2, "safety", 0.1),
        // Wrapper takes precedence
        "bikePreferences",
        Map.of(
          "speed",
          7.0,
          "reluctance",
          3.0,
          "optimisationMethod",
          VehicleRoutingOptimizeType.TRIANGLE,
          "triangleFactors",
          Map.of("time", 0.3, "slope", 0.4, "safety", 0.2)
        )
      )
    );
    mapBikePreferences(preferences, callWith);
    var result = preferences.build();
    assertEquals(7.0, result.speed()); // Wrapper value
    assertEquals(VehicleRoutingOptimizeType.TRIANGLE, result.optimizeType()); // Wrapper value
    // Values may be slightly different due to rounding in Units class
    assertEquals(0.3, result.optimizeTriangle().time());
    assertEquals(0.4, result.optimizeTriangle().slope());
    assertEquals(0.2, result.optimizeTriangle().safety());
  }

  @Test
  void testBikeSpeed_DeprecatedFieldWorks() {
    var preferences = BikePreferences.of();
    var callWith = TestDataFetcherDecorator.of(Map.of("bikeSpeed", 9.0));
    mapBikePreferences(preferences, callWith);
    assertEquals(9.0, preferences.build().speed());
  }

  @Test
  void testBikeOptimization_DeprecatedFieldWorks() {
    var preferences = BikePreferences.of();
    var callWith = TestDataFetcherDecorator.of(
      Map.of("bicycleOptimisationMethod", VehicleRoutingOptimizeType.FLAT_STREETS)
    );
    mapBikePreferences(preferences, callWith);
    assertEquals(VehicleRoutingOptimizeType.FLAT_STREETS, preferences.build().optimizeType());
  }

  @Test
  void testTriangleFactors_DeprecatedTopLevelWorks() {
    var preferences = BikePreferences.of();
    preferences.withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE);
    var callWith = TestDataFetcherDecorator.of(
      Map.of("triangleFactors", Map.of("time", 0.6, "slope", 0.2, "safety", 0.2))
    );
    mapBikePreferences(preferences, callWith);
    var result = preferences.build();
    assertEquals(0.6, result.optimizeTriangle().time());
    assertEquals(0.2, result.optimizeTriangle().slope());
    assertEquals(0.2, result.optimizeTriangle().safety());
  }

  @Test
  void testBike_EmptyWrapperUsesDefaults() {
    var preferences = BikePreferences.of();
    var callWith = TestDataFetcherDecorator.of(Map.of("bikePreferences", Map.of()));
    mapBikePreferences(preferences, callWith);
    var result = preferences.build();
    assertEquals(BikePreferences.DEFAULT.speed(), result.speed());
    assertEquals(BikePreferences.DEFAULT.reluctance(), result.reluctance());
    assertEquals(BikePreferences.DEFAULT.optimizeType(), result.optimizeType());
  }
}
