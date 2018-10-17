package org.opentripplanner.updater.bike_rental;

import java.util.List;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;

/**
 * TODO clarify thread safety.
 * It appears that update() and getStations() are never called simultaneously by different threads, but is not stated.
 */
public interface BikeRentalDataSource {

    /**
     * Fetch current data about bike rental stations and availability from this source.
     * @return true if this operation may have changed something in the list of stations.
     */
    boolean update();

    /**
     * @return a List of all currently known bike rental stations. The updater will use this to update the Graph.
     */
    List<BikeRentalStation> getStations();
    
}
