package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

class TransfersMapper {

    /**
     * Copy pre-calculated transfers from the original graph
     */
    static List<List<Transfer>> mapTransfers(
            Map<Stop, TransitStop> stopVertexForStop,
            List<Stop> stopsByIndex,
            Map<Stop, Integer> indexByStop
    ) {

        List<List<Transfer>> transferByStopIndex = new ArrayList<>();

        for (int stopIndex = 0; stopIndex < stopsByIndex.size(); ++stopIndex) {
            Stop stop = stopsByIndex.get(stopIndex);
            ArrayList<Transfer> list = new ArrayList<>();
            transferByStopIndex.add(list);

            for (Edge edge : stopVertexForStop.get(stop).getOutgoing()) {
                if (edge instanceof SimpleTransfer) {
                    double distance = edge.getDistance();

                    int toStopIndex = indexByStop.get(((TransitStop) edge.getToVertex()).getStop());
                    Transfer transfer = new Transfer(toStopIndex, (int) distance,
                            Arrays.asList(edge.getGeometry().getCoordinates()));

                    list.add(transfer);
                }
            }
        }
        return transferByStopIndex;
    }
}
