package org.opentripplanner.api.extended.ws.model;

import org.onebusaway.gtfs.model.Stop;

public class TransitServerStop {
    private String id;
    private String name;
    private double lon;
    private double lat;

    public TransitServerStop() {
    }
    
    public TransitServerStop(Stop stop) {
        this.id = stop.getId().toString();
        this.setName(stop.getName());
        this.setLon(stop.getLon());
        this.setLat(stop.getLat());
    }
    
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    public double getLon() {
        return lon;
    }
    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }
    public void setLat(double lat) {
        this.lat = lat;
    }
}
