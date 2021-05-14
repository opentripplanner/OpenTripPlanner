package org.opentripplanner.model.plan;

import lombok.Builder;
import lombok.Getter;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingEntrance;

@Getter
@Builder
public class VehicleParkingWithEntrance {
    private final VehicleParking vehicleParking;
    private final VehicleParkingEntrance entrance;
}
