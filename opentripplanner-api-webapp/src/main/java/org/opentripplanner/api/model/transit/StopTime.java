package org.opentripplanner.api.model.transit;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.patch.AgencyAndIdAdapter;

public class StopTime {
    /**
     * These are departure times, except where the stop is the last stop on a particular
     * trip, in which case they are arrivals
     */
    @XmlAttribute
    public long time;

    @XmlElement
    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    public AgencyAndId trip;

}
