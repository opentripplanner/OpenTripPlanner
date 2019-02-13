package org.opentripplanner.analyst.request;

import java.util.GregorianCalendar;

public class SPTRequest {

    public final double lon; 
    public final double lat;
    public final long time;

    public SPTRequest(double lon, double lat, GregorianCalendar gcal) {
        this.lon = lon;
        this.lat = lat;
        if (gcal != null)
            this.time = gcal.getTimeInMillis() / 1000;
        else 
            this.time = System.currentTimeMillis() / 1000;
    }

    public SPTRequest(double lon, double lat, long time) {
        this.lon = lon;
        this.lat = lat;
        this.time = time;
    }
    
    public int hashCode() {
        return (int)(lon * 42677 + lat * 1307 + time);
    }
    
    public boolean equals(Object other) {
        if (other instanceof SPTRequest) {
            SPTRequest that = (SPTRequest) other;
            return this.lon  == that.lon &&
                   this.lat  == that.lat &&
                   this.time == that.time;
        }
        return false;
    }

    public String toString() {
        return String.format("<SPT request, lon=%f lat=%f time=%d>", lon, lat, time);
    }
}
