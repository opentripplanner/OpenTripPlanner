package org.opentripplanner.transit;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.model.Stop;
import org.opentripplanner.streets.StreetLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


/**
 *
 */
public class TransitLayer {

    private static final Logger LOG = LoggerFactory.getLogger(TransitLayer.class);

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

    public void findStreets(StreetLayer streetLayer) {
        for (Stop stop : stops.values()) {
            LOG.info("Streets near {}.", stop.stop_name);
            streetLayer.findNearbyIntersections(stop.stop_lat, stop.stop_lon, 300);
        }
    }

}
