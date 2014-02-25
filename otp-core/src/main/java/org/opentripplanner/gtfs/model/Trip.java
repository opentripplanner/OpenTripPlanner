package org.opentripplanner.gtfs.model;


public class Trip extends GtfsEntity {

    private static final long serialVersionUID = 1L;

    @Required public String trip_id;
    @Required public String route_id;
    @Required public String service_id;
    public String trip_headsign;
    public String trip_short_name;
    public String direction_id;
    public String block_id;
    public String shape_id;
    public String bikes_allowed;
    public String wheelchair_accessible;

    @Override
    public String getKey() {
        return trip_id;
    }

}