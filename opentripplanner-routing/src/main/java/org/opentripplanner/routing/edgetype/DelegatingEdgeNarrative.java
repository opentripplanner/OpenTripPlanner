package org.opentripplanner.routing.edgetype;

import java.util.Set;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Geometry;

public abstract class DelegatingEdgeNarrative implements EdgeNarrative {
    protected EdgeNarrative base;

    public DelegatingEdgeNarrative(EdgeNarrative base) {
	this.base = base;
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
        return base.getMode();
    }

    @Override
    public String getName() {
        return base.getName();
    }
    
    public Set<String> getNotes() {
    	return base.getNotes();
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
    public boolean isRoundabout() {
        return base.isRoundabout();
    }

    public boolean hasBogusName() {
        return base.hasBogusName();
    }
}