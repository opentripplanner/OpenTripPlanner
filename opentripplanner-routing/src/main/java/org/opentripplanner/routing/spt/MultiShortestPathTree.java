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
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Vertex;

public class MultiShortestPathTree extends AbstractShortestPathTree {
    
    private static final long serialVersionUID = MavenVersion.VERSION.getUID();

    public static final ShortestPathTreeFactory FACTORY = new FactoryImpl();

    private Map<Vertex, List<State>> stateSets;

    public MultiShortestPathTree(RoutingRequest options) {
        super(options);
        stateSets = new IdentityHashMap<Vertex, List<State>>();
    }
    
    public Set<Vertex> getVertices() {
        return stateSets.keySet();
    }

    /****
     * {@link ShortestPathTree} Interface
     ****/

    @Override
    public boolean add(State newState) {
    	Vertex vertex = newState.getVertex();
        List<State> states = stateSets.get(vertex);
        if (states == null) {
            states = new ArrayList<State>();
            stateSets.put(vertex, states);
            states.add(newState);
            return true;
        }
        Iterator<State> it = states.iterator();
        while (it.hasNext()) {
            State oldState = it.next();
            // order is important, because in the case of a tie
            // we want to reject the new state
            if (oldState.dominates(newState))
            	return false;
            if (newState.dominates(oldState))
            	it.remove();
        }
        states.add(newState);
        return true;
    }

	@Override
	public State getState(Vertex dest) {
		Collection<State> states = stateSets.get(dest);
		if (states == null)
			return null;
		State ret = null;
		for (State s : states) {
			if ((ret == null || s.betterThan(ret)) && s.isFinal() && s.allPathParsersAccept()) {
				ret = s;
			}
		}
		return ret;
	}

	@Override
	public List<State> getStates(Vertex dest) {
		return stateSets.get(dest);
	}

	@Override
	public int getVertexCount() {
		return stateSets.keySet().size();
	}

	/**
	 * Check that a state coming out of the queue is still in the Pareto-optimal set for this 
	 * vertex, which indicates that it has not been ruled out as a state on an optimal path.
	 * Many shortest path algorithms will decrease the key of an entry in the priority queue 
	 * when it is updated, or remove it when it is dominated. 
	 * 
	 * When the Fibonacci heap was replaced with a binary heap, the decrease-key operation 
	 * was removed for the same reason: both improve theoretical run time complexity, at the 
	 * cost of high constant factors and more complex code.
	 * 
	 * So there can be dominated (useless) states in the queue. When they come out we want to 
	 * ignore them rather than spend time branching out from them.
	 */
	@Override
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

	public String toString() {
        return "MultiSPT(" + this.stateSets.size() + " vertices)";
    }

    private static final class FactoryImpl implements ShortestPathTreeFactory {
        @Override
        public ShortestPathTree create(RoutingRequest options) {
            return new MultiShortestPathTree(options);
        }
    }

    @Override
    public Collection<State> getAllStates() {
        ArrayList<State> allStates = new ArrayList<State>();
        for (List<State> stateSet : stateSets.values()) {
            allStates.addAll(stateSet);
        }
        return allStates;
    }

}