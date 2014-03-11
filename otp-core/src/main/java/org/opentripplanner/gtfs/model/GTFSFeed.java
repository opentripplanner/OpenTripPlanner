package org.opentripplanner.gtfs.model;

import java.util.Map;
import java.util.zip.ZipFile;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

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
    public final Map<Tuple2<String, String>, Stop> stop_times = 
            db.getTreeMap("stop_times");
        
    private void loadFromFile(ZipFile zip) throws Exception {
        new GTFSTable("agency", Agency.class, false).loadTable(zip, agency);
        new GTFSTable("routes", Route.class,  false).loadTable(zip, routes);
        new GTFSTable("stops",  Stop.class,   false).loadTable(zip, stops );
        new GTFSTable("trips",  Trip.class,   false).loadTable(zip, trips );
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

}
