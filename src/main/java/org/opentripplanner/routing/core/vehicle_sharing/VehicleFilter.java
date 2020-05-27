package org.opentripplanner.routing.core.vehicle_sharing;

@FunctionalInterface
public interface VehicleFilter {

    boolean isValid(VehicleDescription vehicle);
}
