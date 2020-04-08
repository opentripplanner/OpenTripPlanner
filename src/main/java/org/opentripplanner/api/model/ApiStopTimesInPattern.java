package org.opentripplanner.api.model;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.stream.Collectors;


@XmlRootElement(name = "StopTimesInPattern")
public class ApiStopTimesInPattern {

    public ApiPatternShort pattern;
    public List<ApiTripTimeShort> times;

    public ApiStopTimesInPattern(org.opentripplanner.model.StopTimesInPattern other) {
        this.pattern = new ApiPatternShort(other.pattern);
        this.times = other.times.stream().map(ApiTripTimeShort::new).collect(Collectors.toList());
    }
}
