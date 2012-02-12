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

import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.edgetype.TransferEdge;
import org.opentripplanner.routing.graph.Graph;

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
            }
        }
    }

}
