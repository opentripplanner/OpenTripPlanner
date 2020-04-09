package org.opentripplanner.routing.core.vehicle_sharing;

import java.util.Locale;

public enum FuelType {

    FOSSIL,
    HYBRID,
    ELECTRIC;

    public static FuelType fromString(String fuelType) {
        if (fuelType == null) {
            return null;
        }
        try {
            return FuelType.valueOf(fuelType.toUpperCase(Locale.US));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
