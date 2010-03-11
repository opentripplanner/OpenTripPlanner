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

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Coordinate;

public class DeadEnd extends OneStreetVertex implements StreetIntersectionVertex {

    private static final long serialVersionUID = 8659709448092487563L;

    double x, y;

    public DeadEnd(IntersectionVertex v) {
        x = v.getX();
        y = v.getY();
        inStreet = v.inStreet;
        outStreet = v.outStreet;
        if(inStreet != null)
            inStreet.setToVertex(this);
        if(outStreet != null)
            outStreet.setFromVertex(this);
    }

    @Override
    public void addIncoming(Edge ee) {
       throw new UnsupportedOperationException("Incoming and outgoing edges are only inStreet and outStreet");
    }

    @Override
    public void addOutgoing(Edge ee) {
        throw new UnsupportedOperationException("Incoming and outgoing edges are only inStreet and outStreet");
    }

    @Override
    public double distance(Vertex v) {

        double xd = v.getX() - x;
        double yd = v.getY() - y;
        return Math.sqrt(xd * xd + yd * yd) * GenericVertex.METERS_PER_DEGREE_AT_EQUATOR * GenericVertex.COS_MAX_LAT;
    }

    @Override
    public double distance(Coordinate c) {

        double xd = c.x - x;
        double yd = c.y - y;
        return Math.sqrt(xd * xd + yd * yd) * GenericVertex.METERS_PER_DEGREE_AT_EQUATOR * GenericVertex.COS_MAX_LAT;
    }

    @Override
    public Coordinate getCoordinate() {
        return new Coordinate(x, y);
    }

    @Override
    public int getDegreeIn() {
        return inStreet != null ? 1 : 0;
    }

    @Override
    public int getDegreeOut() {
        return outStreet != null ? 1 : 0;
    }

    @Override
    public Iterable<Edge> getIncoming() {
        ArrayList<Edge> edges = new ArrayList<Edge>();
        edges.add(inStreet);
        return edges;
    }

    @Override
    public String getLabel() {
        return "DeadEnd(" + x + "," + y + ")";
    }

    @Override
    public String getName() {
        return getLabel();
    }

    @Override
    public Iterable<Edge> getOutgoing() {
        ArrayList<Edge> edges = new ArrayList<Edge>();
        edges.add(outStreet);
        return edges;
    }

    @Override
    public String getStopId() {
        return null;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }
}
