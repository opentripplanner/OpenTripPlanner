package org.opentripplanner.routing.vertextype;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.graph.Graph;

/* Note that this is not a subclass of TransitStop, to avoid it being linked to the street network */
public class TransitStopDepart extends OffboardVertex {

    private static final long serialVersionUID = 5353034364687763358L;
    private TransitStop stopVertex;

    public TransitStopDepart(Graph graph, Stop stop, TransitStop stopVertex) {
        super(graph, GtfsLibrary.convertIdToString(stop.getId()) + "_depart", stop);
        this.stopVertex = stopVertex;
    }

    public TransitStop getStopVertex() {
        return stopVertex;
    }

    @Override
    public FeedScopedId getStopId() {
        return stopVertex.getStopId();
    }
}
