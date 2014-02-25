package org.opentripplanner.gtfs.model;

import org.mapdb.Fun;


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
        return Fun.t2(trip_id, stop_sequence);
    }

    @Override
    public String getFilename() {
        return "stop_times.txt"; // not like the others
    }

}