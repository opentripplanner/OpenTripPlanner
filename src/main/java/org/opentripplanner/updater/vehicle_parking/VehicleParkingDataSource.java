package org.opentripplanner.updater.vehicle_parking;

import org.opentripplanner.routing.vehicle_parking.VehicleParking;

import java.util.List;

/**
 * A (static or dynamic) source of vehicle-parkings.
 *
 */
public interface VehicleParkingDataSource {

    /**
     * Update the data from the source;
     * returns true if there might have been changes
     */
    boolean update();
    
    List<VehicleParking> getVehicleParkings();
    
}
