package org.opentripplanner.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Some stopTimes all in the same pattern.
 * TripTimeShort should probably be renamed StopTimeShort
 */
public class StopTimesInPattern {

    public TripPattern pattern;
    public List<TripTimeShort> times = new ArrayList<>();

    public StopTimesInPattern(TripPattern pattern) {
        this.pattern = pattern;
    }

}
