package org.opentripplanner.api.extended.ws.model;

import java.util.Date;

import org.onebusaway.gtfs.model.Route;

public class TransitServerDeparture implements Comparable<TransitServerDeparture> {
    private String routeId;
    private String headsign;
    private Date date;

    public TransitServerDeparture() {        
    }
    
    public TransitServerDeparture(Route route, String headsign, Date date) {
        String routeId = route.getId().toString();
        this.setRouteId(routeId);
        this.setHeadsign(headsign);
        this.setDate(date);
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setHeadsign(String headsign) {
        this.headsign = headsign;
    }

    public String getHeadsign() {
        return headsign;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getDate() {
        return date;
    }

    @Override
    public int compareTo(TransitServerDeparture other) {
        return this.getDate().compareTo(other.getDate());
    }
}
