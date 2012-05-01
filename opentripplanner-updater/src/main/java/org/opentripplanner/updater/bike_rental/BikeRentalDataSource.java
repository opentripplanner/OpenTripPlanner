package org.opentripplanner.updater.bike_rental;

import java.util.List;

public interface BikeRentalDataSource {

    /** Update the data from the source;
     * returns true if there might have been changes */
    boolean update();
    
    List<BikeRentalStation> getStations();
    
}
