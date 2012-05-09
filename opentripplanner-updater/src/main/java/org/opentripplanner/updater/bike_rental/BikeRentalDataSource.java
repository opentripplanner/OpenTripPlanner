package org.opentripplanner.updater.bike_rental;

import java.util.List;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;

public interface BikeRentalDataSource {

    /** Update the data from the source;
     * returns true if there might have been changes */
    boolean update();
    
    List<BikeRentalStation> getStations();
    
}
