package org.opentripplanner.api.model;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;


@XmlRootElement(name = "StopTimesInPattern")
public class ApiStopTimesInPattern {

    public ApiPatternShort pattern;
    public List<ApiTripTimeShort> times;
}
