package org.opentripplanner.api.model.transit;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.transit_index.adapters.AgencyAndIdAdapter;
import org.opentripplanner.routing.transit_index.adapters.StopAdapter;
import org.opentripplanner.routing.transit_index.adapters.StopType;
import org.opentripplanner.routing.transit_index.adapters.TripAdapter;
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
    
    @JsonSerialize(include=Inclusion.NON_NULL)
    @XmlElement(name = "trip")
    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    public AgencyAndId tripId;

    @JsonSerialize(include=Inclusion.NON_NULL)
    @XmlElement(name = "stop")
    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    public AgencyAndId stopId;
    
}
