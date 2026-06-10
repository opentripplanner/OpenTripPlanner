package org.opentripplanner.street.model.elevation;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;

/**
 * Computes slope-related cost factors for an edge from its elevation profile.
 * <p>
 * Bicycle speed and safety derive from {@link BicycleSlopeSpeedFunction}, a quadratic
 * B-spline fitted to analytical cycling data. The bike work cost uses a cubic energy term
 * ({@link #ENERGY_PER_METER_ON_FLAT} + {@link #ENERGY_SLOPE_FACTOR}&middot;slope&sup3;) and
 * the walking effective length uses {@link ToblersHikingFunction}.
 * <p>
 * Segments with a slope above &plusmn;100% are treated as raster/OSM data glitches and
 * flattened to zero entirely (see {@link #MAX_ABS_SLOPE}); the real slope is preserved for
 * everything between &plusmn;35% and &plusmn;100% so that wheelchair reluctance, walking
 * length and bike energy still see genuine steep terrain. The B-spline itself clamps its
 * input to &plusmn;35% internally to stay within its valid domain.
 */
public class SlopeCostCalculator {

  /*
   * These numbers disagree with everything else I (David Turner) have read about the energy cost
   * of cycling but given that we are going to be fudging them anyway, they're not totally crazy
   */
  private static final double ENERGY_PER_METER_ON_FLAT = 1;

  private static final double ENERGY_SLOPE_FACTOR = 4000;

  /**
   * If the calculated factor is more than this constant, we ignore the calculated factor and use
   * this constant in stead. See ths table in {@link ToblersHikingFunction} for a mapping between
   * the factor and angels(degree and percentage). A factor of 3 with take effect for slopes with a
   * incline above 31.4% and a decline below 41.4%. The worlds steepest road ia about 35%, and the
   * steepest climes in Tour De France is usually in the range 8-12%. Some walking paths may be
   * quite steep, but a penalty of 3 is still a large penalty.
   */
  private static final double MAX_SLOPE_WALK_EFFECTIVE_LENGTH_FACTOR = 3;

  private static final ToblersHikingFunction TOBLER_WALKING_FUNCTION = new ToblersHikingFunction(
    MAX_SLOPE_WALK_EFFECTIVE_LENGTH_FACTOR
  );

  /// Slopes steeper than 100% are treated as raster/OSM data glitches and flattened to zero.
  /// No real road or path is that steep — footpaths and tracks in mountain terrain that
  /// genuinely exceed 35% stay below this threshold.
  private static final double MAX_ABS_SLOPE = 1.0;

  /// Compute the slope costs for an elevation profile.
  ///
  /// Slopes above ±100% ({@link #MAX_ABS_SLOPE}) are treated as raster/OSM data glitches and
  /// flattened to zero — no real road or path is that steep, so the segment is dropped from
  /// every cost computation and the {@code flattened} flag is set.
  ///
  /// Genuinely steep terrain (alpine footpaths, mountain tracks) between ±35% and ±100% keeps
  /// its real slope for `maxSlope` (wheelchair reluctance), the bike energy formula, the Tobler
  /// walking length and the length multiplier. Only the B-spline used for bicycle speed clamps
  /// its input internally (see {@link BicycleSlopeSpeedFunction}); the rationale is in
  /// [PR #7579](https://github.com/opentripplanner/OpenTripPlanner/pull/7579).
  ///
  /// @param elev The elevation profile, where each (x, y) is (distance along edge, elevation)
  public static SlopeCosts getSlopeCosts(CoordinateSequence elev) {
    Coordinate[] coordinates = elev.toCoordinateArray();
    boolean flattened = false;
    double maxSlope = 0;
    double slopeSpeedEffectiveLength = 0;
    double slopeWorkCost = 0;
    double slopeSafetyCost = 0;
    double effectiveWalkLength = 0;
    double[] lengths = getLengthsFromElevation(elev);
    double trueLength = lengths[0];
    double flatLength = lengths[1];
    if (flatLength < 1e-3) {
      // Too small edge, returning neutral slope costs.
      return new SlopeCosts(1.0, 1.0, 0.0, 0.0, 1.0, false, 1.0);
    }
    double lengthMultiplier = trueLength / flatLength;
    for (int i = 0; i < coordinates.length - 1; ++i) {
      double run = coordinates[i + 1].x - coordinates[i].x;
      double rise = coordinates[i + 1].y - coordinates[i].y;
      if (run == 0) {
        continue;
      }
      double slope = rise / run;
      // Slopes above 100% can only be raster/OSM glitches — drop the segment entirely.
      if (slope > MAX_ABS_SLOPE || slope < -MAX_ABS_SLOPE) {
        slope = 0;
        flattened = true;
      }
      if (maxSlope < Math.abs(slope)) {
        maxSlope = Math.abs(slope);
      }

      double slope_or_zero = Math.max(slope, 0);
      double hypotenuse = Math.sqrt(rise * rise + run * run);
      double energy =
        hypotenuse *
        (ENERGY_PER_METER_ON_FLAT +
          ENERGY_SLOPE_FACTOR * slope_or_zero * slope_or_zero * slope_or_zero);
      slopeWorkCost += energy;
      double slopeSpeedCoef = BicycleSlopeSpeedFunction.coefficient(slope, coordinates[i].y);
      slopeSpeedEffectiveLength += run / slopeSpeedCoef;
      // assume that speed and safety are inverses
      double safetyCost = hypotenuse * (slopeSpeedCoef - 1) * 0.25;
      if (safetyCost > 0) {
        slopeSafetyCost += safetyCost;
      }
      effectiveWalkLength += calculateEffectiveWalkLength(run, rise);
    }
    /*
     * Here we divide by the *flat length* as the slope/work cost factors are multipliers of the
     * length of the street edge which is the flat one.
     */
    return new SlopeCosts(
      slopeSpeedEffectiveLength / flatLength,
      slopeWorkCost / flatLength,
      slopeSafetyCost,
      maxSlope,
      lengthMultiplier,
      flattened,
      effectiveWalkLength / flatLength
    );
  }

  /**
   * <p>
   * We use the Tobler function {@link ToblersHikingFunction} to calculate this.
   * </p>
   * <p>
   * When testing this we get good results in general, but for some edges the elevation profile is
   * not accurate. A (serpentine) road is usually build with a constant slope, but the elevation
   * profile in OTP is not as smooth, resulting in an extra penalty for these roads.
   * </p>
   */
  static double calculateEffectiveWalkLength(double run, double rise) {
    return run * TOBLER_WALKING_FUNCTION.calculateHorizontalWalkingDistanceMultiplier(run, rise);
  }

  private static double[] getLengthsFromElevation(CoordinateSequence elev) {
    double trueLength = 0;
    double flatLength = 0;
    double lastX = elev.getX(0);
    double lastY = elev.getY(0);
    for (int i = 1; i < elev.size(); ++i) {
      Coordinate c = elev.getCoordinate(i);
      double x = c.x - lastX;
      double y = c.y - lastY;
      trueLength += Math.sqrt(x * x + y * y);
      flatLength += x;
      lastX = c.x;
      lastY = c.y;
    }
    return new double[] { trueLength, flatLength };
  }
}
