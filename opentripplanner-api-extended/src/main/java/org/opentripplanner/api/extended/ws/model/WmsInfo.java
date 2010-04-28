package org.opentripplanner.api.extended.ws.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.api.extended.ws.TransitServerGtfs;

@XmlRootElement(name="wmsinfo")
public class WmsInfo {
    @SuppressWarnings("unused")
    @XmlElement(name="type")
    private String type;
    
    @SuppressWarnings("unused")
    @XmlElement(name="stop")
    private WmsStop detailedStop;
    
    @SuppressWarnings("unused")
    @XmlElement(name="routes")
    private List<WmsRoute> detailedRoutes;

    @SuppressWarnings("unused")
    @XmlElement(name="stopids")
    private List<String> stopIds;

    @SuppressWarnings("unused")
    @XmlElement(name="routes")
    private List<WmsRoute> routes;
    
    public WmsInfo() {
    }
    
    public WmsInfo(TransitServerGtfs gtfs, AgencyAndId stopId) {
        this.type = "stop";
        this.detailedStop = new WmsStop(gtfs, stopId);
    }

    public WmsInfo(TransitServerGtfs gtfs, List<String> routeIds) {
        this.type = "routes";
                
        List<String> stopIds = new ArrayList<String>();
        List<WmsRoute> routes = new ArrayList<WmsRoute>();
        for (String routeId : routeIds) {
            Route route = gtfs.getRoute(routeId);
            routes.add(new WmsRoute(route));
            
            List<Stop> stopsForRoute = gtfs.getStopsForRoute(routeId);
            for (Stop stop : stopsForRoute) {
                String stopId = stop.getId().getId();
                stopIds.add(stopId);
            }
        }
        this.stopIds = stopIds;
        this.routes = routes;
    }
}
