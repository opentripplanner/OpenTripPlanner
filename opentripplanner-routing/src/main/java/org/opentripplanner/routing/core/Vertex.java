/* This program is free software: you can redistribute it and/or
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
import java.util.Vector;

import com.vividsolutions.jts.geom.Coordinate;

public interface Vertex extends Serializable {

    public String getLabel();

    public double distance(Vertex v);

    public double distance(Coordinate c);

    public Coordinate getCoordinate();

    public int getDegreeOut();

    public int getDegreeIn();

    public void addIncoming(Edge ee);

    public void addOutgoing(Edge ee);

    public String toString();

    public void setX(double x);

    public double getX();

    public void setY(double y);

    public double getY();

    public void setOutgoing(Vector<Edge> outgoing);

    public Iterable<Edge> getOutgoing();

    public void setIncoming(Vector<Edge> incoming);

    public Iterable<Edge> getIncoming();

    public Class<?> getType();

    public void setType(Class<?> type);

    public String getName();

}