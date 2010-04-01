package org.opentripplanner.api.extended.ws.model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="routeId")
public class TransitServerRouteId {

    private String routeId;

    public TransitServerRouteId() {
    }
    
    public TransitServerRouteId(String routeId) {
        this.setRouteId(routeId);
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public String getRouteId() {
        return routeId;
    }

}
