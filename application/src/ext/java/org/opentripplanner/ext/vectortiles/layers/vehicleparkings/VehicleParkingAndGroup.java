package org.opentripplanner.ext.vectortiles.layers.vehicleparkings;

import java.util.Collection;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingGroup;

/**
 * Record that holds {@link VehicleParkingGroup} and a set of {@link VehicleParking} that belong to
 * it.
 */
public record VehicleParkingAndGroup(
  VehicleParkingGroup vehicleParkingGroup,
  Collection<VehicleParking> vehicleParking
) {}
