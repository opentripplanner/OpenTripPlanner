package org.opentripplanner.api.extended.ws.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;

public class TransitServerRouteIds {

    @XmlElement(name="routeId")
    private List<String> routeIds;
    
    public TransitServerRouteIds() {
    }

    public TransitServerRouteIds(Set<String> routeIdsForStopId) {
        this.routeIds = new ArrayList<String>();
        for (String routeId : routeIdsForStopId) {
            this.routeIds.add(routeId);
        }
    }

}
