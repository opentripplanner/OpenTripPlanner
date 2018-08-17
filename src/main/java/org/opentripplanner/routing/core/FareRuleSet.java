package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.FareAttribute;
import org.opentripplanner.common.model.P2;

public class FareRuleSet implements Serializable {

    private static final long serialVersionUID = 7218355718876553028L;

    private String agency = null;
    private Set<FeedScopedId> routes;
    private Set<P2<String>> originDestinations;
    private Set<String> contains;
    private FareAttribute fareAttribute;
    private Set<FeedScopedId> trips;
    
    public FareRuleSet(FareAttribute fareAttribute) {
        this.fareAttribute = fareAttribute;
        routes = new HashSet<FeedScopedId>();
        originDestinations= new HashSet<P2<String>>();
        contains = new HashSet<String>();
        trips = new HashSet<FeedScopedId>();
    }

    public void setAgency(String agency) {
        // TODO With new GTFS lib, read value from fareAttribute directly?
        this.agency = agency;
    }
    
    public String getAgency() {
    	return agency;
    }

    public void addOriginDestination(String origin, String destination) {
        originDestinations.add(new P2<String>(origin, destination));
    }

    public Set<P2<String>> getOriginDestinations() {
        return originDestinations;
    }

    public void addContains(String containsId) {
        contains.add(containsId);
    }
    
    public void addRoute(FeedScopedId route) {
        routes.add(route);
    }
    
    public Set<FeedScopedId> getRoutes() {
    	return routes;
    }

    public FareAttribute getFareAttribute() {
        return fareAttribute;
    }

    public boolean hasAgencyDefined() {
        return agency != null;
    }

    public void addTrip(FeedScopedId trip) {
    	trips.add(trip);
    }
    
    public Set<FeedScopedId> getTrips() {
    	return trips;
    }
    
    public boolean matches(Set<String> agencies, String startZone, String endZone, Set<String> zonesVisited,
                           Set<FeedScopedId> routesVisited, Set<FeedScopedId> tripsVisited) {
        //check for matching agency
        if (agency != null) {
            if (agencies.size() != 1 || !agencies.contains(agency))
                return false;
        }

        //check for matching origin/destination, if this ruleset has any origin/destination restrictions
        if (originDestinations.size() > 0) {
            P2<String> od = new P2<String>(startZone, endZone);
            if (!originDestinations.contains(od)) {
                P2<String> od2 = new P2<String>(od.first, null);
                if (!originDestinations.contains(od2)) {
                    od2 = new P2<String>(null, od.first);
                    if (!originDestinations.contains(od2)) {
                        return false;
                    }
                }
            }
        }

        //check for matching contains, if this ruleset has any containment restrictions
        if (contains.size() > 0) {
            if (!zonesVisited.equals(contains)) {
                return false;
            }
        }

        //check for matching routes
        if (routes.size() != 0) {
            if (!routes.containsAll(routesVisited)) {
                return false;
            }
        }
        
        //check for matching trips
        if (trips.size() != 0) {
        	if (!trips.containsAll(tripsVisited)) {
        		return false;
        	}
        }

        return true;
    }
}

