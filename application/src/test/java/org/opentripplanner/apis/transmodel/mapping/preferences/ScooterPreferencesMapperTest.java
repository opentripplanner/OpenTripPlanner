package org.opentripplanner.apis.transmodel.mapping.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.apis.transmodel.mapping.preferences.ScooterPreferencesMapper.mapScooterPreferences;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.transmodel._support.TestDataFetcherDecorator;
import org.opentripplanner.routing.api.request.preference.ScooterPreferences;
import org.opentripplanner.street.model.VehicleRoutingOptimizeType;

class ScooterPreferencesMapperTest {

  @Test
  void testScooterSpeed_NewNestedFieldWorks() {
    var preferences = ScooterPreferences.of();
    var callWith = TestDataFetcherDecorator.of("scooterPreferences", Map.of("speed", 9.0));
    mapScooterPreferences(preferences, callWith);
    assertEquals(9.0, preferences.build().speed());
  }

  @Test
  void testScooterSpeed_DefaultUsedWhenNotProvided() {
    var preferences = ScooterPreferences.of();
    var callWith = TestDataFetcherDecorator.of(Map.of());
    mapScooterPreferences(preferences, callWith);
    assertEquals(ScooterPreferences.DEFAULT.speed(), preferences.build().speed());
  }

  @Test
  void testScooterReluctance_NewNestedFieldWorks() {
    var preferences = ScooterPreferences.of();
    var callWith = TestDataFetcherDecorator.of("scooterPreferences", Map.of("reluctance", 4.5));
    mapScooterPreferences(preferences, callWith);
    assertEquals(4.5, preferences.build().reluctance());
  }

  @Test
  void testScooterReluctance_DefaultUsedWhenNotProvided() {
    var preferences = ScooterPreferences.of();
    var callWith = TestDataFetcherDecorator.of(Map.of());
    mapScooterPreferences(preferences, callWith);
    assertEquals(ScooterPreferences.DEFAULT.reluctance(), preferences.build().reluctance());
  }

  @Test
  void testScooterOptimization_NewNestedFieldWorks() {
    var preferences = ScooterPreferences.of();
    var callWith = TestDataFetcherDecorator.of(
      "scooterPreferences",
      Map.of("optimisationMethod", VehicleRoutingOptimizeType.SAFEST_STREETS)
    );
    mapScooterPreferences(preferences, callWith);
    assertEquals(VehicleRoutingOptimizeType.SAFEST_STREETS, preferences.build().optimizeType());
  }

  @Test
  void testScooterOptimization_DefaultUsedWhenNotProvided() {
    var preferences = ScooterPreferences.of();
    var callWith = TestDataFetcherDecorator.of(Map.of());
    mapScooterPreferences(preferences, callWith);
    assertEquals(ScooterPreferences.DEFAULT.optimizeType(), preferences.build().optimizeType());
  }

