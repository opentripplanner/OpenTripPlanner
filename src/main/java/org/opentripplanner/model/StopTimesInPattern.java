package org.opentripplanner.model;

import com.google.common.collect.Lists;
import org.opentripplanner.index.model.PatternShort;

import java.util.List;

/**
 * Some stopTimes all in the same pattern.
 * TripTimeShort should probably be renamed StopTimeShort
 */
public class StopTimesInPattern {

    public PatternShort pattern;
    public List<TripTimeShort> times = Lists.newArrayList();

    public StopTimesInPattern(TripPattern pattern) {
        this.pattern = new PatternShort(pattern);
    }

}
