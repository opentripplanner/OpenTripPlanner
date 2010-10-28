/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.spt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Vector;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareAttribute;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.FareContext;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.WrappedCurrency;
import org.opentripplanner.routing.core.Fare.FareType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * A set of edges on a single route 
 * */
class Ride {
    AgencyAndId route;

    Set<String> zones;

    String startZone;

    String endZone;
    
    long startTime;

    public Ride() {
        zones = new HashSet<String>();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Ride");
        if (startZone != null) {
            builder.append("(from ");
            builder.append(startZone);
        }
        if (endZone != null) {
            builder.append(" to ");
            builder.append(endZone);
        }
        builder.append(" on ");
        builder.append(route);
        if (zones.size() > 0) {
            builder.append(" through ");
            boolean first = true;
            for (String zone : zones) {
                if (first) {
                    first = false;
                } else {
                    builder.append(",");
                }
                builder.append(zone);
            }
        }
        builder.append(" at ");
        builder.append(startTime);
        builder.append(")");
        return builder.toString();
    }
}

/**
 * A shortest path on the graph. 
 */
public class GraphPath {
    private final Logger _log = LoggerFactory.getLogger(GraphPath.class);
    
    public Vector<SPTVertex> vertices;

    public Vector<SPTEdge> edges;

    public GraphPath() {
        this.vertices = new Vector<SPTVertex>();
        this.edges = new Vector<SPTEdge>();
    }

    public void optimize() {
        State state = vertices.lastElement().state.clone();
        State state0 = vertices.firstElement().state;
        
        state.alightedLocal = false;
        state.everBoarded = false;
        
        if (edges.isEmpty()) {
            /* nothing to optimize */
            return;
        }
        if (state0.getTime() >= state.getTime()) {
            /* arrive-by trip */

            TraverseOptions options = vertices.lastElement().options;
            ListIterator<SPTEdge> iterator = edges.listIterator(vertices.size() - 1);
            SPTEdge firstEdge = edges.get(0);
            while (iterator.hasPrevious()) {
                SPTEdge edge = iterator.previous();
                if (edge == firstEdge) {
                    state.lastEdgeWasStreet = false;
                }
                TraverseResult result = edge.payload.traverse(state, options);
                assert (result != null);
                state = result.state;
                edge.fromv.state = state;
            }
        } else {
            TraverseOptions options = vertices.lastElement().options;
            ListIterator<SPTEdge> iterator = edges.listIterator(vertices.size() - 1);
            SPTEdge firstEdge = edges.get(0);
            while (iterator.hasPrevious()) {
                SPTEdge edge = iterator.previous();
                if (edge == firstEdge) {
                    state.lastEdgeWasStreet = false;
                }
                TraverseResult result = edge.payload.traverseBack(state, options);
                state = result.state;
                edge.fromv.state = state;
            }
        }
    }

    /** See the thread on gtfs-changes explaining the proper interpretation of fares.txt */
    
    public Fare getCost() {
        State state = vertices.lastElement().state;
        FareContext fareContext = state.fareContext;
        if (fareContext == null) {
            // we have never actually visited any zones, so there's no fare data.
            // perhaps we're planning a biking-only trip.
            return null;
        }

        Currency currency = null;
        HashMap<AgencyAndId, FareRuleSet> fareRules = fareContext.getFareRules();
        HashMap<AgencyAndId, FareAttribute> fareAttributes = fareContext.getFareAttributes();

        // create rides
        List<Ride> rides = new ArrayList<Ride>();
        Ride newRide = null;
        for (SPTVertex vertex : vertices) {
            String zone = vertex.state.zone;
            AgencyAndId route = vertex.state.route;
            if (zone == null) {
                newRide = null;
            } else {
                if (newRide == null || !route.equals(newRide.route)) {
                    newRide = new Ride();
                    rides.add(newRide);
                    newRide.startZone = zone;
                    newRide.route = route;
                    newRide.startTime = vertex.state.getTime();
                }
                newRide.zones.add(zone);
                newRide.endZone = zone;
            }
        }

        // There are no rides, so there's no fare.
        if (rides.size() == 0) {
            return null;
        }
        
        // greedily consume rides

        Set<String> zones = new HashSet<String>();
        Set<AgencyAndId> routes = new HashSet<AgencyAndId>();
        String startZone = null;
        int transfersUsed = -1;
        float totalFare = 0, currentFare = -1;
        long startTime = rides.get(0).startTime;
        for (int i = 0; i < rides.size(); ++i) {
            Ride ride = rides.get(i);
            if (startZone == null) {
                startZone = ride.startZone;
            }
            float bestFare = Float.MAX_VALUE;
            routes.add(ride.route);
            zones.addAll(ride.zones);
            transfersUsed += 1;

            long tripTime = ride.startTime - startTime;
            
            // find the best fare that matches this set of rides
            for (AgencyAndId fareId : fareRules.keySet()) {
                FareRuleSet ruleSet = fareRules.get(fareId);
                if (ruleSet.matches(startZone, ride.endZone, zones, routes)) {
                    FareAttribute attribute = fareAttributes.get(fareId);
                    if (attribute.isTransfersSet() && attribute.getTransfers() < transfersUsed) {
                        continue;
                    }
                    //assume transfers are evaluated at boarding time,
                    //as trimet does
                    if (attribute.isTransferDurationSet() && tripTime > attribute.getTransferDuration() * 1000) {
                        continue;
                    }
                    float newFare = attribute.getPrice();
                    if (newFare < bestFare) {
                        bestFare = newFare;
                        currency = Currency.getInstance(attribute.getCurrencyType());
                    }
                }
            }

            if (bestFare == Float.MAX_VALUE) {
                if (currentFare == -1) {
                    // Problem: there's no fare for this ride.
                    _log.warn("No fare for a perfectly good ride: " + ride);
                    return null;
                }

                // there's no fare, but we can fall back to the previous fare, and retry starting
                // here
                totalFare += currentFare;
                currentFare = -1;
                transfersUsed = -1;
                --i;
                zones = new HashSet<String>();
                startZone = ride.startZone;
                routes = new HashSet<AgencyAndId>();
                startTime = ride.startTime;
            } else {
                currentFare = bestFare;
            }
        }

        totalFare += currentFare;

        Fare fare = new Fare();
        fare.addFare(FareType.regular, new WrappedCurrency(currency), (int) Math.round(totalFare * Math.pow(
                10, currency.getDefaultFractionDigits())));
        return fare;
    }

    public String toString() {
        return vertices.toString();
    }

    public void reverse() {
        Collections.reverse(vertices);
        Collections.reverse(edges);
        for (SPTEdge e : edges) {
            SPTVertex tmp = e.fromv;
            e.fromv = e.tov;
            e.tov = tmp;
        }
    }
    
    public boolean equals(Object o) {
        if (o instanceof GraphPath) {
            return this.edges.equals(((GraphPath)o).edges);
        }
        return false;
    }
    
    public int hashCode() {
        return this.edges.hashCode();
    }
}