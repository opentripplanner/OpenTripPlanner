package org.opentripplanner.gtfs.model;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.zip.ZipFile;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;
import org.mapdb.Fun.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.collect.Lists;

import static org.opentripplanner.common.LoggingUtil.human;

/**
 * All entities must be from a single feed namespace.
 * Composed of several GTFSTables.
 */
public class GTFSFeed {

    private static final Logger LOG = LoggerFactory.getLogger(GTFSFeed.class);

    DB db = DBMaker.newTempFileDB()
            .transactionDisable()
            .asyncWriteEnable()
            .compressionEnable()
            .make(); // db.close();

    String feedId;
    public final Map<String, Agency> agency = Maps.newHashMap();
    public final Map<String, Route>  routes = Maps.newHashMap();
    public final Map<String, Stop>   stops  = Maps.newHashMap();
    public final Map<String, Trip>   trips  = Maps.newHashMap();
    // Map from 2-tuples of (trip_id, stop_sequence) to stoptimes.
    public final ConcurrentNavigableMap<Tuple2, StopTime> stop_times = db.getTreeMap("stop_times");
        
    private void loadFromFile(ZipFile zip) throws Exception {
        new GTFSTable("agency",     Agency.class,   false).loadTable(zip, agency);
        new GTFSTable("routes",     Route.class,    false).loadTable(zip, routes);
        new GTFSTable("stops",      Stop.class,     false).loadTable(zip, stops );
        new GTFSTable("trips",      Trip.class,     false).loadTable(zip, trips );
        new GTFSTable("stop_times", StopTime.class, false).loadTable(zip, stop_times);
    }
    
    public static GTFSFeed fromFile(String file) {
        GTFSFeed feed = new GTFSFeed();
        ZipFile zip;
        try {
            zip = new ZipFile(file);
            feed.loadFromFile(zip);
            zip.close();
            return feed;
        } catch (Exception e) {
            LOG.error("Error loading GTFS: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // Bin all trips by the sequence of stops they visit.
    public void findPatterns() {
        // A map from a list of stop IDs (the pattern) to a list of trip IDs which fit that pattern.
        Map<List<String>, List<String>> tripsForPattern = Maps.newHashMap();
        int n = 0;
        for (String trip_id : trips.keySet()) {
            if (++n % 100000 == 0) {
                LOG.info("trip {}", human(n));
            }
            Map<Fun.Tuple2, StopTime> tripStopTimes =
                stop_times.subMap(
                    Fun.t2(trip_id, null),
                    Fun.t2(trip_id, Fun.HI)
                );
            List<String> stops = Lists.newArrayList();
            // In-order traversal of StopTimes within this trip. The 2-tuple keys determine ordering.
            for (StopTime stopTime : tripStopTimes.values()) {
                stops.add(stopTime.stop_id);
            }
            // Fetch or create the tripId list for this stop pattern, then add the current trip to that list.
            List<String> trips = tripsForPattern.get(stops);
            if (trips == null) {
                trips = Lists.newArrayList();
                tripsForPattern.put(stops, trips);
            }
            trips.add(trip_id);
        }
        LOG.info("Total patterns: {}", tripsForPattern.keySet().size());
    }

}
