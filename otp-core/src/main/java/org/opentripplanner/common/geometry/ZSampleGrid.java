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

import org.opentripplanner.common.geometry.ZSampleGrid.ZSamplePoint;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * A generic indexed grid of TZ samples. TZ could be anything but is usually a vector of parameters.
 * 
 * We assume some sort of equirectangular project between the index coordinates (x,y) and the
 * geographic coordinates (lat, lon). The projection factor (cos phi, standard parallel) is given as
 * a cell size in lat,lon degrees (dLat,dLon)). The conversion is given by the following formulae:
 * 
 * <code>
 * lon = lon0 + x.dLon;
 * lat = lat0 + y.dLat;
 * </code> (lat0,lon0) is the center, (dLat,dLon) is the cell size.
 * 
 * @author laurent
 */
public interface ZSampleGrid<TZ> extends Iterable<ZSamplePoint<TZ>> {

    public interface ZSamplePoint<TZ> {
        /**
         * @return The X index of this sample point.
         */
        public int getX();

        /**
         * @return The Y index of this sample point.
         */
        public int getY();

        /**
         * @return The Z value associated with this sample point.
         */
        public TZ getZ();

        public void setZ(TZ z);

        /**
         * @return The neighboring sample point located at (x,y-1)
         */
        public ZSamplePoint<TZ> up();

        /**
         * @return The neighboring sample point located at (x,y+1)
         */
        public ZSamplePoint<TZ> down();

        /**
         * @return The neighboring sample point located at (x+1,y)
         */
        public ZSamplePoint<TZ> right();

        /**
         * @return The neighboring sample point located at (x-1,y)
         */
        public ZSamplePoint<TZ> left();

    }

    /**
     * @param x
     * @param y
     * @return The sample point located at (x,y). Create a new one if not existing.
     */
    public ZSamplePoint<TZ> getOrCreate(int x, int y);

    /**
     * @param point The sample point
     * @return The (lat,lon) coordinates of this sample point.
     */
    public Coordinate getCoordinates(ZSamplePoint<TZ> point);

    /**
     * @param C The geographical coordinate
     * @return The (x,y) index of the lower-left index of the cell enclosing the point.
     */
    public int[] getLowerLeftIndex(Coordinate C);

    /**
     * @return The base coordinate center (lat0,lon0)
     */
    public Coordinate getCenter();

    /**
     * @return The cell size (dLat,dLon)
     */
    public Coordinate getCellSize();

    public int getXMin();

    public int getXMax();

    public int getYMin();

    public int getYMax();

    public int size();

    /**
     * TODO The mapping between a ZSampleGrid and a DelaunayTriangulation should not be part of an
     * interface but extracted to a converter. This assume that the conversion process does not rely
     * on the inner working of the ZSampleGrid implementation, which should be the case.
     * 
     * @return This ZSampleGrid converted as a DelaunayTriangulation.
     */
    public DelaunayTriangulation<TZ> delaunayTriangulate();
}