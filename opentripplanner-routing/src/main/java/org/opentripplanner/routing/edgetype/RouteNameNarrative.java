package org.opentripplanner.routing.edgetype;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Geometry;

public class RouteNameNarrative implements EdgeNarrative {

    private Trip trip;
    private EdgeNarrative base;

    public RouteNameNarrative(Trip trip, EdgeNarrative edge) {
        this.trip = trip;
        this.base = edge;
    }

    @Override
    public double getDistance() {
        return base.getDistance();
    }

    @Override
    public Vertex getFromVertex() {
        return base.getFromVertex();
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
        return GtfsLibrary.getRouteName(trip.getRoute());
    }

    @Override
    public Vertex getToVertex() {
        return base.getToVertex();
    }

    @Override
    public Trip getTrip() {
        return trip;
    }

    @Override
    public boolean isRoundabout() {
        return base.isRoundabout();
    }

}
