package org.opentripplanner.routing.edgetype.flex;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitStopDepart;

/**
 * Created by dbenoff on 2/10/17.
 */
public class TemporaryTransitBoardAlight extends TransitBoardAlight implements TemporaryEdge {

    public TemporaryTransitBoardAlight(TransitStopDepart fromStopVertex, PatternStopVertex toPatternVertex,
                                       int stopIndex, TraverseMode mode) {
        super(fromStopVertex, toPatternVertex, stopIndex, mode);
    }

    public TemporaryTransitBoardAlight(PatternStopVertex fromPatternStop, TransitStopArrive toStationVertex,
                                       int stopIndex, TraverseMode mode) {
        super(fromPatternStop, toStationVertex, stopIndex, mode);
    }

    @Override
    public void dispose() {
        fromv.removeOutgoing(this);
        tov.removeIncoming(this);
    }
}
