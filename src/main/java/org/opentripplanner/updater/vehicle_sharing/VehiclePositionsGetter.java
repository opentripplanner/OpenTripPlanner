package org.opentripplanner.updater.vehicle_sharing;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;

import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.singletonList;

public class VehiclePositionsGetter {
    long lastUpdate;
    TraverseMode MODE;

    public VehiclePositionsGetter(TraverseMode MODE) {
        this.MODE = MODE;
        lastUpdate = -1;
    }

    VehiclePositionsDiff getVehiclePositionsDiff() {
        float vehLong1 = (float) 17.982160800000003;
        float vehLat1 = (float) 53.1152128;
        List<VehicleDescription> appeared = new LinkedList<VehicleDescription>();
        appeared.add(new VehicleDescription(TraverseMode.CAR, vehLong1,vehLat1));
        appeared.add(new VehicleDescription(TraverseMode.CAR, vehLat1,vehLong1));

        return new VehiclePositionsDiff(appeared, 0L, 0L);

    }
}
