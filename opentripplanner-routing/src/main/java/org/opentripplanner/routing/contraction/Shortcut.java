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
import java.util.List;
import java.util.Set;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.core.DirectEdge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.patch.Patch;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class Shortcut implements DirectEdge, Serializable {
    private static final long serialVersionUID = -5813252201367498850L;
    
    Vertex startVertex, endVertex;
    
    DirectEdge edge1;
    DirectEdge edge2;
    int time;
    double weight = -1;
    private TraverseMode mode;
   
    public Shortcut(DirectEdge edge1, DirectEdge edge2, int time, double weight, TraverseMode mode) {
        this.startVertex = edge1.getFromVertex();
        this.endVertex = edge2.getToVertex();
        this.edge1 = edge1;
        this.edge2 = edge2;
        this.time = time;
        this.weight = weight;
        this.mode = mode;
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
            State s1 = edge1.traverse(s0);
            if (s1 == null)
                return null;
            State s2 = edge2.traverse(s1);
            if (s2 == null)
                return null;
            time = (int) ((s2.getTime() - s0.getTime()) / 1000);
            weight = s2.getWeight() - s0.getWeight();
            mode = s2.getBackEdgeNarrative().getMode();
    	}
        //StateEditor ret = s0.edit(this, (EdgeNarrative) new FixedModeEdge(this, mode));
        StateEditor ret = s0.edit(this);
        ret.incrementTimeInSeconds(time);
        ret.incrementWeight(weight);
        return ret.makeState();
    }

    @Override
    public State traverseBack(State s0) {
    	if (weight == -1) {
            State s1 = edge2.traverseBack(s0);
            if (s1 == null)
                return null;
            State s2 = edge1.traverseBack(s1);
            if (s2 == null)
                return null;
            time = (int) ((s0.getTime() - s2.getTime()) / 1000);
            weight = s2.getWeight() - s0.getWeight();
            mode = s2.getBackEdgeNarrative().getMode();
    	}
        //StateEditor ret = s0.edit(this, (EdgeNarrative) new FixedModeEdge(this, mode));
        StateEditor ret = s0.edit(this);
        ret.incrementTimeInSeconds(-time);
        ret.incrementWeight(weight);
        return ret.makeState();
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
    public boolean isRoundabout() {
        return false;
    }



	@Override
	public State optimisticTraverse(State s0) {
		return traverse(s0);
	}

    public Set<String> getNotes() {
    	return null;
    }
    
	@Override
	public void addPatch(Patch patch) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Patch> getPatches() {
		return null;
	}

	@Override
	public void removePatch(Patch patch) {
		throw new UnsupportedOperationException();		
	}

	/*
	 * recursive shortcut unpack
	 */
	public State unpackTraverse(State s0) {
		State s1;
		if (edge1 instanceof Shortcut) 
			s1 = ((Shortcut)edge1).unpackTraverse(s0);
		else
			s1 = edge1.traverse(s0);

		if (edge2 instanceof Shortcut) 
			s1 = ((Shortcut)edge2).unpackTraverse(s1);
		else
			s1 = edge2.traverse(s1);
		
		return s1;
	}
}
