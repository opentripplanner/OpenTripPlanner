package org.opentripplanner.api.extended.ws.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.api.extended.fork.EncodedPolylineBean;
import org.opentripplanner.api.extended.ws.TransitServerGtfs;

import com.vividsolutions.jts.geom.Geometry;

public class TransitServerRoute {
    private String shortname;
    private String longname;
    private String id;
    private String agencyId;
    private String mode;
    
    private TransitServerStops transitStops;
    
    private EncodedPolylineBean geometry;

    public TransitServerRoute() {
    }
    
    public TransitServerRoute(Route route, List<Stop> stops, EncodedPolylineBean geometry) {
        this.setId(route.getId().toString());
        this.setShortName(route.getShortName());
        this.setLongName(route.getLongName());
        this.setGeometry(geometry);
        this.setAgencyId(route.getAgency().getId());
        this.transitStops = new TransitServerStops(stops);
        
        switch (route.getType()) {
        case 0:
            this.setMode("TRAM");
            break;
        case 1:
            this.setMode("SUBWAY");
            break;
        case 2:
            this.setMode("RAIL");
            break;
        case 3:
            this.setMode("BUS");
            break;
        case 4:
            this.setMode("FERRY");
            break;
        case 5:
            this.setMode("CABLE_CAR");
            break;
        case 6:
            this.setMode("GONDOLA");
            break;
        case 7:
            this.setMode("FUNICULAR");
            break;
        default:
            throw new IllegalArgumentException("unknown gtfs route type " + route.getType());
        }
    }

    public TransitServerRoute(TransitServerGtfs transitServerGtfs, String routeId) {
        Route route = transitServerGtfs.getRoute(routeId);
        List<Stop> stops = transitServerGtfs.getStopsForRoute(routeId);
        
        this.setId(routeId);
        this.setShortName(route.getShortName());
        this.setLongName(route.getLongName());
        this.setStops(new TransitServerStops(stops));
    }

    public void setShortName(String shortName) {
        this.shortname = shortName;
    }

    public String getShortName() {
        return shortname;
    }
    
    public void setLongName(String longName) {
        this.longname = longName;
    }
    
    public String getLongName() {
        return longname;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setGeometry(EncodedPolylineBean geometry) {
        this.geometry = geometry;
    }

    public EncodedPolylineBean getGeometry() {
        return geometry;
    }
    
    public TransitServerStops getStops() {
        return transitStops;
    }
    public void setStops(TransitServerStops stops) {
        this.transitStops = stops;
    }

    public void setAgencyId(String agencyId) {
        this.agencyId = agencyId;
    }

    public String getAgencyId() {
        return agencyId;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }

}
