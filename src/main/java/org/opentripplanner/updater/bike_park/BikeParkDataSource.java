package org.opentripplanner.updater.bike_park;

import org.opentripplanner.routing.bike_park.BikePark;

import java.util.List;

/**
 * A (static or dynamic) source of bike-parks.
 * 
 * Bike park-and-ride and "OV-fiets mode" development has been funded by GoAbout
 * (https://goabout.com/).
 * 
 * @author laurent
 * @author GoAbout
 */
public interface BikeParkDataSource {

    /**
     * Update the data from the source;
     * returns true if there might have been changes
     */
    boolean update();
    
    List<BikePark> getBikeParks();
    
}
