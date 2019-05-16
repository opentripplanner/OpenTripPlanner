package org.opentripplanner.routing.util;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ElevationUtils {
    private static Logger log = LoggerFactory.getLogger(ElevationUtils.class);

    /*
     * These numbers disagree with everything else I (David Turner) have read about the energy cost
     * of cycling but given that we are going to be fudging them anyway, they're not totally crazy
     */
    private static final double ENERGY_PER_METER_ON_FLAT = 1;

    private static final double ENERGY_SLOPE_FACTOR = 4000;

    // Coefficient for velocity-dependent dynamic rolling resistance, here approximated with 0.1
    // See http://www.kreuzotter.de/english/espeed.htm
    private static final double CrV = 0.1;

    /**
     * Coefficient for the dynamic rolling resistance, normalized to road inclination; CrVn = CrV*cos(β)
     *
     * @param beta Inclination angle, = arctan(grade/100)
     */
    public static double getDynamicRollingResistance(double beta) {
        return CrV * Math.cos(beta);
    }

    /**
     * This gravitational constant is actually dependent on latitude and height. However, the amount of variation is not
     * that much, so we just use the constant at 45 degrees of latitude.
     * See https://en.wikipedia.org/wiki/Gravitational_acceleration#Gravity_model_for_Earth
     */
    public static final double GRAVITATIONAL_ACCELERATION_CONSTANT = 9.80665;

    // the Cd * A * p value at 0 elevation
    public static final double ZERO_ELEVATION_DRAG_RESISTIVE_FORCE_COMPONENT = getDragResistiveForceComponent(0);

    /**
     * This is  coefficient of drag and frontal area multiplied together. The equation for drag resistance and the
     * extracted value is as follows:
     *
     * Fdrag = 0.5 * Cd * A * Rho * V^2
     *               ⎣CdA_⎦
     *
     * See See https://www.gribble.org/cycling/power_v_speed.html
     *
     * where
     * Cd = coefficient of drag
     * A = frontal area in m^2
     * Rho = air density in kg / m^3
     *
     * Apparently you need a wind tunnel to properly measure the coefficient of drag, so for now, assume the following:
     * Cd = 0.63
     * A = 0.6
     */
    private static final double CdA = 0.63 * 0.6;

    // the air pressure at sea level in Pascals
    // see https://www.omnicalculator.com/physics/air-pressure-at-altitude
    private static final int AIR_PRESSURE_AT_SEA_LEVEL = 101325;

    // see https://www.omnicalculator.com/physics/air-pressure-at-altitude
    private static final double EARTHY_AIR_MOLAR_MASS = 0.0289644;

    private static final double UNIVERSAL_GAS_CONSTANT = 8.31432;

    // 293°K ~= 19.85°C ~= 67.73°F
    // A slightly higher number than the average earth temperature is assumed since more people live in warmer climates
    // and may not use transportation exposed to the elements until a reasonably nice outdoor temperature.
    private static final int A_RANDOM_OUTDOOR_TEMPERATURE_IN_KELVIN = 293;

    // see https://www.omnicalculator.com/physics/air-density
    private static final double SPECIFIC_GAS_CONSTANT_FOR_DRY_AIR = 287.058;

    /**
     * Calculates the componens of drag resistance except for the velocity assuming travel through dry earthy air. The
     * equation for drag resistance and the extracted value is as follows:
     *
     * Fdrag = 0.5 * Cd * A * Rho * V^2
     *         ⎣__dragComponent_⎦
     *
     * See https://www.gribble.org/cycling/power_v_speed.html
     *
     * The CDA is defined as a constant, but Rho represents air density which is dependent on a lot of things. The full
     * equation is as follows:
     *
     * ρ = (pd / (Rd * T)) + (pv / (Rv * T))
     *
     * See https://www.omnicalculator.com/physics/air-density
     *
     * where
     * pd = air pressure in Pascals
     * Rd = specific gas constant for dry air
     * T = air temperature in Kelvin
     * pv = water vapor pressure in Pascals
     * Rv = specific gas constant for water vapor
     *
     * In the second half of the equation (pv / (Rv * T)), this is a calculation given the water vapor pressure. If we
     * assume travel through dry air, this number effictively becomes 0. Therefore, this calculation is ommitted.
     *
     * Therefore, that leave us with:
     *
     * ρ = (pd / (Rd * T))
     *
     * where:
     * pd = the pressure of dry air in hPa,
     * Rd is the specific gas constant for dry air
     * T is the air temperature in Kelvins
     *
     * The pressure of dry air is described with the following formula:
     *
     * P = P0 * exp (-g * M * (h-h0) / (R * T))
     *
     * See https://www.omnicalculator.com/physics/air-pressure-at-altitude
     *
     * where
     * P0 = is the pressure at the reference level h0 which is assumed to be 0 meters above sea level
     * g = the gravitational acceleration constant
     * M = the molar mass of air
     * h = the altitude in meters at which we want to calculate the pressure
     * R = the universal gas constant
     * T = the temperature at altitude h
     *
     * P0, g, M and R are all constants. h is provided as an input parameter. And we randomly guess T. Furthermore, the
     * temperature is assumed to fall 9.8°C per 1,000 meters.
     *
     * See https://www.onthesnow.com/news/a/15157/does-elevation-affect-temperature
     *
     * @param altitude The altitude in meters
     */
    public static double getDragResistiveForceComponent(double altitude) {
        double randomlyGuessedTemperature = A_RANDOM_OUTDOOR_TEMPERATURE_IN_KELVIN - 9.8 * altitude / 1000;
        return CdA * (
            AIR_PRESSURE_AT_SEA_LEVEL *
            Math.exp(
                -GRAVITATIONAL_ACCELERATION_CONSTANT *
                    EARTHY_AIR_MOLAR_MASS *
                    altitude /
                    (UNIVERSAL_GAS_CONSTANT * randomlyGuessedTemperature)
            ) /
            (SPECIFIC_GAS_CONSTANT_FOR_DRY_AIR * randomlyGuessedTemperature)
        );
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

    /**
     * 
     * @param elev The elevation profile, where each (x, y) is (distance along edge, elevation)
     * @param slopeLimit Whether the slope should be limited to 0.35, which is the max slope for
     * streets that take cars.
     * @return
     */
    public static SlopeCosts getSlopeCosts(CoordinateSequence elev, boolean slopeLimit) {
        Coordinate[] coordinates = elev.toCoordinateArray();
        boolean flattened = false;
        double maxSlope = 0;
        double slopeSpeedEffectiveLength = 0;
        double slopeWorkCost = 0;
        double slopeSafetyCost = 0;
        double[] lengths = getLengthsFromElevation(elev);
        double trueLength = lengths[0];
        double flatLength = lengths[1];
        if (flatLength < 1e-3) {
            log.error("Too small edge, returning neutral slope costs.");
            return new SlopeCosts(
                1.0,
                1.0,
                0.0,
                0.0,
                1.0,
                false,
                new byte[]{0},
                new short[]{(short) trueLength},
                new float[]{(float) getDragResistiveForceComponent(0)}
            );
        }
        double lengthMultiplier = trueLength / flatLength;
        List<GradientBin> gradients = new ArrayList<>();
        for (int i = 0; i < coordinates.length - 1; ++i) {
            double run = coordinates[i + 1].x - coordinates[i].x;
            double rise = coordinates[i + 1].y - coordinates[i].y;
            if (run == 0) {
                continue;
            }
            double slope = rise / run;

            // store gradients for use in micromobility calculations
            // Micromobility speed calculations can handle extreme slopes. However, cap them at 100% or -100%.
            int iGradient = slope < -1
                ? -100
                : slope > 1
                ? 100
                : (int) Math.round(slope * 100.0);

            // add to existing gradient bin
            boolean gradientAdded = false;
            double minCoordinatesAltitude = Math.min(coordinates[i + 1].x, coordinates[i].x);
            for (GradientBin bin : gradients) {
                if (bin.gradient == iGradient) {
                    bin.distance += run;
                    // always use the minimum altitude for maximum air density to underestimate travel time
                    bin.minAltitude = Math.min(bin.minAltitude, minCoordinatesAltitude);
                    gradientAdded = true;
                    break;
                }
            }
            // or create new bin for new gradient
            if (!gradientAdded) {
                GradientBin bin = new GradientBin();
                bin.gradient = iGradient;
                bin.distance = run;
                bin.minAltitude = minCoordinatesAltitude;
                gradients.add(bin);
            }

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
            double energy = hypotenuse
                    * (ENERGY_PER_METER_ON_FLAT + ENERGY_SLOPE_FACTOR * slope_or_zero
                            * slope_or_zero * slope_or_zero);
            slopeWorkCost += energy;
            double slopeSpeedCoef = slopeSpeedCoefficient(slope, coordinates[i].y);
            slopeSpeedEffectiveLength += hypotenuse / slopeSpeedCoef;
            // assume that speed and safety are inverses
            double safetyCost = hypotenuse * (slopeSpeedCoef - 1) * 0.25;
            if (safetyCost > 0) {
                slopeSafetyCost += safetyCost;
            }
        }

        // convert gradient info into arrays of primitives
        byte[] gradientsArr = new byte[gradients.size()];
        short[] gradientLengthsArr = new short[gradients.size()];
        float[] gradientCdas = new float[gradients.size()];
        for (int i = 0; i < gradients.size(); i++) {
            GradientBin bin = gradients.get(i);
            gradientsArr[i] = (byte) bin.gradient;
            gradientLengthsArr[i] = (short) bin.distance;
            gradientCdas[i] = (float) getDragResistiveForceComponent(bin.minAltitude);
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
            gradientsArr,
            gradientLengthsArr,
            gradientCdas
        );
    }

    /** constants for slope computation */
    final static double tx[] = { 0.0000000000000000E+00, 0.0000000000000000E+00, 0.0000000000000000E+00,
            2.7987785324442748E+03, 5.0000000000000000E+03, 5.0000000000000000E+03,
            5.0000000000000000E+03 };
    final static double ty[] = { -3.4999999999999998E-01, -3.4999999999999998E-01, -3.4999999999999998E-01,
            -7.2695627831828688E-02, -2.4945814335295903E-03, 5.3500304527448035E-02,
            1.2191105175593375E-01, 3.4999999999999998E-01, 3.4999999999999998E-01,
            3.4999999999999998E-01 };
    final static double coeff[] = { 4.3843513168660255E+00, 3.6904323727375652E+00, 1.6791850199667697E+00,
            5.5077866957024113E-01, 1.7977766419113900E-01, 8.0906832222762959E-02,
            6.0239305785343762E-02, 4.6782343053423814E+00, 3.9250580214736304E+00,
            1.7924585866601270E+00, 5.3426170441723031E-01, 1.8787442260720733E-01,
            7.4706427576152687E-02, 6.2201805553147201E-02, 5.3131908923568787E+00,
            4.4703901299120750E+00, 2.0085381385545351E+00, 5.4611063530784010E-01,
            1.8034042959223889E-01, 8.1456939988273691E-02, 5.9806795955995307E-02,
            5.6384893192212662E+00, 4.7732222200176633E+00, 2.1021485412233019E+00,
            5.7862890496126462E-01, 1.6358571778476885E-01, 9.4846184210137130E-02,
            5.5464612133430242E-02 };

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

        double h[] = { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
        double hh[] = { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
        double w_x[] = { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
        double w_y[] = { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };

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


    /** parameter A in the Rees (2004) slope-dependent walk cost model **/
    private static double walkParA = 0.75;
    /** parameter C in the Rees (2004) slope-dependent walk cost model **/
    private static double walkParC = 14.6;

    /**
     * The cost for walking in hilly/mountain terrain dependent on slope using an empirical function by
     * WG Rees (Comp & Geosc, 2004), that overhauls the Naismith rule for mountaineering.<br>
     * For a slope of 0 = 0 degree a cost is returned that approximates a speed of 1.333 m/sec = 4.8km/h<br>
     * TODO: Not sure if it makes sense to use maxSlope as input and instead better use
     * a lower estimate / average value. However, the DEM is most likely generalized/smoothed
     * and hence maxSlope may be smaller than in the real world.
     * @param verticalDistance the vertical distance of the line segment
     * @param maxSlope the slope of the segment
     * @return walk costs dependent on slope (in seconds)
     */
    public static double getWalkCostsForSlope(double verticalDistance, double maxSlope) {
        /*
        Naismith (1892):
        "an hour for every three miles on the map, with an additional hour for
        every 2,000 feet of ascent.'
        -------
        in S. Fritz and S. Carver (GISRUK 1998):
        Naismith's Rule: 5 km/h plus 1 hour per 600m ascent; minus 10 minutes per 300 m
        descent for slopes between 5 and 12 degrees; plus 10 minutes per 300m descent
        for slopes greater than 12 degrees.
        ...
        In the case of a 50m grid resolution DEM for every m climbed, 6 seconds are added.
        2 seconds are added in case of a ascent of more than 12 degrees and 2 seconds are
        subtracted if the ascent is between 5-12 degrees.
        -------
        Naismith's rule was overhauled by W.G. Rees (2004), who developed a quadratic
        function for speed estimation:
                1/v = a + b*m + c*m^2
        with a= 0.75 sec/m, b=0.09 s/m, c=14.6 s/m

        As for b=0 there are no big differences the derived cost function is:
                 k = a*d + c * (h*h) / d
        with d= distance, and h = vertical separation

        */
        if (verticalDistance == 0){
            return 0;
        }
        double costs = 0;
        double h = maxSlope * verticalDistance;
        costs = (walkParA * verticalDistance) + (  walkParC * (h * h) / verticalDistance); 
        return  costs;
    }

    public static PackedCoordinateSequence getPartialElevationProfile(
            PackedCoordinateSequence elevationProfile, double start, double end) {
        if (elevationProfile == null) {
            return null;
        }
        List<Coordinate> coordList = new LinkedList<Coordinate>();

        if (start < 0)
            start = 0;

        Coordinate[] coordinateArray = elevationProfile.toCoordinateArray();
        double length = coordinateArray[coordinateArray.length - 1].x;
        if (end > length)
            end = length;

        boolean started = false;
        boolean finished = false;
        Coordinate lastCoord = null;
        for (Coordinate coord : coordinateArray) {
            if (coord.x >= start && coord.x <= end) {
                coordList.add(new Coordinate(coord.x - start, coord.y));
                if (!started) {
                    started = true;
                    if (lastCoord == null) {
                       //no need to interpolate as this is the first coordinate
                        continue;
                    }
                    // interpolate start coordinate 
                    double run = coord.x - lastCoord.x;
                    if (run < 1) {
                        //tiny runs are likely to lead to errors, so we'll skip them
                        continue;
                    }
                    double p = (coord.x - start) / run;
                    double rise = coord.y - lastCoord.y;
                    Coordinate interpolatedStartCoordinate = new Coordinate(0, lastCoord.y + p * rise);
                    coordList.add(0, interpolatedStartCoordinate);
                }
            } else if (coord.x > end && !finished && started && lastCoord != null) {
                finished = true;
                // interpolate end coordinate
                double run = coord.x - lastCoord.x;
                if (run < 1) {
                    //tiny runs are likely to lead to errors, so we'll skip them
                    continue;
                }
                double p = (end - lastCoord.x) / run;
                double rise = coord.y - lastCoord.y;
                Coordinate interpolatedEndCoordinate = new Coordinate(end, lastCoord.y + p * rise);
                coordList.add(interpolatedEndCoordinate);
            }
            lastCoord = coord;
        }

        Coordinate coordArr[] = new Coordinate[coordList.size()];
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

    private static class GradientBin {
        public int gradient;
        public double distance;
        public double minAltitude;
    }
}
