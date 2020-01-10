package org.opentripplanner.api.model;

import java.util.List;
import java.util.stream.Collectors;


public class StopTimesInPattern {

    public PatternShort pattern;
    public List<TripTimeShort> times;

    public StopTimesInPattern(org.opentripplanner.index.model.StopTimesInPattern other) {
        this.pattern = new PatternShort(other.pattern);
        this.times = other.times.stream().map(TripTimeShort::new).collect(Collectors.toList());
    }
}
