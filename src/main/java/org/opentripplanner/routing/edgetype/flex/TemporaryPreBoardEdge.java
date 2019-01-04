package org.opentripplanner.routing.edgetype.flex;

import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.StationEdge;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopDepart;

/**
 * PreBoard edges connect a TransitStop to its agency_stop_depart vertices; PreAlight edges connect
 * an agency_stop_arrive vertex to its TransitStop.
 * 
 * Applies the local stop rules (see TransitStop.java and LocalStopFinder.java) as well as transfer
 * limits, timed and preferred transfer rules, transfer penalties, and boarding costs. This avoids
 * applying these costs/rules repeatedly in (Pattern)Board edges. These are single station or
 * station-to-station specific costs, rather than trip-pattern specific costs.
 */
public class TemporaryPreBoardEdge extends PreBoardEdge implements StationEdge, TemporaryEdge {

    public TemporaryPreBoardEdge(TransitStop from, TransitStopDepart to) {
        super(from, to);
    }

}
