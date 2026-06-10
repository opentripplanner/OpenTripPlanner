package org.opentripplanner.street.model.elevation;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

class SlopeCostCalculatorTest {

  @Test
  void testLengthMultiplier() {
    PackedCoordinateSequenceFactory factory = PackedCoordinateSequenceFactory.DOUBLE_FACTORY;
    CoordinateSequence seq = factory.create(
      new Coordinate[] { new Coordinate(0, 1), new Coordinate(10, 1) }
    );
    SlopeCosts costs = SlopeCostCalculator.getSlopeCosts(seq);
    assertEquals(1.0, costs.lengthMultiplier);

    seq = factory.create(new Coordinate[] { new Coordinate(0, 1), new Coordinate(10, 2) });
    costs = SlopeCostCalculator.getSlopeCosts(seq);
    assertEquals(1.00498756211208902702, costs.lengthMultiplier);

    seq = factory.create(
      new Coordinate[] { new Coordinate(0, 1), new Coordinate(10, 2), new Coordinate(15, 1) }
    );
    costs = SlopeCostCalculator.getSlopeCosts(seq);
    assertEquals(1.00992634231424500668, costs.lengthMultiplier);
  }

  @Test
  void testCalculateSlopeWalkEffectiveLengthFactor() {
    // 35% should hit the MAX_SLOPE_WALK_EFFECTIVE_LENGTH_FACTOR=3, hence 300m is expected
    assertEquals(300.0, SlopeCostCalculator.calculateEffectiveWalkLength(100, 35), 0.1);

    // 10% incline equals 1.42 penalty
    assertEquals(141.9, SlopeCostCalculator.calculateEffectiveWalkLength(100, 10), 0.1);

    // Flat is flat, no penalty
    assertEquals(120.0, SlopeCostCalculator.calculateEffectiveWalkLength(120, 0));

    // 5% downhill is the fastest to walk and effective distance only 0.83 * flat distance
    assertEquals(83.9, SlopeCostCalculator.calculateEffectiveWalkLength(100, -5), 0.1);

    // 10% downhill is about the same as flat
    assertEquals(150.0, SlopeCostCalculator.calculateEffectiveWalkLength(150, -15));

    // 15% downhill have a penalty of 1.19
    assertEquals(238.2, SlopeCostCalculator.calculateEffectiveWalkLength(200, -30), 0.1);

    // 45% downhill hit the MAX_SLOPE_WALK_EFFECTIVE_LENGTH_FACTOR=3 again
    assertEquals(300.0, SlopeCostCalculator.calculateEffectiveWalkLength(100, -45), 0.1);
  }

  private static List<PackedCoordinateSequence> slopeCases() {
    return List.of(STEEP_ELEVATION_PROFILE, STEEP_DOWNHILL_PROFILE);
  }

  @ParameterizedTest
  @MethodSource("slopeCases")
  void nonNegativeSlopeSpeedFactorOnSteepProfiles(PackedCoordinateSequence seq) {
    var slopeCosts = SlopeCostCalculator.getSlopeCosts(seq);
    assertThat(slopeCosts.slopeSpeedFactor).isGreaterThan(0);
  }

  /**
   * Profiles with slopes between 35% and 100% are not glitches but genuine terrain (alpine
   * footpaths, mountain tracks). The true slope must be preserved for {@code maxSlope} so that
   * downstream consumers like wheelchair reluctance can still react to it.
   */
  @Test
  void maxSlopeReflectsRealSlopeOn35To100PercentBand() {
    var costs = SlopeCostCalculator.getSlopeCosts(STEEP_ELEVATION_PROFILE);
    // STEEP_ELEVATION_PROFILE: segments with slope +0.52 and -0.72.
    assertThat(costs.maxSlope).isWithin(1e-9).of(0.72);
    assertThat(costs.flattened).isFalse();
  }

  /**
   * Walking effective length is computed via Tobler's hiking function, which has its own
   * cap. On a steep but genuine profile it must produce a penalty greater than on a flat
   * profile of the same horizontal length.
   */
  @Test
  void effectiveWalkFactorPenalisesGenuineSteepProfile() {
    var steep = SlopeCostCalculator.getSlopeCosts(STEEP_ELEVATION_PROFILE);
    var flat = SlopeCostCalculator.getSlopeCosts(flatProfile());
    assertThat(steep.effectiveWalkFactor).isGreaterThan(flat.effectiveWalkFactor);
  }

  /** The bike energy formula is cubic and stays positive — but it must see the real uphill. */
  @Test
  void slopeWorkFactorPenalisesGenuineSteepUphill() {
    var steep = SlopeCostCalculator.getSlopeCosts(STEEP_ELEVATION_PROFILE);
    var flat = SlopeCostCalculator.getSlopeCosts(flatProfile());
    assertThat(steep.slopeWorkFactor).isGreaterThan(flat.slopeWorkFactor);
  }

  /**
   * The {@code flattened} flag means "this segment was discarded as bad data". It must fire
   * only above the 100% glitch threshold, not for genuinely steep terrain in the 35–100%
   * band.
   */
  @Test
  void flattenedFlagSetOnlyAbove100Percent() {
    var genuine = SlopeCostCalculator.getSlopeCosts(STEEP_ELEVATION_PROFILE);
    assertThat(genuine.flattened).isFalse();

    var glitch = SlopeCostCalculator.getSlopeCosts(STEEP_DOWNHILL_PROFILE);
    assertThat(glitch.flattened).isTrue();
    // Every segment in STEEP_DOWNHILL_PROFILE exceeds 100%, so all slopes are zeroed.
    assertThat(glitch.maxSlope).isEqualTo(0.0);
  }

  private static PackedCoordinateSequence flatProfile() {
    return new PackedCoordinateSequence.Double(
      new Coordinate[] { new Coordinate(0, 100), new Coordinate(50, 100) }
    );
  }
}
