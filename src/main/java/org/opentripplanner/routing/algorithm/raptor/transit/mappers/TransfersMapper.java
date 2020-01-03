package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.transit.StopIndexForRaptor;
import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class TransfersMapper {

    /**
     * Copy pre-calculated transfers from the original graph
     */
    static List<List<Transfer>> mapTransfers(
            Map<Stop, TransitStopVertex> stopVertexForStop,
            StopIndexForRaptor stopIndex
    ) {

        List<List<Transfer>> transferByStopIndex = new ArrayList<>();

        for (int i = 0; i < stopIndex.stopsByIndex.size(); ++i) {
            Stop stop = stopIndex.stopsByIndex.get(i);
            ArrayList<Transfer> list = new ArrayList<>();
            transferByStopIndex.add(list);

            for (Edge edge : stopVertexForStop.get(stop).getOutgoing()) {
                if (edge instanceof SimpleTransfer) {
                    double effectiveDistance = edge.getEffectiveWalkDistance();

                    int toStopIndex = stopIndex.indexByStop.get(((TransitStopVertex) edge.getToVertex()).getStop());
                    Transfer transfer = new Transfer(toStopIndex, (int) effectiveDistance,
                            ((SimpleTransfer) edge).getEdges());

                    list.add(transfer);
                }
            }
        }
        return transferByStopIndex;
    }
}
