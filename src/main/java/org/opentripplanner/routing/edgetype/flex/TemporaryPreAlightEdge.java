package org.opentripplanner.routing.edgetype.flex;

import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.edgetype.StationEdge;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopArrive;


public class TemporaryPreAlightEdge extends PreAlightEdge implements StationEdge, TemporaryEdge {

    public TemporaryPreAlightEdge(TransitStopArrive from, TransitStop to) {
        super(from, to);
    }

}