/*
  This program is free software: you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public License
  as published by the Free Software Foundation, either version 3 of
  the License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.opentripplanner.routing.util.elevation;


/**
 * <p>
 * Tobler's hiking function is an exponential function determining the hiking speed, taking into account the
 * slope angle. It was formulated by Waldo Tobler. This function was estimated from empirical data of Eduard Imhof.
 * [ <a href="https://en.wikipedia.org/wiki/Tobler%27s_hiking_function">Wikipedia</a> ]
 * </p>
 * <pre>
 * Walking speed(W):
 *
 *     W = 6 m/s * Exp(-3.5 * Abs(dh/dx + 0.05))
 * </pre>
 *
 * <p>
 * The {@code 6 m/s} is the Maximum speed achieved. This happens at angle 2.86 degrees or -5% downhill. In OTP we
 * want to apply this as a multiplier to the horizontal walking distance. This is done for all walkable edges in
 * the graph. To find the walkingtime for an edge we use the horizontal walking speed. Therefore:
 * </p>
 * <pre>
 * Given:
 *   Vflat : Speed at 0 degrees - flat
 *   Vmax  : Maximum speed (6 m/s)
 *
 *   Vmax = C * Vflat
 *
 * Then:
 *               1
 *   C = ------------------  = 1.19
 *        EXP(-3.5 * 0.05)
 * And:
 *
 *   dx = Vflat *  t
 *   W = C * Vflat * EXP(-3.5 * ABS(dh/dx + 0.05))
 *
 * The <em>Horizontal walking distance multiplier(F)</em> then becomes:
 *
 *                      1
 *   F = -----------------------------------
 *        C * EXP(-3.5 * ABS(dh/dx + 0.05))
 *
 * Examples:
 *
 *   Angle  | Slope % | Horizontal walking distance multiplier
 *   -------+---------------------------------------
 *    19,3  |   35 %  |  3,41
 *    16,7  |   30 %  |  2,86
 *    14,0  |   25 %  |  2,40
 *    11,3  |   20 %  |  2,02
 *     8,5  |   15 %  |  1,69
 *     5,7  |   10 %  |  1,42
 *     2,9  |    5 %  |  1,19
 *     0,0  |    0 %  |  1,00
 *    −2,9  |   −5 %  |  0,84
 *    −5,7  |  −10 %  |  1,00
 *    −8,5  |  −15 %  |  1,19
 *   −11,3  |  −20 %  |  1,42
 *   −14,0  |  −25 %  |  1,69
 *   −16,7  |  −30 %  |  2,02
 *   −19,3  |  −35 %  |  2,40
 *   −21,8  |  −40 %  |  2,86
 *   −24,2  |  −45 %  |  3,41
 * </pre>
 */
public class ToblersHikingFunction {

    /**
     * The exponential growth factor in Tobler´s function.
     */
    private static final double E = -3.5;

    /**
     * The slope offset where the maximum speed will occur. The value 0.05 will result in a maximum speed at
     * -2.86 degrees (5% downhill).
     */
    private static final double A = 0.05;

    /** The horizontal speed to maximum speed factor: Vmax = C * Vflat */
    private static final double C = 1 / Math.exp(E * A);


    private final double walkDistMultiplierMaxLimit;


    /**
     * @param walkDistMultiplierMaxLimit this property is used to set a maximum limit for the horizontal walking
     *                                   distance multiplier. Must be > 1.0. See the table in the class documentation
     *                                   for finding reasonable values for this constant.
     */
    public ToblersHikingFunction(double walkDistMultiplierMaxLimit) {
        if(walkDistMultiplierMaxLimit < 1.0) {
            throw new IllegalArgumentException("The 'walkDistMultiplierMaxLimit' is " + walkDistMultiplierMaxLimit +
                    ", but must be greater then 1.");
        }
        this.walkDistMultiplierMaxLimit = walkDistMultiplierMaxLimit;
    }

    /**
     * Calculate a walking distance multiplier to account tor the slope penalty.
     * @param dx The horizontal walking distance
     * @param dh The vertical distance (height)
     */
    public double calculateHorizontalWalkingDistanceMultiplier(double dx, double dh) {

        double  distanceMultiplier = 1.0 / (C * Math.exp(E * Math.abs(dh/dx + A)));

        return distanceMultiplier < walkDistMultiplierMaxLimit ? distanceMultiplier : walkDistMultiplierMaxLimit;
    }
}
