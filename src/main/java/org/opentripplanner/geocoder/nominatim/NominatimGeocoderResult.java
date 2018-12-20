package org.opentripplanner.geocoder.nominatim;

public class NominatimGeocoderResult {
    private String lat;
    private String lon;
    private String display_name;
    private String osm_type;
    
    public void setLat(String lat) {
        this.lat = lat;
    }
    
    public String getLat() {
        return lat;
    }
    
    public void setLon(String lon) {
        this.lon = lon;
    }

    public String getLon() {
        return lon;
    }
    
    public double getLatDouble() {
        return Double.parseDouble(lat);
    }
    
    public double getLngDouble() {
        return Double.parseDouble(lon);
    }
    
    public void setDisplay_name(String displayName) {
        this.display_name = displayName;
    }
    
    public String getDisplay_name() {
        return display_name;
    }
    
    public void setOsm_type(String osm_type) {
        this.osm_type = osm_type;
    }

    public String getOsm_type() {
        return osm_type;
    }
}