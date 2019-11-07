package org.opentripplanner.routing.algorithm.raptor.router.street;

import com.conveyal.r5.otp2.api.transit.TransferLeg;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.request.TransferWithDuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TransferToAccessEgressLegMapper {
    private TransitLayer transitLayer;

    public TransferToAccessEgressLegMapper(TransitLayer transitLayer) {
        this.transitLayer = transitLayer;
    }

    public Collection<TransferLeg> map(Map<Stop, Transfer> input, double walkSpeed) {
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
