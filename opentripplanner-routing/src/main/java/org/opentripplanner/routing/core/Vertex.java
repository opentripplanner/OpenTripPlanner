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

package org.opentripplanner.routing.core;

import java.io.Serializable;

import com.vividsolutions.jts.geom.Coordinate;

public interface Vertex extends Serializable, Cloneable {

    /**
     * Every vertex has a label which is globally unique
     */
    public String getLabel();

    /**
     * For vertices that represent stops, the passenger-facing stop ID (for systems like TriMet that
     * have this feature). 
     */
    public String getStopId();

    /** This is a fast but totally inaccurate distance function used as a heuristic in A* */
    public double fastDistance(Vertex v);

    /**
     * This is a correct distance function used during non-time-critical functions.
     */
    public double distance(Coordinate c);

    /**
     * @return The location of the vertex in longitude/latitude
     */
    public Coordinate getCoordinate();

    public String toString();

    public double getX();

    public double getY();

    public String getName();

}