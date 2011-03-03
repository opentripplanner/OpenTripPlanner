package org.opentripplanner.routing.edgetype.factory;

import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.edgetype.TransferEdge;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/** Link graph based on transfers.txt.  Intended for testing */
public class TransferGraphLinker {

    private Graph graph;

    public TransferGraphLinker(Graph graph) {
        this.graph = graph;
    }
    
    public void run() {
        GeometryFactory factory = new GeometryFactory();
        for (TransferTable.Transfer transfer : graph.getTransferTable().getAllTransfers()) {
 
            double distance = transfer.from.distance(transfer.to.getCoordinate());
            TransferEdge edge = null;
            switch (transfer.seconds) {
                case TransferTable.FORBIDDEN_TRANSFER:
                    break;
                case TransferTable.PREFERRED_TRANSFER:
                case TransferTable.TIMED_TRANSFER:
                    edge = new TransferEdge(transfer.from,
                            transfer.to, distance);
                    break;
                default:
                    edge = new TransferEdge(transfer.from,
                            transfer.to, distance, transfer.seconds);
            }
            
            if (edge != null) {
                LineString geometry = factory.createLineString(new Coordinate[] {
                        transfer.from.getCoordinate(),
                        transfer.to.getCoordinate() });
                edge.setGeometry(geometry);
                graph.addEdge(edge);
            }
        }
    }

}
