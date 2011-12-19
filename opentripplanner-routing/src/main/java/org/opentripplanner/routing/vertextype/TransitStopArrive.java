package org.opentripplanner.routing.vertextype;

import org.onebusaway.gtfs.model.AgencyAndId;

public class TransitStopArrive extends OffboardVertex {

    private static final long serialVersionUID = 9213431651426739857L;

    public TransitStopArrive(String label, double x, double y, AgencyAndId stopId) {
        super(label, x, y, stopId);
    }

}
