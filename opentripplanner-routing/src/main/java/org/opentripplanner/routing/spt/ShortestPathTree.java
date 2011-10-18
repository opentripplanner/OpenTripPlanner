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

import java.util.Collection;
import java.util.List;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.Vertex;

/**
 * An interface for classes that track which graph vertices are visited and their associated states,
 * so that decisions can be made about whether new states should be enqueued for later exploration.
 * It also allows states to be retrieved for a given target vertex.
 * 
 * Different implementations of this interface allow the routing algorithm to switch from a basic
 * Dijkstra search (which is sufficient for non-time-dependent modes e.g. bicycle) and the more
 * complex label-setting approaches needed for time-dependent public transit routing.
 * 
 * @author andrewbyrd
 */
public interface ShortestPathTree {

    /**
     * The add method checks a new State to see if it is non-dominated and thus worth visiting
     * later. If so, the method returns 'true' indicating that the state is deemed useful and should
     * be enqueued for later exploration. The method will also perform implementation-specific
     * actions that track dominant or optimal states.
     * 
     * @param s
     *            - the State to add to the SPT, if it is deemed non-dominated
     * @return a boolean value indicating whether the state was added to the tree and should
     *         therefore be enqueued
     */
    public boolean add(State s);

    /**
     * The visit method should generally be called upon extracting a State from a priority queue. It
     * checks whether the State is still worth visiting (i.e. whether it has been dominated since it
     * was enqueued) and informs the ShortestPathTree that this State's outgoing edges have been
     * relaxed.
     * 
     * Note: This is necessary because OTP priority queues are not required to implement the
     * decrease-key operation. In fact, decrease-key is not relevant when it is possible to have
     * several States per vertex, as in multi-criteria time-dependent routing. Since a state may
     * remain in the priority queue after being dominated, such sub-optimal States must be caught as
     * they come out of the queue to avoid unnecessary branching.
     * 
     * @param s
     *            - the state about to be visited
     * @return - whether this state is still considered worth visiting.
     */
    public boolean visit(State s);

    /**
     * Returns a collection of 'interesting' states for the given Vertex. Depending on the
     * implementation, this could contain a single optimal state, a set of Pareto-optimal states, or
     * even states that are not known to be optimal but are judged interesting by some other
     * criteria.
     * 
     * @param dest
     *            - the vertex of interest
     * @return a collection of 'interesting' states at that vertex
     */
    public List<? extends State> getStates(Vertex dest);

    /**
     * Returns the 'best' state for the given Vertex, where 'best' depends on the implementation.
     * 
     * @param dest
     *            - the vertex of interest
     * @return a 'best' state at that vertex
     */
    public State getState(Vertex dest);

    /**
     * This should probably be somewhere else, but leaving it here for now for backward compat.
     * 
     * @param dest
     * @param optimize
     * @param options
     * @return
     */
    public List<GraphPath> getPaths(Vertex dest, boolean optimize);

    public GraphPath getPath(Vertex dest, boolean optimize);

    /**
     * How many vertices are referenced in this SPT?
     * 
     * @return number of vertices
     */
    int getVertexCount();

    public Collection<? extends State> getAllStates();

    /** Visit a vertex after it has been settled */
    public void postVisit(State u);

}