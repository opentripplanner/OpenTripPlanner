package org.opentripplanner.routing.util;

import java.util.LinkedList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.opentripplanner.routing.util.elevation.ToblersHikingFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElevationUtils {

  private static final Logger log = LoggerFactory.getLogger(ElevationUtils.class);

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

  private static final ToblersHikingFunction toblerWalkingFunction = new ToblersHikingFunction(
    MAX_SLOPE_WALK_EFFECTIVE_LENGTH_FACTOR
  );
  /** constants for slope computation */
  static final double[] tx = {
    0.0000000000000000E+00,
    0.0000000000000000E+00,
    0.0000000000000000E+00,
    2.7987785324442748E+03,
    5.0000000000000000E+03,
    5.0000000000000000E+03,
    5.0000000000000000E+03,
  };
  static final double[] ty = {
    -3.4999999999999998E-01,
    -3.4999999999999998E-01,
    -3.4999999999999998E-01,
    -7.2695627831828688E-02,
    -2.4945814335295903E-03,
    5.3500304527448035E-02,
    1.2191105175593375E-01,
    3.4999999999999998E-01,
    3.4999999999999998E-01,
    3.4999999999999998E-01,
  };
  static final double[] coeff = {
    4.3843513168660255E+00,
    3.6904323727375652E+00,
    1.6791850199667697E+00,
    5.5077866957024113E-01,
    1.7977766419113900E-01,
    8.0906832222762959E-02,
    6.0239305785343762E-02,
    4.6782343053423814E+00,
    3.9250580214736304E+00,
    1.7924585866601270E+00,
    5.3426170441723031E-01,
    1.8787442260720733E-01,
    7.4706427576152687E-02,
    6.2201805553147201E-02,
    5.3131908923568787E+00,
    4.4703901299120750E+00,
    2.0085381385545351E+00,
    5.4611063530784010E-01,
    1.8034042959223889E-01,
    8.1456939988273691E-02,
    5.9806795955995307E-02,
    5.6384893192212662E+00,
    4.7732222200176633E+00,
    2.1021485412233019E+00,
    5.7862890496126462E-01,
    1.6358571778476885E-01,
    9.4846184210137130E-02,
    5.5464612133430242E-02,
  };

  /**
   * @param elev       The elevation profile, where each (x, y) is (distance along edge, elevation)
   * @param slopeLimit Whether the slope should be limited to 0.35, which is the max slope for
   *                   streets that take cars.
   */
  public static SlopeCosts getSlopeCosts(CoordinateSequence elev, boolean slopeLimit) {
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
      // Baldwin St in Dunedin, NZ, is the steepest street
      // on earth, and has a grade of 35%.  So for streets
      // which allow cars, we set the limit to 35%.  Footpaths
      // are sometimes steeper, so we turn slopeLimit off for them.
      // But we still need some sort of limit, because the energy
      // usage approximation breaks down at extreme slopes, and
      // gives negative weights
      if ((slopeLimit && (slope > 0.35 || slope < -0.35)) || slope > 1.0 || slope < -1.0) {
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
      double slopeSpeedCoef = slopeSpeedCoefficient(slope, coordinates[i].y);
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

  public static double slopeSpeedCoefficient(double slope, double altitude) {
    /*
     * computed by asking ZunZun for a quadratic b-spline approximating some values from
     * http://www.analyticcycling.com/ForcesSpeed_Page.html fixme: should clamp to local speed
     * limits (code is from ZunZun)
     */

    int nx = 7;
    int ny = 10;
    int kx = 2;
    int ky = 2;

    double[] h = {
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
    };
    double[] hh = {
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
    };
    double[] w_x = {
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
    };
    double[] w_y = {
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
    };

    int i, j, li, lj, lx, ky1, nky1, ly, i1, j1, l2;
    double f, temp;

    int kx1 = kx + 1;
    int nkx1 = nx - kx1;
    int l = kx1;
    int l1 = l + 1;

    while ((altitude >= tx[l1 - 1]) && (l != nkx1)) {
      l = l1;
      l1 = l + 1;
    }

    h[0] = 1.0;
    for (j = 1; j < kx + 1; j++) {
      for (i = 0; i < j; i++) {
        hh[i] = h[i];
      }
      h[0] = 0.0;
      for (i = 0; i < j; i++) {
        li = l + i;
        lj = li - j;
        if (tx[li] != tx[lj]) {
          f = hh[i] / (tx[li] - tx[lj]);
          h[i] = h[i] + f * (tx[li] - altitude);
          h[i + 1] = f * (altitude - tx[lj]);
        } else {
          h[i + 1 - 1] = 0.0;
        }
      }
    }

    lx = l - kx1;
    for (j = 0; j < kx1; j++) {
      w_x[j] = h[j];
    }

    ky1 = ky + 1;
    nky1 = ny - ky1;
    l = ky1;
    l1 = l + 1;

    while ((slope >= ty[l1 - 1]) && (l != nky1)) {
      l = l1;
      l1 = l + 1;
    }

    h[0] = 1.0;
    for (j = 1; j < ky + 1; j++) {
      for (i = 0; i < j; i++) {
        hh[i] = h[i];
      }
      h[0] = 0.0;
      for (i = 0; i < j; i++) {
        li = l + i;
        lj = li - j;
        if (ty[li] != ty[lj]) {
          f = hh[i] / (ty[li] - ty[lj]);
          h[i] = h[i] + f * (ty[li] - slope);
          h[i + 1] = f * (slope - ty[lj]);
        } else {
          h[i + 1 - 1] = 0.0;
        }
      }
    }

    ly = l - ky1;
    for (j = 0; j < ky1; j++) {
      w_y[j] = h[j];
    }

    l = lx * nky1;
    for (i1 = 0; i1 < kx1; i1++) {
      h[i1] = w_x[i1];
    }

    l1 = l + ly;
    temp = 0.0;
    for (i1 = 0; i1 < kx1; i1++) {
      l2 = l1;
      for (j1 = 0; j1 < ky1; j1++) {
        l2 = l2 + 1;
        temp = temp + coeff[l2 - 1] * h[i1] * w_y[j1];
      }
      l1 = l1 + nky1;
    }

    return temp;
  }

  public static PackedCoordinateSequence getPartialElevationProfile(
    PackedCoordinateSequence elevationProfile,
    double start,
    double end
  ) {
    if (elevationProfile == null) {
      return null;
    }

    if (start < 0) {
      start = 0;
    }

    Coordinate[] coordinateArray = elevationProfile.toCoordinateArray();
    double length = coordinateArray[coordinateArray.length - 1].x;
    if (end > length) {
      end = length;
    }

    double newLength = end - start;

    boolean started = false;
    Coordinate lastCoord = null;
    List<Coordinate> coordList = new LinkedList<>();
    for (Coordinate coord : coordinateArray) {
      if (coord.x >= start && !started) {
        started = true;

        if (lastCoord != null) {
          double run = coord.x - lastCoord.x;
          double p = (start - lastCoord.x) / run;
          double rise = coord.y - lastCoord.y;
          double newX = lastCoord.x + p * run - start;
          double newY = lastCoord.y + p * rise;

          if (p > 0 && p < 1) {
            coordList.add(new Coordinate(newX, newY));
          }
        }
      }

      if (started && coord.x >= start && coord.x <= end) {
        coordList.add(new Coordinate(coord.x - start, coord.y));
      }

      if (started && coord.x >= end) {
        if (lastCoord != null && lastCoord.x < end && coord.x > end) {
          double run = coord.x - lastCoord.x;
          // interpolate end coordinate
          double p = (end - lastCoord.x) / run;
          double rise = coord.y - lastCoord.y;
          double newY = lastCoord.y + p * rise;
          coordList.add(new Coordinate(newLength, newY));
        }
        break;
      }

      lastCoord = coord;
    }

    if (coordList.size() < 2) {
      return null;
    }

    Coordinate[] coordArr = new Coordinate[coordList.size()];
    return new PackedCoordinateSequence.Float(coordList.toArray(coordArr), 2);
  }

  /** checks for units (m/ft) in an OSM ele tag value, and returns the value in meters */
  public static Double parseEleTag(String ele) {
    ele = ele.toLowerCase();
    double unit = 1;
    if (ele.endsWith("m")) {
      ele = ele.replaceFirst("\\s*m", "");
    } else if (ele.endsWith("ft")) {
      ele = ele.replaceFirst("\\s*ft", "");
      unit = 0.3048;
    }
    try {
      return Double.parseDouble(ele) * unit;
    } catch (NumberFormatException e) {
      return null;
    }
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
    return run * toblerWalkingFunction.calculateHorizontalWalkingDistanceMultiplier(run, rise);
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
