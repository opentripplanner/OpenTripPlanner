package org.opentripplanner.api.extended.ws.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.api.extended.ws.TransitServerGtfs;

@XmlRootElement(name="stop")
public class TransitServerDetailedStop {
    
    private String name;

    private TransitServerDetailedRoutes routes;
    
    private TransitServerDepartures departures;
    
    public TransitServerDetailedStop() {
    }

    public TransitServerDetailedStop(TransitServerGtfs transitServerGtfs,
            String latlon, int nDepartures) {
        List<Stop> stopsForLatLon = transitServerGtfs.getStopsForLatLon(latlon);
        if (stopsForLatLon.size() == 0) {
            throw new IllegalStateException("no stops found for latlon: " + latlon);
        }
        // we have the first stop as the representative one
        // the other ones differ very slightly by name, but have the same location
        Stop stop = stopsForLatLon.get(0);
        String stopId = stop.getId().toString();
        this.setName(stop.getName());
        Set<String> routeIdsForStopId = transitServerGtfs.getRouteIdsForStopId(stopId);
        this.setDepartures(new TransitServerDepartures(latlon, nDepartures, transitServerGtfs));
        List<Route> routes = new ArrayList<Route>();
        for (String routeId : routeIdsForStopId) {
            Route route = transitServerGtfs.getRoute(routeId);
            routes.add(route);
        }
        this.setRoutes(new TransitServerDetailedRoutes(routes));
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setDepartures(TransitServerDepartures departures) {
        this.departures = departures;
    }

    public TransitServerDepartures getDepartures() {
        return departures;
    }

    public void setRoutes(TransitServerDetailedRoutes routes) {
        this.routes = routes;
    }

    public TransitServerDetailedRoutes getRoutes() {
        return routes;
    }

}
