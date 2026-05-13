package org.opentripplanner.street.model.elevation;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.street.model.elevation.ElevationProfiles.STEEP_DOWNHILL_PROFILE;
import static org.opentripplanner.street.model.elevation.ElevationProfiles.STEEP_ELEVATION_PROFILE;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory;

class ElevationUtilsTest {

  @Test
  void testLengthMultiplier() {
    PackedCoordinateSequenceFactory factory = PackedCoordinateSequenceFactory.DOUBLE_FACTORY;
    CoordinateSequence seq = factory.create(
      new Coordinate[] { new Coordinate(0, 1), new Coordinate(10, 1) }
    );
    SlopeCosts costs = ElevationUtils.getSlopeCosts(seq);
    assertEquals(1.0, costs.lengthMultiplier);

    seq = factory.create(new Coordinate[] { new Coordinate(0, 1), new Coordinate(10, 2) });
    costs = ElevationUtils.getSlopeCosts(seq);
    assertEquals(1.00498756211208902702, costs.lengthMultiplier);

    seq = factory.create(
      new Coordinate[] { new Coordinate(0, 1), new Coordinate(10, 2), new Coordinate(15, 1) }
    );
    costs = ElevationUtils.getSlopeCosts(seq);
    assertEquals(1.00992634231424500668, costs.lengthMultiplier);
  }

  @Test
  void testCalculateSlopeWalkEffectiveLengthFactor() {
    // 35% should hit the MAX_SLOPE_WALK_EFFECTIVE_LENGTH_FACTOR=3, hence 300m is expected
    assertEquals(300.0, ElevationUtils.calculateEffectiveWalkLength(100, 35), 0.1);

    // 10% incline equals 1.42 penalty
    assertEquals(141.9, ElevationUtils.calculateEffectiveWalkLength(100, 10), 0.1);

    // Flat is flat, no penalty
    assertEquals(120.0, ElevationUtils.calculateEffectiveWalkLength(120, 0));

    // 5% downhill is the fastest to walk and effective distance only 0.83 * flat distance
    assertEquals(83.9, ElevationUtils.calculateEffectiveWalkLength(100, -5), 0.1);

    // 10% downhill is about the same as flat
    assertEquals(150.0, ElevationUtils.calculateEffectiveWalkLength(150, -15));

    // 15% downhill have a penalty of 1.19
    assertEquals(238.2, ElevationUtils.calculateEffectiveWalkLength(200, -30), 0.1);

    // 45% downhill hit the MAX_SLOPE_WALK_EFFECTIVE_LENGTH_FACTOR=3 again
    assertEquals(300.0, ElevationUtils.calculateEffectiveWalkLength(100, -45), 0.1);
  }

  @Test
  void testPartialElevationProfile() {
    double[] two_point = new double[] { 0, 10, 10, 20 };
    double[] four_point = new double[] { 0, 100, 10, 110, 20, 120, 25, 125 };
    double[] small_run = new double[] { 0, 100, 10, 110, 20, 120, 20.5, 120.5 };

    // Full elevation is returned
    assertPartialElevation(two_point, 0, 10, two_point);
    assertPartialElevation(two_point, -10, 20, two_point);
    assertPartialElevation(four_point, 0, 25, four_point);
    assertPartialElevation(four_point, -10, 30, four_point);

    // null is returned for single-point sections
    assertPartialElevation(two_point, 0, 0, null);
    assertPartialElevation(two_point, 10, 10, null);

    // partial sections are returned (from 1 segment)
    assertPartialElevation(two_point, 0, 0.5, new double[] { 0, 10, 0.5, 10.5 });
    assertPartialElevation(two_point, 9.5, 10, new double[] { 0, 19.5, 0.5, 20 });
    assertPartialElevation(two_point, 4, 8, new double[] { 0, 14, 4, 18 });

    // partial sections are returned (along segments)
    assertPartialElevation(four_point, 0, 20, new double[] { 0, 100, 10, 110, 20, 120 });
    assertPartialElevation(four_point, 10, 20, new double[] { 0, 110, 10, 120 });
    assertPartialElevation(four_point, 20, 25, new double[] { 0, 120, 5, 125 });

    // partial sections are returned (between segments)
    assertPartialElevation(four_point, 5, 20, new double[] { 0, 105, 5, 110, 15, 120 });
    assertPartialElevation(four_point, 5, 25, new double[] { 0, 105, 5, 110, 15, 120, 20, 125 });

    assertPartialElevation(small_run, 10, 20.5, new double[] { 0, 110, 10, 120, 10.5, 120.5 });
    assertPartialElevation(
      small_run,
      0,
      20.25,
      new double[] { 0, 100, 10, 110, 20, 120, 20.25, 120.25 }
    );
    assertPartialElevation(
      small_run,
      0.25,
      20.25,
      new double[] { 0, 100.25, 9.75, 110, 19.75, 120, 20, 120.25 }
    );
  }

  private static List<PackedCoordinateSequence> slopeCases() {
    return List.of(STEEP_ELEVATION_PROFILE, STEEP_DOWNHILL_PROFILE);
  }

  @ParameterizedTest
  @MethodSource("slopeCases")
  void nonNegativeSlopeSpeedFactorOnSteepProfiles(PackedCoordinateSequence seq) {
    var slopeCosts = ElevationUtils.getSlopeCosts(seq);
    assertThat(slopeCosts.slopeSpeedFactor).isGreaterThan(0);
  }

  private static void assertPartialElevation(
    double[] coordinates,
    double begin,
    double end,
    double[] expectedCoordinates
  ) {
    var elevationProfile = new PackedCoordinateSequence.Double(coordinates, 2, 0);

    var partialElevationProfile = ElevationUtils.getPartialElevationProfile(
      elevationProfile,
      begin,
      end
    );

    if (expectedCoordinates == null) {
      assertNull(partialElevationProfile);
    } else {
      var expectedElevationProfile = new PackedCoordinateSequence.Double(
        expectedCoordinates,
        2,
        0
      ).toCoordinateArray();
      var actualElevationProfile = partialElevationProfile != null
        ? partialElevationProfile.toCoordinateArray()
        : null;
      assertArrayEquals(expectedElevationProfile, actualElevationProfile);
    }
  }
}
