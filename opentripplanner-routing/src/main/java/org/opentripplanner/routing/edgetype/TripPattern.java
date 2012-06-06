package org.opentripplanner.routing.edgetype;

import java.util.List;

import javax.xml.bind.annotation.XmlTransient;

import org.onebusaway.gtfs.model.Stop;

/* simple interface for trip patterns */
public interface TripPattern {
    @XmlTransient
    List<Stop> getStops();
}
