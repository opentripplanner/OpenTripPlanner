package org.opentripplanner.routing.vertextype;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.graph.Graph;

public class TransitStopArrive extends OffboardVertex {

    private static final long serialVersionUID = 9213431651426739857L;

    public TransitStopArrive(Graph g, String label, double x, double y, AgencyAndId stopId) {
        super(g, label, x, y, stopId);
    }

}
