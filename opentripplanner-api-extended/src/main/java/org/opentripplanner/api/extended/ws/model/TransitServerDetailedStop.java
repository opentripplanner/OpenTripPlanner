package org.opentripplanner.api.extended.ws.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.api.extended.ws.TransitServerGtfs;

@XmlRootElement(name="stop")
public class TransitServerDetailedStop {
    
    private String name;
    
    @XmlElement(name="routeIds")
    private TransitServerRouteIds routeIds;
    
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
        this.routeIds = new TransitServerRouteIds(routeIdsForStopId);
        this.setDepartures(new TransitServerDepartures(latlon, nDepartures, transitServerGtfs));
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

}
