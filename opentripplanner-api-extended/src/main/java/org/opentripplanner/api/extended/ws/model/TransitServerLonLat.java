package org.opentripplanner.api.extended.ws.model;

public class TransitServerLonLat {
    
    private double lon;
    private double lat;

    public TransitServerLonLat() {
    }
    
    public TransitServerLonLat(double lon, double lat) {
        this.setLon(lon);
        this.setLat(lat);
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getLon() {
        return lon;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLat() {
        return lat;
    }
}
