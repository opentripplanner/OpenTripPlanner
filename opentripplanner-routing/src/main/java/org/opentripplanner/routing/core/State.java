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

package org.opentripplanner.routing.core;

import java.util.Arrays;
import java.util.Date;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.algorithm.NegativeWeightException;
import org.opentripplanner.routing.automata.AutomatonState;
import org.opentripplanner.routing.edgetype.OnBoardForwardEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.pathparser.PathParser;

public class State implements Cloneable {

    /* Data which is likely to change at most traversals */
    // the current time at this state, in seconds
    protected long time;

    // accumulated weight up to this state
    protected double weight;

    // associate this state with a vertex in the graph
    protected Vertex vertex;

    // allow path reconstruction from states
    protected State backState;

    protected Edge backEdge;

    protected EdgeNarrative backEdgeNarrative;

    // how many edges away from the initial state
    protected int hops;

    // allow traverse result chaining (multiple results)
    protected State next;

    /* StateData contains data which is unlikely to change as often */
    public StateData stateData;

    // how far have we walked
    protected double walkDistance;

    // track the states of all path parsers -- probably changes frequently
    protected int[] pathParserStates;
    
    /* CONSTRUCTORS */

    /**
     * Create an initial state representing the beginning of a search for the given routing context. 
     * Initial "parent-less" states can only be created at the beginning of a trip. elsewhere, all 
     * states must be created from a parent and associated with an edge.
     */
    public State(RoutingRequest opt) {
        this (opt.rctx.origin, opt.getSecondsSinceEpoch(), opt);
    }

    /**
     * Create an initial state, forcing vertex to the specified value. Useful for reusing a 
     * RoutingContext in TransitIndex, tests, etc.
     */
    public State(Vertex vertex, RoutingRequest opt) {
        this(vertex, opt.getSecondsSinceEpoch(), opt);
    }

    /**
     * Create an initial state, forcing vertex and time to the specified values. Useful for reusing 
     * a RoutingContext in TransitIndex, tests, etc.
     */
    public State(Vertex vertex, long time, RoutingRequest options) {
        this.weight = 0;
        this.vertex = vertex;
        this.backState = null;
        this.backEdge = null;
        this.backEdgeNarrative = null;
        this.hops = 0;
        this.stateData = new StateData();
        // note that here we are breaking the circular reference between rctx and options
        // this should be harmless since reversed clones are only used when routing has finished
        this.stateData.opt = options;
        this.stateData.startTime = time;
        this.stateData.usingRentedBike = false;
        this.time = time;
        if (options.rctx != null) {
        	this.pathParserStates = new int[options.rctx.pathParsers.length];
        	Arrays.fill(this.pathParserStates, AutomatonState.START);
        }
        stateData.routeSequence = new AgencyAndId[0];
    }

    /**
     * Create a state editor to produce a child of this state, which will be the result of
     * traversing the given edge.
     * 
     * @param e
     * @return
     */
    public StateEditor edit(Edge e) {
        return new StateEditor(this, e);
    }

    public StateEditor edit(Edge e, EdgeNarrative en) {
        return new StateEditor(this, e, en);
    }

    protected State clone() {
        State ret;
        try {
            ret = (State) super.clone();
        } catch (CloneNotSupportedException e1) {
            throw new IllegalStateException("This is not happening");
        }
        return ret;
    }

    /*
     * FIELD ACCESSOR METHODS States are immutable, so they have only get methods. The corresponding
     * set methods are in StateEditor.
     */

    /**
     * Retrieve a State extension based on its key.
     * 
     * @param key - An Object that is a key in this State's extension map
     * @return - The extension value for the given key, or null if not present
     */
    public Object getExtension(Object key) {
        if (stateData.extensions == null) {
            return null;
        }
        return stateData.extensions.get(key);
    }

    public String toString() {
        return "<State " + new Date(getTimeInMillis()) + " [" + weight + "] " + (isBikeRenting() ? "BIKE_RENT " : "") + vertex + ">";
    }
    
    public String toStringVerbose() {
        return "<State " + new Date(getTimeInMillis()) + 
                " w=" + this.getWeight() + 
                " t=" + this.getElapsedTime() + 
                " d=" + this.getWalkDistance() + 
                " b=" + this.getNumBoardings();
    }
    
    /** Returns time in seconds since epoch */
    public long getTime() {
        return this.time;
    }

    /** returns the length of the trip in seconds up to this state */
    public long getElapsedTime() {
        return Math.abs(this.time - stateData.startTime);
    }

