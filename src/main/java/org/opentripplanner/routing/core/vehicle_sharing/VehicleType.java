package org.opentripplanner.routing.core.vehicle_sharing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum VehicleType {

    CAR,
    MOTORBIKE,
    KICKSCOOTER;

    private static final String _CAR = "car";
    private static final String _MOTORBIKE = "scooter";
    private static final String _KICKSCOOTER = "un-pedal-scooter";

    private static final Logger LOG = LoggerFactory.getLogger(VehicleType.class);

    // Cannot be named `fromString` as it would become default constructor
    // for `@QueryParam("vehicleTypesAllowed")` in `RoutingResource.java`
    public static VehicleType fromDatabaseVehicleType(String vehicleType) {
        if (vehicleType == null) {
            LOG.warn("Cannot create vehicle type enum from null");
            return null;
        }
        switch (vehicleType) {
            case _CAR:
                return VehicleType.CAR;
            case _MOTORBIKE:
                return VehicleType.MOTORBIKE;
            case _KICKSCOOTER:
                return VehicleType.KICKSCOOTER;
            default:
                LOG.warn("Cannot create vehicle type enum - unknown vehicle type {}", vehicleType);
                return null;
        }
    }
}
