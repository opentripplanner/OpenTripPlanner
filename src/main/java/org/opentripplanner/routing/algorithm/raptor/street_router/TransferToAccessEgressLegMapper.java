package org.opentripplanner.routing.algorithm.raptor.street_router;

import com.conveyal.r5.profile.otp2.api.transit.TransferLeg;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.transit_data_provider.TransferWithDuration;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.TransitLayer;

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
            int duration = (int)Math.floor(transfer.getDistanceMeters() / walkSpeed);
            int stopIndex = transitLayer.getIndexByStop(stop);
            Transfer newTransfer = new Transfer(stopIndex, transfer.getDistanceMeters(), transfer.getCoordinates());
            result.add(new TransferWithDuration(newTransfer, duration));
        }
        return result;
    }
}
