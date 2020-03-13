package org.opentripplanner.updater.vehicle_sharing;

import org.opentripplanner.routing.core.vehicle_sharing.CarDescription;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;

import java.util.List;

import static java.util.Collections.singletonList;

public class VehiclePositionsGetter {

    VehiclePositionsDiff getVehiclePositionsDiff() {
        float vehLong1 = (float) 17.982160800000003;
        float vehLat1 = (float) 53.1152128;
        List<VehicleDescription> appeared = singletonList(new CarDescription(vehLong1, vehLat1));
        return new VehiclePositionsDiff(appeared, 0L, 0L);
    }
}
