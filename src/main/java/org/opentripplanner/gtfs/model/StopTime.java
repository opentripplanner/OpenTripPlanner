package org.opentripplanner.gtfs.model;

import org.mapdb.Fun;

/**
 * Ordering of stoptimes will be provided by the keys, which are a tuple of (string, int).
 */
public class StopTime extends GtfsEntity {

    private static final long serialVersionUID = 1L;
    
    @Required public String trip_id;
    @Required public String arrival_time;
    @Required public String departure_time;
    @Required public String stop_id;
    @Required public String stop_sequence;
    public String stop_headsign;
    public String pickup_type;
    public String drop_off_type;
    public String shape_dist_traveled;

    @Override
    public Object getKey() {
        return Fun.t2(trip_id, Integer.parseInt(stop_sequence));
    }

}