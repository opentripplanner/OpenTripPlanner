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

package org.opentripplanner.routing.impl;

import java.util.Vector;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Coordinate;

public final class DummyReferenceVertex implements Vertex {

    private static final long serialVersionUID = 1L;

    private final String vertexId;

    public DummyReferenceVertex(String vertexId) {
        this.vertexId = vertexId;
    }

    @Override
    public String getLabel() {
        return this.vertexId;
    }
    
    @Override
    public String getName() {
        return this.vertexId;
    }

    @Override
    public String getStopId() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void addIncoming(Edge ee) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addOutgoing(Edge ee) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double distance(Vertex v) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double distance(Coordinate c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Coordinate getCoordinate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getDegreeIn() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getDegreeOut() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Edge> getIncoming() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Edge> getOutgoing() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<?> getType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getX() {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getY() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setIncoming(Vector<Edge> incoming) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOutgoing(Vector<Edge> outgoing) {
        throw new UnsupportedOperationException();
    }
}
