package org.opentripplanner.routing.core.vehicle_sharing;

import java.util.ArrayList;
import java.util.List;

public class VehicleValidator {

    private final List<VehicleFilter> filters = new ArrayList<>();


    public void addFilter(VehicleFilter filter) {
        filters.add(filter);
    }

    public boolean isValid(VehicleDescription vehicle) {
        return filters.stream().allMatch(f -> f.isValid(vehicle));
    }
}
