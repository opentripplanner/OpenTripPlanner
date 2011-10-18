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

import java.util.Date;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.algorithm.NegativeWeightException;
import org.opentripplanner.routing.edgetype.OnBoardForwardEdge;

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
    protected StateData stateData;

    private double lastTransitWalk = 0;

    /* CONSTRUCTORS */

    /**
     * Create a state representing the beginning of a trip at the given vertex, at the current
     * system time.
     */
    public State(Vertex v, TraverseOptions opt) {
        this((int) (System.currentTimeMillis() / 1000), v, opt);
    }

    /**
     * Create a state representing the beginning of a search at the given vertex, at the given time.
     * 
     * @param time - The time at which the search will start
     * @param v - The origin vertex of the search
     */
    public State(long time, Vertex vertex, TraverseOptions opt) {
        // parent-less states can only be created at the beginning of a trip.
        // elsewhere, all states must be created from a parent
        // and associated with an edge.

        this.time = time;
        this.weight = 0;
        this.vertex = vertex;
        this.backState = null;
        this.backEdge = null;
        this.backEdgeNarrative = null;
        this.hops = 0;
        this.stateData = new StateData();
        stateData.options = opt;
        stateData.startTime = time;
        stateData.tripSeqHash = 0;
        // System.out.printf("new state %d %s %s \n", this.time, this.vertex, stateData.options);
    }

    public State createState(long time, Vertex vertex, TraverseOptions options) {
        return new State(time, vertex, options);
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
        return stateData.extensions.get(key);
    }

    public String toString() {
        return "<State " + new Date(getTimeInMillis()) + " [" + weight + "] " + vertex + ">";
    }

    public long getTime() {
        return this.time;
    }

    public long getElapsedTime() {
        return Math.abs(this.time - stateData.startTime);
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

    public boolean isEverBoarded() {
        return stateData.everBoarded;
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
        return stateData.walkDistance;
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

        if (this.similarTripSeq(other)) {
            return this.weight <= other.weight;
        }

        double weightDiff = this.weight / other.weight;
        return (weightDiff < 1.02 && this.weight - other.weight < 30) && this.getElapsedTime() - other.getElapsedTime() <= 30;
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
     * {@link Edge#traverse(State, TraverseOptions)} or
     * {@link Edge#traverseBack(State, TraverseOptions)}
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

    public TraverseOptions getOptions() {
        return stateData.options;
    }

    public State reversedClone() {
        // We no longer compensate for schedule slack (minTransferTime) here.
        // It is distributed symmetrically over all preboard and prealight edges.
        return createState(this.time, this.vertex, stateData.options.reversedClone());
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

    public boolean similarTripSeq(State existing) {
        return this.stateData.tripSeqHash == existing.stateData.tripSeqHash;
    }

    public double getWalkSinceLastTransit() {
        return stateData.walkDistance - lastTransitWalk ;
    }

    public double getWalkAtLastTransit() {
        return lastTransitWalk;
    }

    public void setWalkAtLastTransit(double lastTransitWalk) {
        this.lastTransitWalk = lastTransitWalk;
    }
}
