package org.opentripplanner.osm;

import java.io.Serializable;

public class Node extends Tagged implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final double FIXED_PRECISION_FACTOR = 1e6;

    public Node () { }

    public Node (double lat, double lon) {
        setLatLon(lat, lon);
    }

    /* Angles are stored as fixed precision 32 bit integers because 32 bit floats are not sufficiently precise. */
    public int fixedLat;
    public int fixedLon;

    public double getLat() {return fixedLat / FIXED_PRECISION_FACTOR;}

    public double getLon() {return fixedLon / FIXED_PRECISION_FACTOR;}

    public void setLatLon (double lat, double lon) {
        this.fixedLat = (int)(lat * FIXED_PRECISION_FACTOR);
        this.fixedLon = (int)(lon * FIXED_PRECISION_FACTOR);
    }

}
