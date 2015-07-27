package org.opentripplanner.transit;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.model.Stop;

import java.util.HashMap;
import java.util.Map;


/**
 *
 */
public class TransitLayer {

    Map<String, Stop> stops = new HashMap<>();

    public void loadFromGtfs (GTFSFeed gtfs) {
        stops.putAll(gtfs.stops);
    }

    public static TransitLayer fromGtfs (String file) {
        GTFSFeed gtfs = GTFSFeed.fromFile(file);
        TransitLayer transitLayer = new TransitLayer();
        transitLayer.loadFromGtfs(gtfs);
        return transitLayer;
    }

}
