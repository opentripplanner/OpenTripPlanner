package org.opentripplanner.routing.vertextype.flex;

import com.google.common.collect.Iterables;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TemporaryVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopDepart;

public class TemporaryTransitStopDepart extends TransitStopDepart implements TemporaryVertex {
    public TemporaryTransitStopDepart(Graph graph, Stop stop, TransitStop transitStop) {
        super(graph, stop, transitStop);
    }

    @Override
    public boolean isEndVertex() {
        return false;
    }

    @Override
    public void dispose() {
        for (Object temp : Iterables.concat(getIncoming(), getOutgoing())) {
            ((TemporaryEdge) temp).dispose();
        }
    }
}
