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
     * final itinerary presented to the user. When planning with departure time, the edges will then
     * be re-traversed once more in order to move the waiting time forward in time, towards the end.
     * 
     * @param s
     *            - the state for which a path is requested
     * @param optimize
     *            - whether excess waiting time should be removed
     * @param options
     *            - the traverse options used to reach this state
     */
    public GraphPath(State s, boolean optimize) {
        // Only optimize transit trips
        optimize &= s.getOptions().modes.isTransit();
        this.rctx = s.getContext();
        this.back = s.getOptions().arriveBy;
        // optimize = false; // DEBUG
        if (s.getOptions().startingTransitTripId != null) {
            LOG.debug("Disable reverse-optimize for on-board depart");
            optimize = false;
        }

//        LOG.info("NORMAL");
//        s.dumpPath();
//        LOG.info("OPTIMIZED");
//        s.optimize().dumpPath();

        /* Put path in chronological order, and optimize as necessary */
        State lastState;
        walkDistance = s.getWalkDistance();
        if (back) {
            lastState = optimize ? s.optimize() : s.reverse();
        } else {
            lastState = optimize ? s.optimize().optimize() : s;
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
            
            // Record the edge if it exists and this is not the first state in the path.
            if (cur.getBackEdge() != null && cur.getBackState() != null) {
                edges.addFirst(cur.getBackEdge());
            }
        }
        // dump();
    }

    /**
     * Returns the start time of the trip in seconds since the epoch.
     * @return
     */
    public long getStartTime() {
        return states.getFirst().getTimeSeconds();
    }

    /**
     * Returns the end time of the trip in seconds since the epoch.
     * @return
     */
    public long getEndTime() {
        return states.getLast().getTimeSeconds();
    }

    /**
     * Returns the duration of the trip in seconds.
     * @return
     */
    public int getDuration() {
        // test to see if it is the same as getStartTime - getEndTime;
        return (int) states.getLast().getElapsedTimeSeconds();
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

    /** @return A list containing one AgencyAndId (trip_id) for each vehicle boarded in this path,
     * in the chronological order they are boarded. */
    public List<AgencyAndId> getTrips() {
        List<AgencyAndId> ret = new LinkedList<AgencyAndId>();
        Trip lastTrip = null;
        for (State s : states) {
            if (s.getBackEdge() != null) {
                Trip trip = s.getBackTrip();
                if (trip != null && trip != lastTrip) {
                    ret.add(trip.getId());
                    lastTrip = trip;
                }
            }
        }
        return ret;
    }

    public String toString() {
    	return "GraphPath(nStates=" + states.size() + ")";
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
        for (State s : states) {
            //System.out.println(s.getBackEdge() + " leads to " + s);
            if (s.getBackEdge() != null) {
                System.out.println(s.getBackEdge().getClass().getSimpleName() + " --> " + s.getVertex().getClass().getSimpleName());
                System.out.println("  " + s.weight);
            }
        }
        System.out.println(" --- END GRAPHPATH DUMP ---");
        System.out.println("Total meters walked in the preceding graphpath: " +
               states.getLast().getWalkDistance());
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
