package org.opentripplanner.routing.algorithm.raptor.street_router;

import com.conveyal.r5.otp2.api.transit.TransferLeg;
import org.opentripplanner.model.Stop;
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
            int duration = (int)Math.floor(transfer.getDistance() / walkSpeed); //TODO: Avoid hard coding walk speed
            int stopIndex = transitLayer.getIndexByStop(stop);
            // TODO - Calculate som meaningful cost
            result.add(new R5TransferLeg(stopIndex, duration));
        }
        return result;
    }

    // TODO name R5TransferLeg
    private static class R5TransferLeg implements TransferLeg {
        private int stop;
        private int durationInSeconds;

        private R5TransferLeg(int stop, int durationInSeconds) {
            this.stop = stop;
            this.durationInSeconds = durationInSeconds;
        }

        @Override public int stop() {
            return stop;
        }

        @Override public int durationInSeconds() {
            return durationInSeconds;
        }
    }
}
