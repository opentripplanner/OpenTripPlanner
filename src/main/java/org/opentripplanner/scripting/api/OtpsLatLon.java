package org.opentripplanner.scripting.api;

/**
 * Simple geographical coordinates.
 * 
 * @author laurent
 */
public class OtpsLatLon {

    private double lat, lon;

    protected OtpsLatLon(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    /**
     * @return The latitude.
     */
    public double getLat() {
        return lat;
    }

    /**
     * @return The longitude.
     */
    public double getLon() {
        return lon;
    }

    @Override
    public String toString() {
        return "(" + lat + "," + lon + ")";
    }

}
