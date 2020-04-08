package org.opentripplanner.api.model;

import java.util.List;
import java.util.stream.Collectors;


public class StopTimesInPattern {

    public PatternShort pattern;
    public List<ApiTripTimeShort> times;

    public StopTimesInPattern(org.opentripplanner.model.StopTimesInPattern other) {
        this.pattern = new PatternShort(other.pattern);
        this.times = other.times.stream().map(ApiTripTimeShort::new).collect(Collectors.toList());
    }
}
