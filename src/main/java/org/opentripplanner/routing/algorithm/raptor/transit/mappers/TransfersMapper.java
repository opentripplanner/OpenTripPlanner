package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.routing.algorithm.raptor.transit.StopIndexForRaptor;
import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;

class TransfersMapper {

    /**
     * Copy pre-calculated transfers from the original graph
     */
    static List<List<Transfer>> mapTransfers(
        StopIndexForRaptor stopIndex,
        Multimap<StopLocation, PathTransfer> transfersByStop
    ) {

        List<List<Transfer>> transferByStopIndex = new ArrayList<>();

        for (int i = 0; i < stopIndex.stopsByIndex.size(); ++i) {
            var stop = stopIndex.stopsByIndex.get(i);
            ArrayList<Transfer> list = new ArrayList<>();
            transferByStopIndex.add(list);

            for (PathTransfer pathTransfer : transfersByStop.get(stop)) {
                if (pathTransfer.to instanceof Stop) {
                    int toStopIndex = stopIndex.indexByStop.get(pathTransfer.to);
                    Transfer newTransfer;
                    if (pathTransfer.getEdges() != null) {
                        newTransfer = new Transfer(
                            toStopIndex,
                            pathTransfer.getEdges()
                        );
                    }
                    else {
                        newTransfer = new Transfer(
                            toStopIndex,
                            (int) Math.ceil(pathTransfer.getDistanceMeters())
                        );
                    }

                    list.add(newTransfer);
                }
            }
        }
        return transferByStopIndex;
    }
}
