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
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareAttribute;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.FareContext;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.core.MutableEdgeNarrative;
import org.opentripplanner.routing.core.RouteSpec;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.core.WrappedCurrency;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.edgetype.PatternBoard;
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
    private static final Logger _log = LoggerFactory.getLogger(GraphPath.class);
    public LinkedList<State> states;
    public LinkedList<Edge> edges;
    // needed to track repeat invocations of path-reversing methods
    private boolean back; 
    private TraverseOptions options; 
    
    /**
     * Construct a GraphPath based on the given state by following 
     * back-edge fields all the way back to the origin of the search. 
     * This constructs a proper Java list of states (allowing random access 
     * etc.) from the predecessor information left in states by the 
     * search algorithm.
     * 
     * Optionally re-traverses all edges backward in order to remove excess 
     * waiting time from the final itinerary presented to the user.
     * 
     * @param s - the state for which a path is requested
     * @param optimize - whether excess waiting time should be removed
     * @param options - the traverse options used to reach this state
     */
    public GraphPath(State s, boolean optimize) {
    	this.options = s.getOptions();
    	this.back = options.isArriveBy();
    	
        /* Put path in chronological order, and optimize as necessary */
        State lastState;
        if (back) {
        	lastState = optimize ? optimize(s) : reverse(s);
        } else {
        	lastState = optimize ? reverse(optimize(s)) : s;
        }
        //DEBUG
        //lastState = s;
        
        /* Starting from latest (time-wise) state, copy states to the head
         * of a list in reverse chronological order. List indices will thus
         * increase forward in time, and backEdges will be chronologically 
         * 'back' relative to their state.
         */
        this.states = new LinkedList<State>();
        this.edges = new LinkedList<Edge>();
        for (State cur = lastState; cur != null; cur = cur.getBackState()) {
        	states.addFirst(cur);
        	if (cur.getBackEdge() != null)
        		edges.addFirst(cur.getBackEdge());
        }
    }

    public long getStartTime() {
        return states.getFirst().getTime();
    }

    public long getEndTime() {
        return states.getLast().getTime();
    }

    public long getDuration() {
    	// test to see if it is the same as getStartTime - getEndTime;
        return states.getLast().getElapsedTime();
    }

    public double getWeight() {
        return states.getLast().getWeight();
    }

    public Vertex getStartVertex() {
        return states.getFirst().getVertex();
    }
    
    public Vertex getEndVertex() {
        return states.getLast().getVertex();
    }

    /**
     * Get a list containing one RouteSpec object for each vehicle boarded in this path. 
     * @return a list of RouteSpec objects for this path
     */
    public List<RouteSpec> getRouteSpecs() {
    	List<RouteSpec> ret = new LinkedList<RouteSpec>();
    	for (State s : states) {
    		Edge e = s.getBackEdge();
            if (e instanceof PatternBoard) {
            	Trip trip = ((PatternBoard)e).getPattern().getTrip(s.getTrip());
                String routeName = GtfsLibrary.getRouteName(trip.getRoute());
                RouteSpec spec = new RouteSpec(trip.getId().getAgencyId(), routeName);
                ret.add(spec);
                // TODO: Check implementation, use edge list in graphpath
            }
    	}
    	return ret;
    }
    
    /** See the thread on gtfs-changes explaining the proper interpretation of fares.txt */

    public Fare getCost() {
    	
        State state = states.getLast();
        FareContext fareContext = state.getFareContext();
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
        for (State curr : states) {
            String zone = curr.getZone();
            AgencyAndId route = curr.getRoute();
            if (zone == null) {
                newRide = null;
            } else {
                if (newRide == null || !route.equals(newRide.route)) {
                    newRide = new Ride();
                    rides.add(newRide);
                    newRide.startZone = zone;
                    newRide.route = route;
                    newRide.startTime = curr.getTime();
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
                    // assume transfers are evaluated at boarding time,
                    // as trimet does
                    if (attribute.isTransferDurationSet()
                            && tripTime > attribute.getTransferDuration() * 1000) {
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
        fare.addFare(FareType.regular, new WrappedCurrency(currency),
                (int) Math.round(totalFare * Math.pow(10, currency.getDefaultFractionDigits())));
        return fare;
    }

    public String toString() {
        return "GraphPath(" + states.toString() + ")";
    }

    public boolean equals(Object o) {
        if (o instanceof GraphPath) {
            return this.states.equals(((GraphPath) o).states);
        }
        return false;
    }

    public int hashCode() {
        return this.states.hashCode();
    }

    /****
     * Private Methods
     ****/

    /** 
     * Reverse the path implicit in the given state, i.e. produce a new
     * chain of states that leads from this state to the other end of the
     * implicit path.
     */
    private State reverse(State s) {
    	// not implemented yet;
    	// use reverse-optimize method for now.
    	return optimize(s);    	
    }

    /** 
     * Reverse the path implicit in the given state, re-traversing all edges
     * in the opposite direction so as to remove any unnecessary waiting in 
     * the resulting itinerary. This produces a path that passes through all the
     * same edges, but which may have a shorter overall duration due to different
     * weights on time-dependent (e.g. transit boarding) edges.
     * 
     * @param s - a state resulting from a path search
     * @return a state at the other end of a reversed, optimized path 
     */
    // not static because direction of search is tracked in instance
    private State optimize(State s) {

    	// this should be sufficient, do we need a special reverse reset method?
    	State ret = new State(s.getTime(), s.getVertex(), s.getOptions());

    	// reverse the search direction 
    	// for now, traverse and makeChild do not look at the direction 
    	// indicated in the options, so we can force a reverse traverse.
    	back = !back;
    	
    	for (State orig = s; orig != null; orig = orig.getBackState()) {
    		Edge e = orig.getBackEdge();
    		if (e==null) continue; //break
    		ret = back ? e.traverseBack(ret) : e.traverse(ret);
            copyExistingNarrativeToNewNarrativeAsAppropriate(
            		orig.getBackEdgeNarrative(), ret.getBackEdgeNarrative());
    	}
    	
        return ret;
    }

    private static void copyExistingNarrativeToNewNarrativeAsAppropriate(
    		EdgeNarrative from, EdgeNarrative to) {

        if (!(to instanceof MutableEdgeNarrative))
            return;

        MutableEdgeNarrative m = (MutableEdgeNarrative) to;

        if (to.getFromVertex() == null)
            m.setFromVertex(from.getFromVertex());

        if (to.getToVertex() == null)
            m.setToVertex(from.getToVertex());
    }

}