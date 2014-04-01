package org.opentripplanner.gtfs.model;


public class Stop extends GtfsEntity {

    private static final long serialVersionUID = 1L;

    @Required public String stop_id;
    @Required public String stop_name;
    @Required public String stop_lat;
    @Required public String stop_lon;
    public String stop_code;
    public String stop_desc;
    public String zone_id;
    public String stop_url;
    public String location_type;
    public String parent_station;
    public String stop_timezone;
    public String wheelchair_boarding;

    @Override
    public String getKey() {
        return stop_id;
    }

}