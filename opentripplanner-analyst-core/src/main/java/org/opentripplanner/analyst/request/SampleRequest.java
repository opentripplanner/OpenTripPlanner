package org.opentripplanner.analyst.request;

/**
 * Used in the SampleCache. Bins sample requests by rounded latitude and longitude.
 * This prevents cache misses due to projection and floating point slop. Pixel bins 
 * will become visible at higher zoom levels, but initial rendering of tiles at those 
 * levels will be much faster and use less memory. 
 */
public class SampleRequest {

    public final int lon; 
    public final int lat;

    /* rounding to 4 decimal places gives house-sized pixel bins. */
    public SampleRequest(double lon, double lat) {
        this.lon = (int) (lon * 10000);
        this.lat = (int) (lat * 10000);
    }
    
    //  90 degrees to 4 decimal digits = 900000.
    //  900000 << 12 = 3686400000 -- fits into a 32 bit int with wraparound. 
    public int hashCode() {
        return ((lat << 10) ^ lon);
    }
    
    public boolean equals(Object other) {
        if (other instanceof SampleRequest) {
            SampleRequest that = (SampleRequest) other;
            return this.lon  == that.lon &&
                    this.lat  == that.lat ;
        }
        return false;
    }

    public String toString() {
        return String.format("<Sample request (integer binned), lon=%d lat=%d>", lon, lat);
    }

}
