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

import com.vividsolutions.jts.geom.Coordinate;

interface DelaunayPoint<TZ> {

    public Coordinate getCoordinates();

    public TZ getZ();
}

interface DelaunayEdge<TZ> {

    public DelaunayPoint<TZ> getA();

    public DelaunayPoint<TZ> getB();

    /**
     * @param ccw true (CCW) for A->B left edge, false (CW) for right edge.
     * @return The edge starting at B, going right or left.
     */
    public DelaunayEdge<TZ> getEdge1(boolean ccw);

    /**
     * @param ccw For same value of ccw, will return the same side as getEdge1().
     * @return The edge starting at A, going right or left.
     */
    public DelaunayEdge<TZ> getEdge2(boolean ccw);

    public boolean isProcessed();

    public void setProcessed(boolean processed);
}

/**
 * A Delaunay triangulation (adapted to isoline building).
 * 
 * @author laurent
 */
public interface DelaunayTriangulation<TZ> {

    public int edgesCount();
    
    public Iterable<? extends DelaunayEdge<TZ>> edges();

}