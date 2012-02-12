package org.opentripplanner.routing.vertextype;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.graph.AbstractVertex;
import org.opentripplanner.routing.graph.Graph;

/** Abstract base class for vertices in the GTFS layer of the graph. */
public abstract class TransitVertex extends AbstractVertex {
    
    private static final long serialVersionUID = 53855622892837370L;

    private final AgencyAndId stopId;
    
    private final String stopCode;

    public TransitVertex(Graph graph, String label, Stop stop) {
        super(graph, label, stop.getLon(), stop.getLat(), stop.getName());
        this.stopId = stop.getId();
        this.stopCode = stop.getCode();
    }

    /** Get the stop at which this TransitVertex is located */
    public AgencyAndId getStopId() {
        return stopId;
    }
    
    /** The passenger-facing stop ID/Code (for systems like TriMet that have this feature).  */
    public String getStopCode() {
        return stopCode;
    }

}
