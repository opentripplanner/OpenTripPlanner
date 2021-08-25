package org.opentripplanner.updater.bike_rental;

import java.util.List;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.bike_rental.GeofencingZone;

public interface BikeRentalGeofencingDataSource {

    /**
     * Fetch current data about bike rental stations and availability from this source.
     * @return true if this operation may have changed something in the list of stations.
     */
    boolean update();

    /**
     * @return a List of all currently known bike rental geofencing zones. The updater will use this to update the Graph.
     */
    List<GeofencingZone> getZones();
    
}
