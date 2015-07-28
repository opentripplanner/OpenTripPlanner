package org.opentripplanner.streets;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.model.Stop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


/**
 *
 */
public class TransitLayer {

    private static final Logger LOG = LoggerFactory.getLogger(TransitLayer.class);

    public List<Stop> stops = new ArrayList<>();

    public void loadFromGtfs (GTFSFeed gtfs) {
        for (Stop stop : gtfs.stops.values()) {
            stops.add(stop);
        }
    }

    public static TransitLayer fromGtfs (String file) {
        GTFSFeed gtfs = GTFSFeed.fromFile(file);
        TransitLayer transitLayer = new TransitLayer();
        transitLayer.loadFromGtfs(gtfs);
        return transitLayer;
    }

}
