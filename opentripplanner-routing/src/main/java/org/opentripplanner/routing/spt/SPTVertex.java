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

package org.opentripplanner.routing.spt;

import java.io.Serializable;
import java.util.ArrayList;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.State;

import com.vividsolutions.jts.geom.Coordinate;

/** A vertex in the ShortestPathTree */

public class SPTVertex implements Vertex, Serializable {
    
    private static final long serialVersionUID = -4422788581123655293L;

    public SPTEdge incoming;

    public Vertex mirror;

    public State state;
    
    public TraverseOptions options;

    /** Total cost to this vertex */
    public double weightSum;

    public int hops;

    public SPTVertex(Vertex mirror, State state, double weightSum, TraverseOptions options) {
        this(mirror, state, weightSum, options, 0);
    }
    
    public SPTVertex(Vertex mirror, State state, double weightSum, TraverseOptions options, int hops) {
        this.mirror = mirror;
        this.state = state;
        this.weightSum = weightSum;
        this.options = options;
        this.hops = hops;
    }

    public SPTEdge setParent(SPTVertex parent, Edge ep, EdgeNarrative edgeNarrative) {
        incoming = new SPTEdge(parent, this, ep, edgeNarrative);
        return incoming;
    }
    
    public SPTEdge getParent() {
        return incoming;
    }

    public String toString() {
        return this.mirror + " (" + this.weightSum + ")";
    }

    public String getName() {
        return this.mirror.getName();
    }

    public AgencyAndId getStopId() {
        return this.mirror.getStopId();
    }

    public boolean equals(SPTVertex v) {
        return v.mirror == mirror && v.incoming == incoming;
    }

    @Override
    public double distance(Coordinate c) {
        return mirror.distance(c);
    }

    @Override
    public double distance(Vertex v) {
        return mirror.distance(v);
    }

    @Override
    public Coordinate getCoordinate() {
        return mirror.getCoordinate();
    }

    public Iterable<SPTEdge> getIncoming() {
        ArrayList<SPTEdge> ret = new ArrayList<SPTEdge>(1);
        ret.add(incoming);
        return ret;
    }

    @Override
    public String getLabel() {
        return mirror.getLabel();
    }

    @Override
    public double getX() {
        return mirror.getX();
    }

    @Override
    public double getY() {
        return mirror.getY();
    }

    public int hashCode() {
        return mirror.hashCode();
    }

    @Override
    public double getDistanceToNearestTransitStop() {
        return mirror.getDistanceToNearestTransitStop();
    }

    @Override
    public void setDistanceToNearestTransitStop(double distance) {
        throw new UnsupportedOperationException();
    }
}