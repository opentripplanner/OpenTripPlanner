package org.opentripplanner.routing.car_rental;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public enum CarFuelType {
    @JsonProperty("gasoline")
    GASOLINE,
    @JsonProperty("electric")
    ELECTRIC,
    @JsonProperty("unknown")
    UNKNOWN;

    @JsonCreator
    public static CarFuelType forValue(String value) {
        switch (value.toLowerCase()) {
            case "gasoline":
                return GASOLINE;
            case "electric":
                return ELECTRIC;
            default:
                return UNKNOWN;
        }
    }
}
