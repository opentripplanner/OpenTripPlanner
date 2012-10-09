package org.opentripplanner.api.model.transit;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import org.opentripplanner.routing.transit_index.adapters.StopType;
import org.opentripplanner.routing.transit_index.adapters.TripType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class StopTime {
    /**
     * These are departure times, except where the stop is the last stop on a particular
     * trip, in which case they are arrivals
     */
    @JsonSerialize
    @XmlAttribute
    public long time;

    @JsonSerialize
    @XmlAttribute
    public String phase;

    @JsonSerialize
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @XmlElement
    public TripType trip;

    @JsonSerialize
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @XmlElement
    public StopType stop;

}
