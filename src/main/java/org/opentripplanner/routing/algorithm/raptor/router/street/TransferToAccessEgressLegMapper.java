package org.opentripplanner.routing.algorithm.raptor.router.street;

import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.request.TransferWithDuration;
import org.opentripplanner.transit.raptor.api.transit.TransferLeg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TransferToAccessEgressLegMapper {
    private final TransitLayer transitLayer;
    private final double walkSpeed;

    public TransferToAccessEgressLegMapper(TransitLayer transitLayer, double walkSpeed) {
        this.transitLayer = transitLayer;
        this.walkSpeed = walkSpeed;
    }

    public Collection<TransferLeg> map(Map<Stop, Transfer> input) {
        List<TransferLeg> result = new ArrayList<>();
        for (Map.Entry<Stop, Transfer> entry : input.entrySet()) {
            Stop stop = entry.getKey();
            Transfer transfer = entry.getValue();
            int stopIndex = transitLayer.getIndexByStop(stop);
            Transfer newTransfer = new Transfer(
                    stopIndex,
                    transfer.getEffectiveWalkDistanceMeters(),
                    transfer.getEdges()
            );
            result.add(new TransferWithDuration(newTransfer, walkSpeed));
        }
        return result;
    }
}
