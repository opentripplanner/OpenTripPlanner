package org.opentripplanner.routing.vertextype;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.util.NonLocalizedString;

/** Abstract base class for vertices in the GTFS layer of the graph. */
public abstract class TransitVertex extends Vertex {

    private static final long serialVersionUID = 53855622892837370L;

    private final Stop stop;

    public TransitVertex(Graph graph, String label, Stop stop) {
        super(graph, label, stop.getLon(), stop.getLat(), new NonLocalizedString(stop.getName()));
        this.stop = stop;
    }

    /** Get the stop at which this TransitVertex is located */
    public FeedScopedId getStopId() {
        return stop.getId();
    }

    /** The passenger-facing stop ID/Code (for systems like TriMet that have this feature). */
    public String getStopCode() {
        return stop.getCode();
    }

    /** The passenger-facing code/name indentifying the platform/quay */
    public String getPlatformCode() {
        return stop.getPlatformCode();
    }

    /** Stop information need by API */
    public Stop getStop() {
        return stop;
    }

}
