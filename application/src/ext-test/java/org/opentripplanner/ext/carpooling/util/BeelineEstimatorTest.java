package org.opentripplanner.ext.carpooling.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_EAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTHEAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTHWEST;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.geometry.WgsCoordinate;

class BeelineEstimatorTest {

  private BeelineEstimator estimator;

  @BeforeEach
  void setup() {
    estimator = new BeelineEstimator();
  }

  @Test
  void estimateDuration_shortDistance_returnsReasonableDuration() {
    // Oslo Center to Oslo East (~2.5km beeline)
    Duration duration = estimator.estimateDuration(OSLO_CENTER, OSLO_EAST);

    // With default parameters (1.3 detour factor, 10 m/s speed):
    // Expected: ~2500m * 1.3 / 10 = ~325 seconds = ~5.4 minutes
    // > 4 minutes
    assertTrue(duration.getSeconds() > 240);
    // < 8 minutes
    assertTrue(duration.getSeconds() < 480);
  }

  @Test
  void estimateDuration_mediumDistance_returnsReasonableDuration() {
    // Oslo Center to Oslo North (~3.3km beeline)
    Duration duration = estimator.estimateDuration(OSLO_CENTER, OSLO_NORTH);

    // Expected: ~3300m * 1.3 / 10 = ~429 seconds = ~7.2 minutes
    // > 5 minutes
    assertTrue(duration.getSeconds() > 300);
    // < 10 minutes
    assertTrue(duration.getSeconds() < 600);
  }

  @Test
  void estimateDuration_sameLocation_returnsZero() {
    Duration duration = estimator.estimateDuration(OSLO_CENTER, OSLO_CENTER);
    assertEquals(Duration.ZERO, duration);
  }

  @Test
  void estimateDuration_veryShortDistance_roundsDownToZero() {
    // Two points very close together (~10 meters)
    var point1 = new WgsCoordinate(59.9139, 10.7522);
    var point2 = new WgsCoordinate(59.9140, 10.7522);

    Duration duration = estimator.estimateDuration(point1, point2);

    // ~10m * 1.3 / 10 = ~1.3 seconds, rounds to 1
    assertTrue(duration.getSeconds() <= 5);
  }

  @Test
  void calculateCumulativeTimes_simpleRoute_calculatesCorrectly() {
    // Route: Oslo Center → Oslo East → Oslo North
    List<WgsCoordinate> points = List.of(OSLO_CENTER, OSLO_EAST, OSLO_NORTH);

    Duration[] times = estimator.calculateCumulativeTimes(points);

    assertEquals(3, times.length);
    // Start at 0
    assertEquals(Duration.ZERO, times[0]);

    // Each segment should add positive duration
    assertTrue(times[1].compareTo(Duration.ZERO) > 0);
    assertTrue(times[2].compareTo(times[1]) > 0);

    // Total duration should be reasonable (sum of two ~3-5km segments)
    // > 10 minutes
    assertTrue(times[2].getSeconds() > 600);
    // < 30 minutes
    assertTrue(times[2].getSeconds() < 1800);
  }

  @Test
  void calculateCumulativeTimes_singlePoint_returnsZero() {
    List<WgsCoordinate> points = List.of(OSLO_CENTER);

    Duration[] times = estimator.calculateCumulativeTimes(points);

    assertEquals(1, times.length);
    assertEquals(Duration.ZERO, times[0]);
  }

  @Test
  void calculateCumulativeTimes_emptyList_returnsEmptyArray() {
    List<WgsCoordinate> points = List.of();

    Duration[] times = estimator.calculateCumulativeTimes(points);

    assertEquals(0, times.length);
  }

  @Test
  void calculateCumulativeTimes_multipleStops_timesAreMonotonic() {
    // Route with multiple stops
    List<WgsCoordinate> points = List.of(
      OSLO_CENTER,
      OSLO_EAST,
      OSLO_NORTHEAST,
      OSLO_NORTH,
      OSLO_NORTHWEST
    );

    Duration[] times = estimator.calculateCumulativeTimes(points);

    // Times should be strictly increasing
    for (int i = 1; i < times.length; i++) {
      assertTrue(
        times[i].compareTo(times[i - 1]) > 0,
        "Time at position " + i + " should be greater than time at position " + (i - 1)
      );
    }
  }

  @Test
  void customDetourFactor_increasedFactor_increasesEstimate() {
    var defaultEstimator = new BeelineEstimator(1.3, 10.0);
    var higherDetourEstimator = new BeelineEstimator(1.5, 10.0);

    Duration defaultDuration = defaultEstimator.estimateDuration(OSLO_CENTER, OSLO_NORTH);
    Duration higherDetourDuration = higherDetourEstimator.estimateDuration(OSLO_CENTER, OSLO_NORTH);

    // Higher detour factor should give longer duration
    assertTrue(higherDetourDuration.compareTo(defaultDuration) > 0);
  }

