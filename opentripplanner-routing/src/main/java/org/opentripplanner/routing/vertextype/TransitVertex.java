package org.opentripplanner.routing.vertextype;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.graph.AbstractVertex;

/** Abstract base class for vertices in the GTFS layer of the graph. */
public abstract class TransitVertex extends AbstractVertex {
    
    private static final long serialVersionUID = 53855622892837370L;

    private final AgencyAndId stopId;
    
    public TransitVertex(String label, double x, double y, AgencyAndId stopId) {
        super(label, x, y);
        this.stopId = stopId;
    }

    /** Get the stop at which this TransitVertex is located */
    public AgencyAndId getStopId() {
        return stopId;
    }
    
    public String getName() {
        return stopId.toString();
    }

}
