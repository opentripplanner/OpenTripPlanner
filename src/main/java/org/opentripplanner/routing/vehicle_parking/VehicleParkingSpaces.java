package org.opentripplanner.routing.vehicle_parking;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * The number of spaces by type. {@code null} if unknown.
 */
@Data
@Builder
public class VehicleParkingSpaces implements Serializable {

    private final Integer bicycleSpaces;

    private final Integer carSpaces;

    private final Integer wheelchairAccessibleCarSpaces;
}
