package org.opentripplanner.routing.vertextype;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.graph.Graph;

public abstract class OffboardVertex extends TransitVertex {

    private static final long serialVersionUID = 1L;

    public OffboardVertex(Graph g, String label, double x, double y, AgencyAndId stopId) {
        super(g, label, x, y, stopId);
    }

}
