package org.opentripplanner.routing.vertextype;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.graph.Graph;

public class TransitStopArrive extends OffboardVertex {

    private static final long serialVersionUID = 9213431651426739857L;
    private TransitStop stopVertex;

    public TransitStopArrive(Graph g, Stop stop, TransitStop stopVertex) {
        super(g, GtfsLibrary.convertIdToString(stop.getId()) + "_arrive", stop);
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
