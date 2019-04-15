package org.opentripplanner.routing.edgetype.factory;

import java.util.HashMap;
import java.util.Map;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.core.StopTransfer;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.edgetype.TransferEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStationStop;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

/** Link graph based on transfers.txt.  Intended for testing */
@Deprecated
public class TransferGraphLinker {

    private Graph graph;

    public TransferGraphLinker(Graph graph) {
        this.graph = graph;
    }
    
    public void run() {
        // Create a mapping from StopId to StopVertices
        Map<FeedScopedId, TransitStationStop> stopNodes = new HashMap<FeedScopedId, TransitStationStop>();
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
