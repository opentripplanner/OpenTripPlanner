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

import java.util.LinkedList;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.RouteSpec;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A shortest path on the graph.
 */
public class GraphPath {
    private static final Logger LOG = LoggerFactory.getLogger(GraphPath.class);

    public LinkedList<State> states;

    public LinkedList<Edge> edges;

    // needed to track repeat invocations of path-reversing methods
    private boolean back;

    private double walkDistance = 0;

    // don't really need to save this (available through State) but why not
    private RoutingContext rctx;

    /**
     * Construct a GraphPath based on the given state by following back-edge fields all the way back
     * to the origin of the search. This constructs a proper Java list of states (allowing random
     * access etc.) from the predecessor information left in states by the search algorithm.
     * 
     * Optionally re-traverses all edges backward in order to remove excess waiting time from the
     * final itinerary presented to the user.
     * 
     * @param s
     *            - the state for which a path is requested
     * @param optimize
     *            - whether excess waiting time should be removed
     * @param options
     *            - the traverse options used to reach this state
     */
    public GraphPath(State s, boolean optimize) {
        this.rctx = s.getContext();
        this.back = s.getOptions().isArriveBy();
        
        /* Put path in chronological order, and optimize as necessary */
        State lastState;
        walkDistance = s.getWalkDistance();
        if (back) {
            lastState = optimize ? s.optimize() : s.reverse();
        } else {
            lastState = optimize ? s.optimize().reverse() : s;
        }
        // DEBUG
        // lastState = s;

        /*
         * Starting from latest (time-wise) state, copy states to the head of a list in reverse
         * chronological order. List indices will thus increase forward in time, and backEdges will
         * be chronologically 'back' relative to their state.
         */
        this.states = new LinkedList<State>();
        this.edges = new LinkedList<Edge>();
        for (State cur = lastState; cur != null; cur = cur.getBackState()) {
            states.addFirst(cur);
            if (cur.getBackEdge() != null)
                edges.addFirst(cur.getBackEdge());
        }
    }

    /**
     * Returns the start time of the trip in seconds since the epoch.
     * @return
     */
    public long getStartTime() {
        return states.getFirst().getTime();
    }

    /**
     * Returns the end time of the trip in seconds since the epoch.
     * @return
     */
    public long getEndTime() {
        return states.getLast().getTime();
    }

    /**
     * Returns the duration of the trip in seconds.
     * @return
     */
    public int getDuration() {
        // test to see if it is the same as getStartTime - getEndTime;
        return (int) states.getLast().getElapsedTime();
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
     * 
     * @return a list of RouteSpec objects for this path
     */
    public List<RouteSpec> getRouteSpecs() {
        List<RouteSpec> ret = new LinkedList<RouteSpec>();
        for (State s : states) {
            Edge e = s.getBackEdge();
            if (e == null) continue;
            Trip trip = s.getBackTrip();
            if ( trip != null) {
                String routeName = GtfsLibrary.getRouteName(trip.getRoute());
                RouteSpec spec = new RouteSpec(trip.getId().getAgencyId(), routeName);
                ret.add(spec);
                // TODO: Check implementation, use edge list in graphpath
            }
        }
        return ret;
    }

    /**
     * Get a list containing one AgencyAndId (trip id) for each vehicle boarded in this path.
     * 
     * @return a list of the ids of trips used by this path
     */
    public List<AgencyAndId> getTrips() {
        List<AgencyAndId> ret = new LinkedList<AgencyAndId>();
        for (State s : states) {
            Edge e = s.getBackEdge();
            if (e == null) continue;
            Trip trip = s.getBackTrip();
            if (trip != null)
                ret.add(trip.getId());
        }
        return ret;
    }

    public String toString() {
        return "GraphPath(" + states.toString() + ")";
    }

    /**
     * Two paths are equal if they use the same ordered list of trips
     */
    public boolean equals(Object o) {
        if (o instanceof GraphPath) {
            GraphPath go = (GraphPath) o;
            return go.getTrips().equals(getTrips());
        }
        return false;
    }

    // must compare edges, not states, since states are different at each search
    public int hashCode() {
        return this.edges.hashCode();
    }

    /****
     * Private Methods
     ****/

    public void dump() {
        System.out.println(" --- BEGIN GRAPHPATH DUMP ---");
        System.out.println(this.toString());
        for (State s : states)
            System.out.println(s + " via " + s.getBackEdge());
        System.out.println(" --- END GRAPHPATH DUMP ---");
    }

    public void dumpPathParser() {
        System.out.println(" --- BEGIN GRAPHPATH DUMP ---");
        System.out.println(this.toString());
        for (State s : states) 
            System.out.println(s.getPathParserStates() + s + " via " + s.getBackEdge());
        System.out.println(" --- END GRAPHPATH DUMP ---");
    }

    public double getWalkDistance() {
        return walkDistance;
    }
    
    public RoutingContext getRoutingContext() {
        return rctx;
    }

}
