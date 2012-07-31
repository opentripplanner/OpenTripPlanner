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
import org.opentripplanner.routing.graph.AbstractVertex;
import org.opentripplanner.routing.graph.Vertex;

public class ArrayMultiShortestPathTree extends AbstractShortestPathTree {

    private static final long serialVersionUID = MavenVersion.VERSION.getUID();

    public static final ShortestPathTreeFactory FACTORY = new FactoryImpl();

    private Map<Vertex, List<State>> stateSets;

    private List<State>[] stateSetArray;

    private int maxIndex;

    @SuppressWarnings("unchecked")
    public ArrayMultiShortestPathTree(RoutingRequest options) {
        super(options);
        stateSets = new IdentityHashMap<Vertex, List<State>>();
        maxIndex = AbstractVertex.getMaxIndex();
        stateSetArray = new List[maxIndex];
    }

    public Set<Vertex> getVertices() {
        throw new UnsupportedOperationException();
    }

    /****
     * {@link ShortestPathTree} Interface
     ****/

    @Override
    public boolean add(State newState) {
        Vertex vertex = newState.getVertex();
        int index = vertex.getIndex();
        List<State> states;
        if (index < maxIndex) {
            states = stateSetArray[index];
            if (states == null) {
                states = new ArrayList<State>();
                stateSetArray[index] = states;
                states.add(newState);
                return true;
            }
        } else {
            states = stateSets.get(vertex);
            if (states == null) {
                states = new ArrayList<State>();
                stateSets.put(vertex, states);
                states.add(newState);
                return true;
            }
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
        List<State> states = getStates(dest);
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
        int index = dest.getIndex();
        if (index < maxIndex) {
            return stateSetArray[index];
        } else {
            return stateSets.get(dest);
        }
    }

    @Override
    public int getVertexCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean visit(State state) {
        boolean ret = false;
        for (State s : getStates(state.getVertex())) {
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
            return new ArrayMultiShortestPathTree(options);
        }
    }

    @Override
    public Collection<State> getAllStates() {
        ArrayList<State> allStates = new ArrayList<State>();
        for (List<State> stateSet : stateSets.values()) {
            allStates.addAll(stateSet);
        }
        for (List<State> stateSet : stateSetArray) {
            if (stateSet == null) continue;
            allStates.addAll(stateSet);
        }
        return allStates;
    }

}