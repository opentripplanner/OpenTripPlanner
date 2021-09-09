package org.opentripplanner.updater.vehicle_rental;

import java.util.List;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;

/**
 * TODO clarify thread safety.
 * It appears that update() and getStations() are never called simultaneously by different threads, but is not stated.
 */
public interface VehicleRentalDataSource {

    /**
     * Fetch current data about vehicle rental stations and availability from this source.
     * @return true if this operation may have changed something in the list of stations.
     */
    boolean update();

    /**
     * @return a List of all currently known vehicle rental stations. The updater will use this to update the Graph.
     */
    List<VehicleRentalStation> getStations();

    /**
     * @see org.opentripplanner.updater.GraphUpdater#setup
     */
    default void setup() {}
}
