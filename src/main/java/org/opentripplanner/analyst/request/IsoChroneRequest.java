package org.opentripplanner.analyst.request;

import java.util.Arrays;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;

/**
 * A request for an isochrone vector.
 * 
 * @author laurent
 */
public class IsoChroneRequest {

    public final List<Integer> cutoffSecList;

    public boolean includeDebugGeometry;

    public int precisionMeters = 200;

    public int offRoadDistanceMeters = 150;

    public int maxTimeSec = 0;

    public Coordinate coordinateOrigin;

    public int minCutoffSec = Integer.MAX_VALUE;

    public int maxCutoffSec = 0;

    public IsoChroneRequest(List<Integer> cutoffSecList) {
        this.cutoffSecList = cutoffSecList;
        for (Integer cutoffSec : cutoffSecList) {
            if (cutoffSec > maxCutoffSec)
                maxCutoffSec = cutoffSec;
            if (cutoffSec < minCutoffSec)
                minCutoffSec = cutoffSec;
        }
    }

    @Override
    public int hashCode() {
        return cutoffSecList.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof IsoChroneRequest) {
            IsoChroneRequest otherReq = (IsoChroneRequest) other;
            return this.cutoffSecList.equals(otherReq.cutoffSecList);
        }
        return false;
    }

    public String toString() {
        return String.format("<isochrone request, cutoff=%s sec, precision=%d meters>",
                Arrays.toString(cutoffSecList.toArray()), precisionMeters);
    }
}
