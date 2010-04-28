package org.opentripplanner.api.extended.ws.model;

import javax.xml.bind.annotation.XmlElement;

import org.onebusaway.gtfs.model.Route;

public class WmsRoute {
    @SuppressWarnings("unused")
    @XmlElement(name="shortName")
    private String shortName;
    
    @SuppressWarnings("unused")
    @XmlElement(name="longName")
    private String longName;
    
    @SuppressWarnings("unused")
    @XmlElement(name="mode")
    private String mode;
    
    @SuppressWarnings("unused")
    @XmlElement(name="agencyId")
    private String agencyId;
    
    @SuppressWarnings("unused")
    @XmlElement(name="routeId")
    private String routeId;
    
    public WmsRoute() {
    }
    
    public WmsRoute(Route route) {
        this.shortName = route.getShortName();
        this.longName = route.getLongName();
        this.mode = getMode(route.getType());
        this.agencyId = route.getId().getAgencyId();
        this.routeId = route.getId().getId();
    }

    private String getMode(int type) {
        switch (type) {
        case 0:
            return "TRAM";
        case 1:
            return "SUBWAY";
        case 2:
            return "RAIL";
        case 3:
            return "BUS";
        case 4:
            return "FERRY";
        case 5:
            return "CABLE_CAR";
        case 6:
            return "GONDOLA";
        case 7:
            return "FUNICULAR";
        default:
            throw new IllegalArgumentException("unknown gtfs route type " + type);
        }
    }
}
