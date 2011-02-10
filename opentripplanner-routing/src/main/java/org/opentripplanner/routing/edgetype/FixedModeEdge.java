package org.opentripplanner.routing.edgetype;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Geometry;

public class FixedModeEdge implements EdgeNarrative {
    EdgeNarrative base;
    TraverseMode mode;

    public FixedModeEdge(EdgeNarrative base, TraverseMode mode) {
	this.base = base;
	this.mode = mode;
    }

    @Override
    public double getDistance() {
        return base.getDistance();
    }

    @Override
    public Geometry getGeometry() {
        return base.getGeometry();
    }

    @Override
    public TraverseMode getMode() {
        return mode;
    }

    @Override
    public String getName() {
        return base.getName();
    }

    @Override
    public Trip getTrip() {
        return base.getTrip();
    }

    public Vertex getFromVertex() {
        return base.getFromVertex();
    }

    public Vertex getToVertex() {
        return base.getToVertex();
    }

    @Override
    public String getName(State state) {
        return base.getName(state);
    }
    
    public String toString() {
        return "FixedModeEdge(" + base + ", " + mode + ")";
    }

    @Override
    public boolean isRoundabout() {
        return base.isRoundabout();
    }
}