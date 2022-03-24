package org.opentripplanner.updater.vehicle_rental.datasources;

import org.entur.gbfs.v2_2.vehicle_types.GBFSVehicleType;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType;

public class GbfsVehicleTypeMapper {

    private final String systemId;

    public GbfsVehicleTypeMapper(String systemId) {
        this.systemId = systemId;
    }

    public RentalVehicleType mapRentalVehicleType(GBFSVehicleType vehicleType) {
        return new RentalVehicleType(
                new FeedScopedId(systemId, vehicleType.getVehicleTypeId()),
                vehicleType.getName(),
                RentalVehicleType.FormFactor.fromGbfs(vehicleType.getFormFactor()),
                RentalVehicleType.PropulsionType.fromGbfs(vehicleType.getPropulsionType()),
                vehicleType.getMaxRangeMeters()
        );
    }
}
