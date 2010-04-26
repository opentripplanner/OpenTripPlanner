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

public interface Vertex extends Serializable {

    public String getLabel();

    public String getStopId();

    /** This is fast but totally inaccurate distance function used as a heuristic in A* */
    public double fastDistance(Vertex v);

    public double distance(Coordinate c);

    public Coordinate getCoordinate();

    public int getDegreeOut();

    public int getDegreeIn();

    public void addIncoming(Edge ee);

    public void addOutgoing(Edge ee);

    public String toString();

    public double getX();

    public double getY();

    public Iterable<Edge> getOutgoing();

    public Iterable<Edge> getIncoming();

    public String getName();

}