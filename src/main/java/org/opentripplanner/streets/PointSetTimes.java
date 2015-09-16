package org.opentripplanner.streets;

import org.opentripplanner.analyst.PointSet;

/**
 * The travel time to every point in a PointSet.
 * This class just serves to provide type context to a raw array of travel times, and to associate those travel times
 * with a specific PointSet to catch mistakes where a set of travel times is used with the wrong PointSet.
 */
public class PointSetTimes {

    public final PointSet pointSet;

    public final int[] travelTimes;

    protected PointSetTimes(PointSet pointSet, int[] travelTimes) {
        this.pointSet = pointSet;
        this.travelTimes = travelTimes;
    }

    public int size()  {
        return travelTimes.length;
    }

    public int getTravelTimeToPoint (int p) {
        return travelTimes[p];
    }
}
