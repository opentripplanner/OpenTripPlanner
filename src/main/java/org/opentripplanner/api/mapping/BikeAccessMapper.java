package org.opentripplanner.api.mapping;

import org.opentripplanner.model.BikeAccess;

public class BikeAccessMapper {
    public static int mapToApi(BikeAccess bikeAccess) {
        switch (bikeAccess) {
            case ALLOWED:
                return 1;
            case NOT_ALLOWED:
                return 2;
            default:
                return 0;
        }
    }
}
