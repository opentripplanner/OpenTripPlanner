package org.opentripplanner.api.extended.ws.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.onebusaway.gtfs.model.Stop;

@XmlRootElement(name="stops")
public class TransitServerStops {
    
    @XmlElement(name="stop")
    private List<TransitServerStop> transitStops = new ArrayList<TransitServerStop>();
    
    public TransitServerStops() {
    }
    
    public TransitServerStops(List<Stop> stops) {
        for (Stop stop : stops) {
            transitStops.add(new TransitServerStop(stop));
        }
    }
}
