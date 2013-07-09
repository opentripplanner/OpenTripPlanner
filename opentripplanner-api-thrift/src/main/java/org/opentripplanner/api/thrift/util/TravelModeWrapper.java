package org.opentripplanner.api.thrift.util;

import lombok.AllArgsConstructor;
import lombok.Data;

import org.opentripplanner.api.thrift.definition.TravelMode;
import org.opentripplanner.routing.core.TraverseMode;

@Data
@AllArgsConstructor
public class TravelModeWrapper {

    private TravelMode travelMode;

    /**
     * Convert to a TraverseMode (internal representation).
     * 
     * @return TraverseMode value
     */
    public TraverseMode toTraverseMode() {
        switch (travelMode) {
        case BICYCLE:
            return TraverseMode.BICYCLE;
        case WALK:
            return TraverseMode.WALK;
        case CAR:
            return TraverseMode.CAR;
        case CUSTOM_MOTOR_VEHICLE:
            return TraverseMode.CUSTOM_MOTOR_VEHICLE;
        case TRAM:
            return TraverseMode.TRAM;
        case SUBWAY:
            return TraverseMode.SUBWAY;
        case RAIL:
            return TraverseMode.RAIL;
        case ANY_TRAIN:
            return TraverseMode.TRAINISH;
        case ANY_TRANSIT:
            return TraverseMode.TRANSIT;
        default:
            return null;
        }
    }
}
