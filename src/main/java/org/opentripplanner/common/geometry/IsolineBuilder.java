/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.common.geometry;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Generic interface for a class that compute an isoline out of a TZ 2D "field".
 * 
 * @author laurent
 */
public interface IsolineBuilder<TZ> {

    /**
     * A ZMetric is a metric for some generic TZ value.
     * 
     * By metric here we understand:
     * <ul>
     * <li>Cutting detection on a range, z0 in [Za, Zb] (rely on TZ to be an ordered set)</li>
     * <li>Interpolation on a range, z0 in [Za, Zb].</li>
     * </ul>
     * Cutting detection could be easily implemented using interpolation, but usually interpolating
     * is rather more expansive than cutting detection so we split the two operations.
     * 
     * @author laurent
     */
    public interface ZMetric<TZ> {
        /**
         * Check if the edge [AB] between two samples A and B "intersect" the zz0 plane.
         * 
         * @param zA z value for the A sample
         * @param zB z value for the B sample
         * @param z0 z value for the intersecting plane
         * @return 0 if no intersection, -1 or +1 if intersection (depending on which is lower, A or
         *         B).
         */
        public int cut(TZ zA, TZ zB, TZ z0);

        /**
         * Interpolate a crossing point on an edge [AB].
         * 
         * @param zA z value for the A sample
         * @param zB z value for the B sample
         * @param z0 z value for the intersecting plane
         * @return k value between 0 and 1, where the crossing occurs. 0=A, 1=B.
         */
        public double interpolate(TZ zA, TZ zB, TZ z0);
    }

    public Geometry computeIsoline(TZ z0);

}
