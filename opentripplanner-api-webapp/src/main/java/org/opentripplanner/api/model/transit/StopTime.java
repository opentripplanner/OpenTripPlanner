package org.opentripplanner.api.model.transit;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.opentripplanner.routing.transit_index.adapters.StopType;
import org.opentripplanner.routing.transit_index.adapters.TripType;

public class StopTime {
    /**
     * These are departure times, except where the stop is the last stop on a particular
     * trip, in which case they are arrivals
     */
    @XmlAttribute
    public long time;

    @JsonSerialize(include=Inclusion.NON_NULL)
    @XmlElement
    public TripType trip;

    @JsonSerialize(include=Inclusion.NON_NULL)
    @XmlElement
    public StopType stop;
}
