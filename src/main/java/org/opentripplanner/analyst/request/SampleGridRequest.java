package org.opentripplanner.analyst.request;

import org.locationtech.jts.geom.Coordinate;

/**
 * A request for a sample grid (of a SPT).
 * 
 * @author laurent
 */
public class SampleGridRequest {

    public int precisionMeters = 200;

    public int offRoadDistanceMeters = 150;

    public int maxTimeSec = 0;

    public Coordinate coordinateOrigin;

    public SampleGridRequest() {
    }

    public String toString() {
        return String.format("<timegrid request, coordBase=%s precision=%d meters>",
                coordinateOrigin, precisionMeters);
    }
}
