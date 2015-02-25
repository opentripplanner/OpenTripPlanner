/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.edgetype.factory;

import java.util.HashMap;
import java.util.Map;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.core.StopTransfer;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.edgetype.TransferEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStationStop;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

/** Link graph based on transfers.txt.  Intended for testing */
@Deprecated
public class TransferGraphLinker {

    private Graph graph;

    public TransferGraphLinker(Graph graph) {
        this.graph = graph;
    }
    
    public void run() {
        // Create a mapping from StopId to StopVertices
        Map<AgencyAndId, TransitStationStop> stopNodes = new HashMap<AgencyAndId, TransitStationStop>();
        for (Vertex v : graph.getVertices()) {
            if (v instanceof TransitStationStop) {
                TransitStationStop transitStationStop = (TransitStationStop) v;
                Stop stop = transitStationStop.getStop();
                stopNodes.put(stop.getId(), transitStationStop);
            }
        } 
        
        // Create edges
        for (TransferTable.Transfer transfer : graph.getTransferTable().getAllFirstSpecificTransfers()) {
            TransitStationStop fromVertex = stopNodes.get(transfer.fromStopId);
            TransitStationStop toVertex = stopNodes.get(transfer.toStopId);

            double distance = SphericalDistanceLibrary.distance(fromVertex.getCoordinate(),
                    toVertex.getCoordinate());
            TransferEdge edge = null;
            switch (transfer.seconds) {
                case StopTransfer.FORBIDDEN_TRANSFER:
                case StopTransfer.UNKNOWN_TRANSFER:
                    break;
                case StopTransfer.PREFERRED_TRANSFER:
                case StopTransfer.TIMED_TRANSFER:
                    edge = new TransferEdge(fromVertex,
                            toVertex, distance);
                    break;
                default:
                    edge = new TransferEdge(fromVertex,
                            toVertex, distance, transfer.seconds);
            }
            
            if (edge != null) {
                LineString geometry = GeometryUtils.getGeometryFactory().createLineString(new Coordinate[] {
                        fromVertex.getCoordinate(),
                        toVertex.getCoordinate() });
                edge.setGeometry(geometry);
            }
        }
    }

}
