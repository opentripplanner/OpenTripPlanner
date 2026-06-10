package org.opentripplanner.street.model.elevation;

/**
 * Cycling speed coefficient as a function of slope and altitude, modelled as a quadratic
 * B-spline fitted to data from
 * <a href="http://www.analyticcycling.com/ForcesSpeed_Page.html">analyticcycling.com</a>.
 * The coefficient is a multiplier on flat-ground cycling speed: greater than {@code 1.0} on
 * a downhill, less than {@code 1.0} on an uphill.
 * <p>
 * The spline is defined on a bounded domain given by its knot vectors:
 * <ul>
 *   <li>slope: {@code [-0.35, +0.35]}, i.e. &plusmn;35% &mdash; see {@link #MIN_SLOPE} and
 *       {@link #MAX_SLOPE}</li>
 *   <li>altitude: {@code [0, 5000]} metres</li>
 * </ul>
 * Outside this range the spline extrapolates and can return negative coefficients, so
 * {@link #coefficient(double, double)} clamps its slope argument to the valid range before
 * evaluation.
 */
public final class BicycleSlopeSpeedFunction {

  /** Lower bound of the spline's slope domain (steepest downhill the spline is defined for). */
  static final double MIN_SLOPE = -0.35;

  /** Upper bound of the spline's slope domain (steepest uphill the spline is defined for). */
  static final double MAX_SLOPE = 0.35;

  /** Altitude (x) knot vector. Bounds: {@code [0, 5000]} metres. */
  private static final double[] TX = {
    0.0000000000000000E+00,
    0.0000000000000000E+00,
    0.0000000000000000E+00,
    2.7987785324442748E+03,
    5.0000000000000000E+03,
    5.0000000000000000E+03,
    5.0000000000000000E+03,
  };

  /** Slope (y) knot vector. Bounds: {@code [-0.35, +0.35]}. */
  private static final double[] TY = {
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

  /** Spline coefficient matrix (flattened, row-major over the TX x TY grid). */
  private static final double[] COEFF = {
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

  private BicycleSlopeSpeedFunction() {}

  /**
   * Evaluate the spline at the given slope and altitude. The slope is clamped to
   * {@code [MIN_SLOPE, MAX_SLOPE]} before evaluation to keep the spline inside its valid
   * domain.
   *
   * @param slope    {@code rise / run} along the edge (no unit, e.g. {@code 0.05} for 5%)
   * @param altitude metres above sea level at the start of the segment
   * @return speed coefficient: {@code > 1.0} on downhills (faster), {@code < 1.0} on uphills
   */
  public static double coefficient(double slope, double altitude) {
    /*
     * computed by asking ZunZun for a quadratic b-spline approximating some values from
     * http://www.analyticcycling.com/ForcesSpeed_Page.html fixme: should clamp to local speed
     * limits (code is from ZunZun)
     */
    slope = Math.clamp(slope, MIN_SLOPE, MAX_SLOPE);

    int nx = 7;
    int ny = 10;
    int kx = 2;
    int ky = 2;

    double[] h = new double[25];
    double[] hh = new double[25];
    double[] w_x = new double[25];
    double[] w_y = new double[25];

    int i, j, li, lj, lx, ky1, nky1, ly, i1, j1, l2;
    double f, temp;

    int kx1 = kx + 1;
    int nkx1 = nx - kx1;
    int l = kx1;
    int l1 = l + 1;

    while ((altitude >= TX[l1 - 1]) && (l != nkx1)) {
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
        if (TX[li] != TX[lj]) {
          f = hh[i] / (TX[li] - TX[lj]);
          h[i] = h[i] + f * (TX[li] - altitude);
          h[i + 1] = f * (altitude - TX[lj]);
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

    while ((slope >= TY[l1 - 1]) && (l != nky1)) {
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
        if (TY[li] != TY[lj]) {
          f = hh[i] / (TY[li] - TY[lj]);
          h[i] = h[i] + f * (TY[li] - slope);
          h[i + 1] = f * (slope - TY[lj]);
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
        temp = temp + COEFF[l2 - 1] * h[i1] * w_y[j1];
      }
      l1 = l1 + nky1;
    }

    return temp;
  }
}
