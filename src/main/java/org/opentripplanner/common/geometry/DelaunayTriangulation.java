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

/**
 * A DelaunayPoint is the geometrical point of a node of the triangulation.
 * 
 * @author laurent
 * @param <TZ>
 */
interface DelaunayPoint<TZ> {

    /**
     * @return The geometric location of this point.
     */
    public Coordinate getCoordinates();

    /**
     * @return The Z value for this point.
     */
    public TZ getZ();
}

/**
 * A DelaunayEdge is a directed segment between two DelaunayPoints of the triangulation.
 * 
 * The interface is kept minimal for isoline building purposes.
 * 
 * @author laurent
 * @param <TZ>
 */
interface DelaunayEdge<TZ> {

    /**
     * @return The start point (node) of this edge.
     */
    public DelaunayPoint<TZ> getA();

    /**
     * @return The end point (node) of this edge.
     */
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

    /**
     * HACK. This should not be here really. But with Java, attaching some user value to an object
     * rely on another level of indirection and costly maps/arrays. Exposing this flag directly here
     * saves *lots* of processing time. TODO Is there a better way to do that?
     * 
     * @return The flag set by setProcessed.
     */
    public boolean isProcessed();

    /**
     * @param processed
     */
    public void setProcessed(boolean processed);
}

/**
 * A Delaunay triangulation (adapted to isoline building).
 * 
 * A simple interface returning a collection (an iterable) of DelaunayEdges. The interface is kept
 * minimal for isoline building purposes.
 * 
 * @author laurent
 * @param TZ The value stored for each node.
 */
public interface DelaunayTriangulation<TZ> {

    public int edgesCount();

    public Iterable<? extends DelaunayEdge<TZ>> edges();

}