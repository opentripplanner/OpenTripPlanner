package org.opentripplanner.graph_builder.impl.stopsAlerts;

import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Ben
 * Date: 02/01/13
 * Time: 17:32
 * To change this template use File | Settings | File Templates.
 */
public class UnconnectedStop extends AbstractStopTester {

    @Override
    public boolean fulfillDemands(TransitStop ts, Graph graph) {
        List<Edge> outgoingStreets = ts.getOutgoingStreetEdges();
        boolean hasStreetLink = false;
        for(Edge e:ts.getIncoming()){
            if(e instanceof StreetTransitLink){
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
