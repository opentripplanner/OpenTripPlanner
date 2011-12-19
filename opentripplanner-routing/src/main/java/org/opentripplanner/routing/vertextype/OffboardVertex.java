package org.opentripplanner.routing.vertextype;

import org.onebusaway.gtfs.model.AgencyAndId;

public abstract class OffboardVertex extends TransitVertex {

    private static final long serialVersionUID = 1L;

    public OffboardVertex(String label, double x, double y, AgencyAndId stopId) {
        super(label, x, y, stopId);
    }

}