    /** returns the length of the trip in seconds up to this time, not including the initial wait.
        It subtracts out the initial wait or a clamp value specified in the request.
        This is used in lieu of reverse optimization in Analyst. */
    public long getActiveTime () {
        long clampInitialWait = stateData.opt.clampInitialWait;

        long initialWait = stateData.initialWaitTime;

        // only subtract up the clamp value
        if (clampInitialWait > 0 && initialWait > clampInitialWait)
            initialWait = clampInitialWait;            

        long activeTime = getElapsedTime() - initialWait;
        // TODO: what should be done here?
        if (activeTime < 0)
            activeTime = getElapsedTime();

        return activeTime;            
    }

    public int getTrip() {
        return stateData.trip;
    }

    public AgencyAndId getTripId() {
        return stateData.tripId;
    }

    public String getZone() {
        return stateData.zone;
    }

    public AgencyAndId getRoute() {
        return stateData.route;
    }

    public int getNumBoardings() {
        return stateData.numBoardings;
    }

    public boolean isAlightedLocal() {
        return stateData.alightedLocal;
    }

    /**
     * Whether this path has ever previously boarded (or alighted from, in a reverse search) a
     * transit vehicle
     */
    public boolean isEverBoarded() {
        return stateData.everBoarded;
    }

    public boolean isBikeRenting() {
        return stateData.usingRentedBike;
    }

    /**
     * @return True if the state at vertex can be the end of path.
     */
    public boolean isFinal() {
        return !isBikeRenting();
    }

    public Vertex getPreviousStop() {
        return stateData.previousStop;
    }

    public long getLastAlightedTime() {
        return stateData.lastAlightedTime;
    }

    public NoThruTrafficState getNoThruTrafficState() {
        return stateData.noThruTrafficState;
    }

    public double getWalkDistance() {
        return walkDistance;
    }

    public Vertex getVertex() {
        return this.vertex;
    }

    /**
     * Multicriteria comparison. Returns true if this state is better than the other one (or equal)
     * both in terms of time and weight.
     */
    public boolean dominates(State other) {
        if (other.weight == 0) {
            return false;
        }
        // Multi-state (bike rental) - no domination for different states
        if (isBikeRenting() != other.isBikeRenting())
            return false;

        if (this.similarRouteSequence(other)) {
            return this.weight <= other.weight;
        }

        double weightDiff = this.weight / other.weight;
        return walkDistance <= other.getWalkDistance() * 1.05
                && (weightDiff < 1.02 && this.weight - other.weight < 30)
                && this.getElapsedTime() - other.getElapsedTime() <= 30;
    }

    /**
     * Returns true if this state's weight is lower than the other one. Considers only weight and
     * not time or other criteria.
     */
    public boolean betterThan(State other) {
        return this.weight < other.weight;
    }

    public double getWeight() {
        return this.weight;
    }

    public int getTimeDeltaSec() {
        return (int) (this.time - backState.time);
    }

    public int getAbsTimeDeltaSec() {
        return (int) Math.abs(this.time - backState.time);
    }

    public double getWeightDelta() {
        return this.weight - backState.weight;
    }

    public void checkNegativeWeight() {
        double dw = this.weight - backState.weight;
        if (dw < 0) {
            throw new NegativeWeightException(String.valueOf(dw) + " on edge " + backEdge);
        }
    }

    public boolean isOnboard() {
        return this.backEdge instanceof OnBoardForwardEdge;
    }

    public EdgeNarrative getBackEdgeNarrative() {
        return this.backEdgeNarrative;
    }

    public State getBackState() {
        return this.backState;
    }

    public Edge getBackEdge() {
        return this.backEdge;
    }

    public boolean exceedsHopLimit(int maxHops) {
        return hops > maxHops;
    }

    public boolean exceedsWeightLimit(double maxWeight) {
        return weight > maxWeight;
    }

    public long getStartTime() {
        return stateData.startTime;
    }

    /**
     * Optional next result that allows {@link Edge} to return multiple results from
     * {@link Edge#traverse(State, RoutingRequest)} or
     * {@link Edge#traverseBack(State, RoutingRequest)}
     * 
     * @return the next additional result from an edge traversal, or null if no more results
     */
    public State getNextResult() {
        return next;
    }

