package org.opentripplanner.api.extended.ws.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import org.onebusaway.gtfs.model.Route;

public class TransitServerDetailedRoutes {
    
    @XmlElement(name="route")
    private List<TransitServerDetailedRoute> transitRoutes = new ArrayList<TransitServerDetailedRoute>();
    
    public TransitServerDetailedRoutes() {
    }
    
    public TransitServerDetailedRoutes(List<Route> routes) {
        for (Route route : routes) {
            transitRoutes.add(new TransitServerDetailedRoute(route));
        }
    }
}
