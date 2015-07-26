package org.opentripplanner.streets.structs;

import org.nustaq.offheap.structs.FSTStruct;

/**
 *
 */
public class StreetIntersection extends FSTStruct {

    public static final double FIXED_FACTOR = 1e7; // we could just reuse the constant from osm-lib Node.
    public int fixedLat;
    public int fixedLon;

    public void setLat(double lat) {
        fixedLat = (int)(lat * FIXED_FACTOR);
    }

    public void setLon(double lon) {
        fixedLon = (int)(lon * FIXED_FACTOR);
    }

    public void setLatLon(double lat, double lon) {
        setLat(lat);
        setLon(lon);
    }

    public double getLat() {
        return fixedLat / FIXED_FACTOR;
    }

    public double getLon() {
        return fixedLon / FIXED_FACTOR;
    }

}
