package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import com.google.common.collect.Multimap;
import org.opentripplanner.model.SimpleTransfer;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.transit.StopIndexForRaptor;
import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;

import java.util.ArrayList;
import java.util.List;

class TransfersMapper {

    /**
     * Copy pre-calculated transfers from the original graph
     */
    static List<List<Transfer>> mapTransfers(
        StopIndexForRaptor stopIndex,
        Multimap<Stop, SimpleTransfer> transfersByStop
    ) {

        List<List<Transfer>> transferByStopIndex = new ArrayList<>();

        for (int i = 0; i < stopIndex.stopsByIndex.size(); ++i) {
            Stop stop = stopIndex.stopsByIndex.get(i);
            ArrayList<Transfer> list = new ArrayList<>();
            transferByStopIndex.add(list);

            for (SimpleTransfer simpleTransfer : transfersByStop.get(stop)) {
                double effectiveDistance = simpleTransfer.getEffectiveWalkDistance();
                int toStopIndex = stopIndex.indexByStop.get(simpleTransfer.to);
                Transfer transfer = new Transfer(toStopIndex, (int) effectiveDistance,
                    simpleTransfer.getEdges());

                list.add(transfer);
            }
        }
        return transferByStopIndex;
    }
}