  @Test
  void testTriangleFactors_WrapperTakesPrecedenceOverDeprecated() {
    var preferences = ScooterPreferences.of();
    preferences.withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE);
    var callWith = TestDataFetcherDecorator.of(
      Map.of(
        "triangleFactors",
        // Deprecated - ignored because wrapper exists
        Map.of("time", 0.5, "slope", 0.3, "safety", 0.2),
        "scooterPreferences",
        Map.of(
          "triangleFactors",
          // Wrapper wins
          Map.of("time", 0.3, "slope", 0.4, "safety", 0.3)
        )
      )
    );
    mapScooterPreferences(preferences, callWith);
    var result = preferences.build();
    assertEquals(0.3, result.optimizeTriangle().time());
    assertEquals(0.4, result.optimizeTriangle().slope());
    assertEquals(0.3, result.optimizeTriangle().safety());
  }

  @Test
  void testTriangleFactors_NewNestedFieldWorks() {
    var preferences = ScooterPreferences.of();
    preferences.withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE);
    var callWith = TestDataFetcherDecorator.of(
      "scooterPreferences",
      Map.of("triangleFactors", Map.of("time", 0.4, "slope", 0.3, "safety", 0.3))
    );
    mapScooterPreferences(preferences, callWith);
    var result = preferences.build();
    assertEquals(0.4, result.optimizeTriangle().time());
    assertEquals(0.3, result.optimizeTriangle().slope());
    assertEquals(0.3, result.optimizeTriangle().safety());
  }

  @Test
  void testScooter_AllNewFieldsTogether() {
    var preferences = ScooterPreferences.of();
    preferences.withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE);
    var callWith = TestDataFetcherDecorator.of(
      "scooterPreferences",
      Map.of(
        "speed",
        10.5,
        "reluctance",
        4.0,
        "optimisationMethod",
        VehicleRoutingOptimizeType.TRIANGLE,
        "triangleFactors",
        Map.of("time", 0.5, "slope", 0.3, "safety", 0.2)
      )
    );
    mapScooterPreferences(preferences, callWith);
    var result = preferences.build();
    // Due to rounding in Units.speed
    assertEquals(11, result.speed());
    assertEquals(4.0, result.reluctance());
    assertEquals(VehicleRoutingOptimizeType.TRIANGLE, result.optimizeType());
    assertEquals(0.5, result.optimizeTriangle().time());
  }

  @Test
  void testScooter_PartialFieldsProvided() {
    var preferences = ScooterPreferences.of();
    var callWith = TestDataFetcherDecorator.of("scooterPreferences", Map.of("speed", 11.0));
    mapScooterPreferences(preferences, callWith);
    var result = preferences.build();
    // Provided
    assertEquals(11.0, result.speed());
    // Default
    assertEquals(ScooterPreferences.DEFAULT.reluctance(), result.reluctance());
    // Default
    assertEquals(ScooterPreferences.DEFAULT.optimizeType(), result.optimizeType());
  }

  @Test
  void testScooterSpeed_FallsBackToBikeSpeed() {
    var preferences = ScooterPreferences.of();
    var callWith = TestDataFetcherDecorator.of(Map.of("bikeSpeed", 8.0));
    mapScooterPreferences(preferences, callWith);
    assertEquals(8.0, preferences.build().speed());
  }

  @Test
  void testScooterOptimization_FallsBackToBicycleOptimisationMethod() {
    var preferences = ScooterPreferences.of();
    var callWith = TestDataFetcherDecorator.of(
      Map.of("bicycleOptimisationMethod", VehicleRoutingOptimizeType.FLAT_STREETS)
    );
    mapScooterPreferences(preferences, callWith);
    assertEquals(VehicleRoutingOptimizeType.FLAT_STREETS, preferences.build().optimizeType());
  }

  @Test
  void testTriangleFactors_FallsBackToTopLevel() {
    var preferences = ScooterPreferences.of();
    preferences.withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE);
    var callWith = TestDataFetcherDecorator.of(
      Map.of("triangleFactors", Map.of("time", 0.5, "slope", 0.3, "safety", 0.2))
    );
    mapScooterPreferences(preferences, callWith);
    var result = preferences.build();
    assertEquals(0.5, result.optimizeTriangle().time());
    assertEquals(0.3, result.optimizeTriangle().slope());
    assertEquals(0.2, result.optimizeTriangle().safety());
  }

  @Test
  void testScooterSpeed_WrapperTakesPrecedenceOverBikeSpeed() {
    var preferences = ScooterPreferences.of();
    var callWith = TestDataFetcherDecorator.of(
      Map.of("bikeSpeed", 10.0, "scooterPreferences", Map.of("speed", 6.0))
    );
    mapScooterPreferences(preferences, callWith);
    assertEquals(6.0, preferences.build().speed());
  }

  @Test
  void testScooterOptimization_WrapperTakesPrecedenceOverBicycleOptimisationMethod() {
    var preferences = ScooterPreferences.of();
    var callWith = TestDataFetcherDecorator.of(
      Map.of(
        "bicycleOptimisationMethod",
        VehicleRoutingOptimizeType.FLAT_STREETS,
        "scooterPreferences",
        Map.of("optimisationMethod", VehicleRoutingOptimizeType.TRIANGLE)
      )
    );
    mapScooterPreferences(preferences, callWith);
    assertEquals(VehicleRoutingOptimizeType.TRIANGLE, preferences.build().optimizeType());
  }

  @Test
  void testScooter_EmptyWrapperUsesDefaults() {
    var preferences = ScooterPreferences.of();
    var callWith = TestDataFetcherDecorator.of(Map.of("scooterPreferences", Map.of()));
    mapScooterPreferences(preferences, callWith);
    var result = preferences.build();
    assertEquals(ScooterPreferences.DEFAULT.speed(), result.speed());
    assertEquals(ScooterPreferences.DEFAULT.reluctance(), result.reluctance());
    assertEquals(ScooterPreferences.DEFAULT.optimizeType(), result.optimizeType());
  }
}