  @Test
  void customSpeed_lowerSpeed_increasesEstimate() {
    var defaultEstimator = new BeelineEstimator(1.3, 10.0);
    var slowerEstimator = new BeelineEstimator(1.3, 5.0);

    Duration defaultDuration = defaultEstimator.estimateDuration(OSLO_CENTER, OSLO_NORTH);
    Duration slowerDuration = slowerEstimator.estimateDuration(OSLO_CENTER, OSLO_NORTH);

    // Lower speed should give longer duration
    assertTrue(slowerDuration.compareTo(defaultDuration) > 0);
    // Should be approximately double
    assertTrue(slowerDuration.getSeconds() > defaultDuration.getSeconds() * 1.8);
    assertTrue(slowerDuration.getSeconds() < defaultDuration.getSeconds() * 2.2);
  }

  @Test
  void customParameters_applyCorrectly() {
    // Custom: 2x detour, 20 m/s speed
    var customEstimator = new BeelineEstimator(2.0, 20.0);

    // Calculate expected duration manually
    double beelineDistance = SphericalDistanceLibrary.fastDistance(
      OSLO_CENTER.asJtsCoordinate(),
      OSLO_NORTH.asJtsCoordinate()
    );
    double expectedSeconds = (beelineDistance * 2.0) / 20.0;
    Duration expectedDuration = Duration.ofSeconds((long) expectedSeconds);

    Duration actualDuration = customEstimator.estimateDuration(OSLO_CENTER, OSLO_NORTH);

    // Should be very close (within 1 second due to rounding)
    long diff = Math.abs(actualDuration.getSeconds() - expectedDuration.getSeconds());
    assertTrue(diff <= 1);
  }

  @Test
  void getDetourFactor_returnsConfiguredValue() {
    assertEquals(1.3, estimator.getDetourFactor());

    var customEstimator = new BeelineEstimator(1.5, 10.0);
    assertEquals(1.5, customEstimator.getDetourFactor());
  }

  @Test
  void getSpeedMps_returnsConfiguredValue() {
    assertEquals(10.0, estimator.getSpeed());

    var customEstimator = new BeelineEstimator(1.3, 15.0);
    assertEquals(15.0, customEstimator.getSpeed());
  }

  @Test
  void defaultDetourFactor_is1Point3() {
    assertEquals(1.3, BeelineEstimator.DEFAULT_DETOUR_FACTOR);
  }

  @Test
  void defaultSpeed_is10MetersPerSecond() {
    assertEquals(10.0, BeelineEstimator.DEFAULT_SPEED_MPS);
  }

  @Test
  void constructor_detourFactorLessThanOne_throwsException() {
    assertThrows(
      IllegalArgumentException.class,
      () -> new BeelineEstimator(0.9, 10.0),
      "detourFactor must be >= 1.0"
    );
  }

  @Test
  void constructor_detourFactorExactlyOne_accepts() {
    // Minimum valid detour factor (no detour)
    var estimator = new BeelineEstimator(1.0, 10.0);
    assertEquals(1.0, estimator.getDetourFactor());
  }

  @Test
  void constructor_zeroSpeed_throwsException() {
    assertThrows(
      IllegalArgumentException.class,
      () -> new BeelineEstimator(1.3, 0.0),
      "speedMps must be positive"
    );
  }

  @Test
  void constructor_negativeSpeed_throwsException() {
    assertThrows(
      IllegalArgumentException.class,
      () -> new BeelineEstimator(1.3, -5.0),
      "speedMps must be positive"
    );
  }

  @Test
  void estimateDuration_longDistance_scalesCorrectly() {
    // Oslo to Bergen (~300km beeline)
    var bergen = new WgsCoordinate(60.39, 5.32);

    Duration duration = estimator.estimateDuration(OSLO_CENTER, bergen);

    // Expected: ~300,000m * 1.3 / 10 = 39,000 seconds = 650 minutes
    // This is just a sanity check - beeline is not accurate for such long distances
    // > 8.3 hours
    assertTrue(duration.getSeconds() > 30000);
    // < 16.7 hours
    assertTrue(duration.getSeconds() < 60000);
  }

  @Test
  void calculateCumulativeTimes_twoPoints_calculatesCorrectly() {
    List<WgsCoordinate> points = List.of(OSLO_CENTER, OSLO_NORTH);

    Duration[] times = estimator.calculateCumulativeTimes(points);

    assertEquals(2, times.length);
    assertEquals(Duration.ZERO, times[0]);
    assertTrue(times[1].compareTo(Duration.ZERO) > 0);
  }

  @Test
  void estimateDuration_optimisticEstimate_lessThanActualStreetRoute() {
    // Beeline estimates should be optimistic (underestimate actual travel time)
    // This is important for the heuristic to work correctly

    // For urban areas, actual street routes are typically 1.3-1.5x beeline
    // Our default detour factor of 1.3 is intentionally optimistic
    Duration beelineEstimate = estimator.estimateDuration(OSLO_CENTER, OSLO_NORTH);

    // Typical actual street route would be ~1.5x beeline at 10 m/s
    double actualBeelineDistance = SphericalDistanceLibrary.fastDistance(
      OSLO_CENTER.asJtsCoordinate(),
      OSLO_NORTH.asJtsCoordinate()
    );
    Duration conservativeActualTime = Duration.ofSeconds(
      (long) ((actualBeelineDistance * 1.5) / 10.0)
    );

    // Our estimate should be less than or equal to a conservative actual time
    assertTrue(beelineEstimate.compareTo(conservativeActualTime) <= 0);
  }
}
