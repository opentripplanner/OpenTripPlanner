package org.opentripplanner.ext.vectortiles.layers.vehicleparkings;

import java.util.Set;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingGroup;

/**
 * Record that holds {@link VehicleParkingGroup} and a set of {@link VehicleParking} that belong to
 * it.
 */
public record VehicleParkingAndGroup(
  VehicleParkingGroup vehicleParkingGroup,
  Set<VehicleParking> vehicleParking
) {}
