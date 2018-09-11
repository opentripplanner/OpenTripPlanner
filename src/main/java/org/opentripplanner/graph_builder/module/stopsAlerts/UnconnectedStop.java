package org.opentripplanner.graph_builder.module.stopsAlerts;

import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.util.List;

public class UnconnectedStop extends AbstractStopTester {


    /**
     * @retrun return true if the stop is not connected to any street
     */
    @Override
    public boolean fulfillDemands(TransitStop ts, Graph graph) {
        List<Edge> outgoingStreets = ts.getOutgoingStreetEdges();
        boolean hasStreetLink = false;
        for(Edge e:ts.getIncoming()){
            if(e instanceof StreetTransitLink || e instanceof PathwayEdge){
                hasStreetLink = true;
                break;
            }
        }
        if(!hasStreetLink){
            //TODO: see what if there is incoming and not outgoing
            for(Edge e:ts.getOutgoing()){
                if(e instanceof StreetTransitLink){
                    hasStreetLink = true;
                    break;
                }
            }
        }
        return !(hasStreetLink || (outgoingStreets.size() > 0));
    }
}
