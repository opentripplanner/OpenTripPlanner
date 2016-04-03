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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * This class keeps track which graph vertices have been visited and their associated states,
 * so that decisions can be made about whether new states should be enqueued for later exploration.
 * It also allows states to be retrieved for a given target vertex.
 * 
 * We no longer have different implementations of ShortestPathTree because the label-setting (multi-state) approach
 * used in public transit routing, turn restrictions, bike rental, etc. is a generalization of the basic Dijkstra 
 * (single-state) approach. It is much more straightforward to use the more general SPT implementation in all cases.
 *
 * Note that turn restrictions make all searches multi-state; however turn restrictions do not apply when walking.
 * The turn restriction handling is done in the base dominance function implementation, and applies to all subclasses.
 * It essentially splits each vertex into N vertices depending on the incoming edge being taken.
 */
public class ShortestPathTree {

    private static final Logger LOG = LoggerFactory.getLogger(ShortestPathTree.class);

    public final RoutingRequest options;

    public final DominanceFunction dominanceFunction;

    private Map<Vertex, List<State>> stateSets;

    public ShortestPathTree (RoutingRequest options, DominanceFunction dominanceFunction) {
        this.options = options;
        this.dominanceFunction = dominanceFunction;
        stateSets = new IdentityHashMap<Vertex, List<State>>();
    }

    /** @return a list of GraphPaths, sometimes empty but never null. */
    public List<GraphPath> getPaths(Vertex dest, boolean optimize) {
        List<? extends State> stateList = getStates(dest);
        if (stateList == null)
            return Collections.emptyList();
        List<GraphPath> ret = new LinkedList<GraphPath>();
        for (State s : stateList) {
            if (s.isFinal()) {
                ret.add(new GraphPath(s, optimize));
            }
        }
        return ret;
    }

    /** @return a default set of back-optimized paths to the target vertex. */
    public List<GraphPath> getPaths() { return getPaths(options.getRoutingContext().target, true); }

    /** @return a single optimal, optionally back-optimized path to the given vertex. */
    public GraphPath getPath(Vertex dest, boolean optimize) {
        State s = getState(dest);
        if (s == null) {
            return null;
        } else {
            return new GraphPath(s, optimize);
        }
    }

    /** @return the routing context for the search that produced this tree */
    public RoutingRequest getOptions() {
        return options;
    }
    
    /** Print out a summary of the number of states and vertices. */
    public void dump() {
        Multiset<Integer> histogram = HashMultiset.create();
        int statesCount = 0;
        int maxSize = 0;
        for (Map.Entry<Vertex, List<State>> kv : stateSets.entrySet()) {
            List<State> states = kv.getValue();
            int size = states.size();
            histogram.add(size);
            statesCount += size;
            if (size > maxSize) {
                maxSize = size;
            }
        }
        LOG.info("SPT: vertices: " + stateSets.size() + " states: total: "
                + statesCount + " per vertex max: " + maxSize + " avg: "
                + (statesCount * 1.0 / stateSets.size()));
        List<Integer> nStates = new ArrayList<Integer>(histogram.elementSet());
        Collections.sort(nStates);
        for (Integer nState : nStates) {
            LOG.info(nState + " states: " + histogram.count(nState) + " vertices.");
        }
    }

    public Set<Vertex> getVertices() {
        return stateSets.keySet();
    }

    /**
     * The add method checks a new State to see if it is non-dominated and thus worth visiting
     * later. If so, the method returns 'true' indicating that the state is deemed useful and should
     * be enqueued for later exploration. The method will also perform implementation-specific
     * actions that track dominant or optimal states.
     *
     * @param newState the State to add to the SPT, if it is deemed non-dominated
     * @return a boolean value indicating whether the state was added to the tree and should
     *          therefore be enqueued
     */
    public boolean add(State newState) {
        Vertex vertex = newState.getVertex();
        List<State> states = stateSets.get(vertex);

        // if the vertex has no states, add one and return
        if (states == null) {
            states = new ArrayList<>();
            stateSets.put(vertex, states);
            states.add(newState);
            return true;
        }

        // if the vertex has any states that dominate the new state, don't add the state
        // if the new state dominates any old states, remove them
        Iterator<State> it = states.iterator();
        while (it.hasNext()) {
            State oldState = it.next();
            // order is important, because in the case of a tie
            // we want to reject the new state
            if (dominanceFunction.betterOrEqualAndComparable(oldState, newState))
                return false;
            if (dominanceFunction.betterOrEqualAndComparable(newState, oldState))
                it.remove();
        }

        // any states remaining are co-dominant with the new state
        states.add(newState);
        return true;
    }

    /**
     * Returns the 'best' state for the given Vertex, where 'best' depends on the implementation.
     *
     * @param dest the vertex of interest
     * @return a 'best' state at that vertex
     */
    public State getState(Vertex dest) {
        Collection<State> states = stateSets.get(dest);
        if (states == null)
            return null;
        State ret = null;
        // TODO are we only checking path parser acceptance when we fetch states via this specific method?
        for (State s : states) {
            if ((ret == null || s.weight < ret.weight) && s.isFinal()) {
                ret = s;
            }
        }
        return ret;
    }

    /**
     * Returns a collection of 'interesting' states for the given Vertex. Depending on the
     * implementation, this could contain a single optimal state, a set of Pareto-optimal states, or
     * even states that are not known to be optimal but are judged interesting by some other
     * criteria.
     *
     * @param dest the vertex of interest
     * @return a collection of 'interesting' states at that vertex
     */
    public List<State> getStates(Vertex dest) {
        return stateSets.get(dest);
    }

    /** @return number of vertices referenced in this SPT */
    public int getVertexCount() {
        return stateSets.keySet().size();
    }

    /**
     * The visit method should be called upon extracting a State from a priority queue. It
     * checks whether the State is still worth visiting (i.e. whether it has been dominated since it
     * was enqueued) and informs the ShortestPathTree that this State's outgoing edges have been
     * relaxed. A state may remain in the priority queue after being dominated, and such sub-optimal 
     * states must be caught as they come out of the queue to avoid unnecessary branching.
     * 
     * So this function checks that a state coming out of the queue is still in the Pareto-optimal set for this vertex, 
     * which indicates that it has not been ruled out as a state on an optimal path. Many shortest 
     * path algorithms will decrease the key of a vertex in the priority queue when it is updated, but we store states
     * in the queue rather than vertices, and states do not get updated or change their weight.
     * TODO consider just removing states from the priority queue.
     *
     * When the Fibonacci heap was replaced with a binary heap, the decrease-key operation was 
     * removed for the same reason: both improve theoretical run time complexity, at the cost of 
     * high constant factors and more complex code.
     *
     * So there can be dominated (useless) states in the queue. When they come out we want to 
     * ignore them rather than spend time branching out from them.
     *
     * @param state - the state about to be visited
     * @return - whether this state is still considered worth visiting.
     */
    public boolean visit(State state) {
        boolean ret = false;
        for (State s : stateSets.get(state.getVertex())) {
            if (s == state) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    /** @return every state in this tree */
    public Collection<State> getAllStates() {
        ArrayList<State> allStates = new ArrayList<State>();
        for (List<State> stateSet : stateSets.values()) {
            allStates.addAll(stateSet);
        }
        return allStates;
    }

    public String toString() {
        return "ShortestPathTree(" + this.stateSets.size() + " vertices)";
    }

}