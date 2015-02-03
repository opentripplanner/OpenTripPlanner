/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

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
