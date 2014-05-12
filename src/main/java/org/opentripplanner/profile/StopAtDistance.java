package org.opentripplanner.profile;

import lombok.AllArgsConstructor;
import org.onebusaway.gtfs.model.Stop;

/**
 * A stop associated with its distance from a search location.
 * Used in profile routing.
*/
@AllArgsConstructor
public class StopAtDistance implements Comparable<StopAtDistance> {

    public Stop stop;
    public int distance;

    @Override
    public int compareTo(StopAtDistance that) {
        return this.distance - that.distance;
    }

    public String toString() {
        return String.format("stop %s at %dm", stop.getCode(), distance);
    }

}
