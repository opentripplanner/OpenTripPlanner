package org.opentripplanner.api.thrift.util;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.opentripplanner.api.thrift.definition.TravelMode;
import org.opentripplanner.routing.core.TraverseMode;

public class TravelModeWrapperTest {

    @Test
    public void testToTraverseMode() {
        Map<TravelMode, TraverseMode> modeMap = new HashMap<TravelMode, TraverseMode>();
        modeMap.put(TravelMode.BICYCLE, TraverseMode.BICYCLE);
        modeMap.put(TravelMode.CAR, TraverseMode.CAR);
        modeMap.put(TravelMode.CUSTOM_MOTOR_VEHICLE, TraverseMode.CUSTOM_MOTOR_VEHICLE);
        modeMap.put(TravelMode.WALK, TraverseMode.WALK);
        modeMap.put(TravelMode.ANY_TRAIN, TraverseMode.TRAINISH);
        modeMap.put(TravelMode.ANY_TRANSIT, TraverseMode.TRANSIT);

        for (TravelMode travelMode : modeMap.keySet()) {
            TraverseMode expectedTraverseMode = modeMap.get(travelMode);
            TraverseMode actualTraverseMode = (new TravelModeWrapper(travelMode)).toTraverseMode();
            assertEquals(expectedTraverseMode, actualTraverseMode);
        }
    }
}
