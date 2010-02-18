package org.opentripplanner.routing.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.common.model.P2;

public class FareContext {

    private Set<AgencyAndId> routes;
    private Set<P2<String>> originDestinations;
    private Set<String> contains;
    
    public FareContext() {
        routes = new HashSet<AgencyAndId>();
        originDestinations= new HashSet<P2<String>>();
        contains = new HashSet<String>();
    }

    public void addOriginDestination(String origin, String destination) {
        originDestinations.add(new P2<String>(origin, destination));        
    }

    public void addContains(String containsId) {
        contains.add(containsId);
    }
    
    public void addRoute(AgencyAndId route) {
        routes.add(route);
    }

    public boolean matches(List<String> zonesVisited, List<AgencyAndId> routesVisited) {
        //check for matching origin/destination
        P2<String> od = new P2<String>(zonesVisited.get(0), zonesVisited.get(zonesVisited.size() - 1));
        if (!originDestinations.contains(od)) {
            P2<String> od2 = new P2<String>(od.getFirst(), null);
            if (!originDestinations.contains(od2)) {
                od2 = new P2<String>(null, od.getFirst());
                if (!originDestinations.contains(od2)) {
                    return false;
                }
            }
        }
        //check for matching contains
        for (String contained : contains) {
            if (!zonesVisited.contains(contained)) {
                return false;
            }
        }
        
        //check for matching routes
        for (AgencyAndId route : routesVisited) {
            if (!routes.contains(route)) {
                return false;
            }
        }
        
        return true;
    }
}

