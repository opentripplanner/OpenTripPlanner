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

package org.opentripplanner.routing.contraction;

import java.io.Serializable;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.algorithm.NegativeWeightException;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class Shortcut implements Edge, Serializable {
    private static final long serialVersionUID = -5813252201367498850L;
    
    Vertex startVertex, endVertex;
    
    Edge edge1;
    Edge edge2;
    int time;
    double weight = -1;
   
    public Shortcut(Edge edge1, Edge edge2, int time, double weight) {
        startVertex = edge1.getFromVertex();
        endVertex = edge2.getToVertex();
        this.edge1 = edge1;
        this.edge2 = edge2;
        this.time = time;
        this.weight = weight;
    }

    @Override
    public String getDirection() {
        return null;
    }

    @Override
    public double getDistance() {
        return 0;
    }

    @Override
    public Geometry getGeometry() {
        GeometryFactory gf = new GeometryFactory();
        return gf.createLineString(new Coordinate[] { getFromVertex().getCoordinate(), getToVertex().getCoordinate() });
    }

    @Override
    public TraverseMode getMode() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Trip getTrip() {
        return null;
    }

    @Override
    public TraverseResult traverse(State s0, TraverseOptions wo) throws NegativeWeightException {
        if (weight != -1) {
            State s1 = s0.clone();
            s1.incrementTimeInSeconds(time);
            return new TraverseResult(weight, s1);
        }
        State state = s0;
        long startTime = state.getTime();
        TraverseResult wr = edge1.traverse(state, wo);
        if (wr == null) {
            return null;
        }
        state = wr.state;
        double aweight = wr.weight;
        wr = edge2.traverse(state, wo);
        if (wr == null) {
            return null;
        }
        state = wr.state;
        weight = aweight + wr.weight;
        
        time = (int) ((state.getTime() - startTime) / 1000);
        return new TraverseResult(weight, state);
    }

    @Override
    public TraverseResult traverseBack(State s0, TraverseOptions wo) throws NegativeWeightException {
        if (weight != -1) {
            State s1 = s0.clone();
            s1.incrementTimeInSeconds(-time);
            return new TraverseResult(weight, s1);
        }
        State state = s0;
        long startTime = state.getTime();

        TraverseResult wr = edge2.traverseBack(state, wo);
        if (wr == null) {
            return null;
        }
        state = wr.state;
        double bweight = wr.weight;

        wr = edge1.traverseBack(state, wo);
        if (wr == null) {
            return null;
        }
        state = wr.state;
        weight = bweight + wr.weight;
        
        
        time = (int) ((startTime - state.getTime()) / 1000);
        return new TraverseResult(weight, state);
    }
    
    public String toString() {
        return "Shortcut(" + edge1 + "," + edge2 + ")";
    }

    @Override
    public Vertex getFromVertex() {
        return startVertex;
    }

    @Override
    public Vertex getToVertex() {
        return endVertex;
    }

    @Override
    public void setFromVertex(Vertex vertex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setToVertex(Vertex vertex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName(State state) {
        return getName();
    }
}