    /**
     * Extend an exiting result chain by appending this result to the existing chain. The usage
     * model looks like this:
     * 
     * <code>
     * TraverseResult result = null;
     * 
     * for( ... ) {
     *   TraverseResult individualResult = ...;
     *   result = individualResult.addToExistingResultChain(result);
     * }
     * 
     * return result;
     * </code>
     * 
     * @param existingResultChain the tail of an existing result chain, or null if the chain has not
     *        been started
     * @return
     */
    public State addToExistingResultChain(State existingResultChain) {
        if (this.getNextResult() != null)
            throw new IllegalStateException("this result already has a next result set");
        next = existingResultChain;
        return this;
    }

    public State detachNextResult() {
        State ret = this.next;
        this.next = null;
        return ret;
    }

    public RoutingContext getContext() {
        return stateData.opt.rctx;
    }

    public RoutingRequest getOptions () {
        return stateData.opt;
    }
    
    public TraverseMode getNonTransitMode(RoutingRequest options) {
        TraverseModeSet modes = options.getModes();
        if (modes.getCar())
            return TraverseMode.CAR;
        if (modes.getWalk() && !isBikeRenting())
            return TraverseMode.WALK;
        if (modes.getBicycle())
            return TraverseMode.BICYCLE;
        if (modes.getWalk())
            return TraverseMode.WALK;
        return null;
    }

    public State reversedClone() {
        // We no longer compensate for schedule slack (minTransferTime) here.
        // It is distributed symmetrically over all preboard and prealight edges.
        return new State(this.vertex, this.time, stateData.opt.reversedClone());
    }

    public void dumpPath() {
        System.out.printf("---- FOLLOWING CHAIN OF STATES ----\n");
        State s = this;
        while (s != null) {
            System.out.printf("%s via %s \n", s, s.backEdgeNarrative);
            s = s.backState;
        }
        System.out.printf("---- END CHAIN OF STATES ----\n");
    }

    public long getTimeInMillis() {
        return time * 1000;
    }

    public boolean similarRouteSequence(State that) {
        AgencyAndId[] rs0 = this.stateData.routeSequence;
        AgencyAndId[] rs1 = that.stateData.routeSequence;
        if (rs0 == rs1)
            return true;
        int n = rs0.length < rs1.length ? rs0.length : rs1.length;
        for (int i = 0; i < n; i++)
            if (rs0[i] != rs1[i])
                return false;
        return true;
    }

    public double getWalkSinceLastTransit() {
        return walkDistance - stateData.lastTransitWalk;
    }

    public double getWalkAtLastTransit() {
        return stateData.lastTransitWalk;
    }

    public boolean multipleOptionsBefore() {
        boolean foundAlternatePaths = false;
        TraverseMode requestedMode = getNonTransitMode(getOptions());
        for (Edge out : backState.vertex.getOutgoing()) {
            if (out == backEdge) {
                continue;
            }
            if (!(out instanceof StreetEdge)) {
                continue;
            }
            State outState = out.traverse(backState);
            if (outState == null) {
                continue;
            }
            if (!outState.getBackEdgeNarrative().getMode().equals(requestedMode)) {
                //walking a bike, so, not really an exit
                continue;
            }
            // this section handles the case of an option which is only an option if you walk your
            // bike. It is complicated because you will not need to walk your bike until one
            // edge after the current edge.

            //now, from here, try a continuing path.
            Vertex tov = outState.getVertex();
            boolean found = false;
            for (Edge out2 : tov.getOutgoing()) {
                State outState2 = out2.traverse(outState);
                if (outState2 != null && !outState2.getBackEdgeNarrative().getMode().equals(requestedMode)) {
                    // walking a bike, so, not really an exit
                    continue;
                }
                found = true;
                break;
            }
            if (!found) {
                continue;
            }

            // there were paths we didn't take.
            foundAlternatePaths = true;
            break;
        }
        return foundAlternatePaths;
    }
    
    public boolean allPathParsersAccept() {
    	PathParser[] parsers = this.stateData.opt.rctx.pathParsers;
    	for (int i = 0; i < parsers.length; i++)
    		if ( ! parsers[i].accepts(pathParserStates[i]))
    			return false;
    	return true;
	}
    		
	public String getPathParserStates() {
		StringBuilder sb = new StringBuilder();
		sb.append("( ");
		for (int i : pathParserStates)
			sb.append(String.format("%02d ", i));
		sb.append(")");
		return sb.toString();
	}

    public TripPattern getLastPattern() {
        return stateData.lastPattern;
    }

    public ServiceDay getServiceDay() {
        return stateData.serviceDay;
    }

    public void setServiceDay(ServiceDay sd) {
        stateData.serviceDay = sd;
    }

}