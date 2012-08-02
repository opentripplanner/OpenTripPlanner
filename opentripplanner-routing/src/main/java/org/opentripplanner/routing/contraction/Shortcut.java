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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.AbstractEdge;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.patch.Patch;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

public class Shortcut extends AbstractEdge {
    private static final long serialVersionUID = -5813252201367498850L;
    
    Edge edge1;
    Edge edge2;
    int time;
    double weight = -1;
    private TraverseMode mode;
    private double walkDistance;

    public Shortcut(Edge edge1, Edge edge2, int time, double weight, double walkDistance, TraverseMode mode) {
        super(edge1.getFromVertex(), edge2.getToVertex());
        this.edge1 = edge1;
        this.edge2 = edge2;
        this.time = time;
        this.weight = weight;
        this.walkDistance = walkDistance;
        this.mode = mode;
    }

    @Override
    public double getDistance() {
        return 0;
    }

    @Override
    public LineString getGeometry() {
        return GeometryUtils.getGeometryFactory().createLineString(new Coordinate[] { getFromVertex().getCoordinate(), getToVertex().getCoordinate() });
    }

    @Override
    public TraverseMode getMode() {
        return mode;
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
    public State traverse(State s0) {
        if (weight == -1) {
        	Edge first, second;
        	if (s0.getOptions().isArriveBy()) {
        		first = edge2;
        		second = edge1;
        	} else {
        		first = edge1;
        		second = edge2;
        	}
            State s1 = first.traverse(s0);
            if (s1 == null)
                return null;
            State s2 = second.traverse(s1);
            if (s2 == null)
                return null;
            time = (int) Math.abs(s0.getTime() - s2.getTime());
            weight = s2.getWeight() - s0.getWeight();
            mode = s2.getBackEdgeNarrative().getMode();
            walkDistance = s2.getWalkDistance() - s0.getWalkDistance();
        }
        //StateEditor ret = s0.edit(this, (EdgeNarrative) new FixedModeEdge(this, mode));
        StateEditor ret = s0.edit(this);
        ret.incrementTimeInSeconds(time);
        ret.incrementWeight(weight);
        ret.incrementWalkDistance(walkDistance);
        return ret.makeState();
    }
    
    public String toString() {
        return "Shortcut(" + edge1 + "," + edge2 + ")";
    }

    @Override
    public boolean isRoundabout() {
        return false;
    }

	@Override
	public State optimisticTraverse(State s0) {
		return traverse(s0);
	}

    public Set<Alert> getNotes() {
    	return null;
    }
    
	@Override
	public void addPatch(Patch patch) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Patch> getPatches() {
        return Collections.emptyList();
	}

	@Override
	public void removePatch(Patch patch) {
		throw new UnsupportedOperationException();		
	}

	/*
	 * recursive shortcut unpack
	 */
	public State unpackTraverse(State s0) {
		Edge first, second;
		if (s0.getOptions().isArriveBy()) {
			first  = edge2;
			second = edge1;
		} else {
			first  = edge1;
			second = edge2;
		}
		State s1;
		if (first instanceof Shortcut) 
			s1 = ((Shortcut)first).unpackTraverse(s0);
		else
			s1 = first.traverse(s0);

		// traversals might fail during unpacking, even if they didn't during search
		if (s1 == null)
			return null;

		if (second instanceof Shortcut) 
			s1 = ((Shortcut)second).unpackTraverse(s1);
		else
			s1 = second.traverse(s1);

		return s1;
	}

	@Override
	public boolean hasBogusName() {
		return false;
	}

	@Override
	public double timeLowerBound(RoutingRequest options) {
		return weight == -1 ? 0 : weight;
	}

    @Override
    public double weightLowerBound(RoutingRequest options) {
        return timeLowerBound(options);
    }
}
