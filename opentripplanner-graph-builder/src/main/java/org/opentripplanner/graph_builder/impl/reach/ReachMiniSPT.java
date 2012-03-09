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

package org.opentripplanner.graph_builder.impl.reach;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.AbstractShortestPathTree;
import org.opentripplanner.routing.spt.ShortestPathTree;

public class ReachMiniSPT extends AbstractShortestPathTree {
    protected static final int DEFAULT_CAPACITY = 500;

    Map<Vertex, ReachState> states;

    Set<ReachState> settled = new HashSet<ReachState>();
    
    protected double epsilon;

    private boolean done = false;

    private ArrayList<ReachState> innerCircle = new ArrayList<ReachState>();

    /**
     * Constructor that uses a default capacity for internal vertex-keyed data
     * structures.
     */
    public ReachMiniSPT(double epsilon) {
        this(DEFAULT_CAPACITY, epsilon);
    }

    /**
     * Constructor with a parameter indicating the initial capacity of the data structures holding
     * vertices. This can help avoid resizing and rehashing these objects during path searches.
     * 
     * @param n - the initial size of vertex-keyed maps
     */
    public ReachMiniSPT(int n, double epsilon) {
        states = new IdentityHashMap<Vertex, ReachState>(n);
        this.epsilon = epsilon;
    }

    @Override
    public Collection<ReachState> getAllStates() {
        return states.values();
    }
    
    public Collection<ReachState> getSettledStates() {
        return settled;
    }
    
    /****
     * {@link ShortestPathTree} Interface
     ****/

    @Override
    public boolean add(State state) {
        Vertex here = state.getVertex();
        State existing = states.get(here);

        if (existing == null || state.getWeight () < existing.getWeight()) {
            states.put(here, (ReachState) state);
            return true;
        } else
            return false;
    }

    @Override
    public List<State> getStates(Vertex dest) {
        State s = states.get(dest);
        if (s == null)
            return Collections.emptyList();
        else
            return Arrays.asList(s); // single-element array-backed list
    }

    @Override
    public ReachState getState(Vertex dest) {
        return states.get(dest);
    }

    @Override
    public void postVisit(State s) {
        ReachState state = (ReachState) s;
        settled.add(state);
        State backState = s.getBackState();
        if (backState != null && s.getWeight() - s.getWeightDelta() >= 2 * epsilon) {
            done = true;
        }

        // is vertex in inner circle?
        if (backState == null || backState.getWeight() < epsilon) {
            innerCircle.add(state);
        }
    }

    @Override
    public int getVertexCount() {
        return states.size();
    }

    @Override
    public boolean visit(State s) {
        return true;
    }

    public boolean shouldSearchContinue() {
        return !done ;
    }

    public void computeChildren() {
        for (ReachState s : states.values()) {
            ReachState backState = (ReachState) s.getBackState();
            if (backState == null) {
                continue;
            }
            backState.addChild(s);
            
            if (!settled.contains(s)) {
                backState.height = Double.MAX_VALUE;
                s.height = Double.MAX_VALUE;
            }
        }
    }
    
    /*
     * The height of v is the distance from v to the highest-weight descendant, or infinity
     * if some descendants are not yet closed.  There's also a penalty from deleted edges.
     */

    public double getHeight(ReachState s, Map<Vertex, Double> outPenalty) {
        if (s.height != null) {
            return s.height;
        }

        Vertex w = s.getVertex();
        Collection<ReachState> wchildren = s.getChildren();
        if (wchildren == null) {
            s.height = 0.0;
            return 0.0;
        }

        double height = 0;
        for (ReachState state : wchildren) {
            if (!settled.contains(state)) {
                s.height = Double.MAX_VALUE;
                return Double.MAX_VALUE;
            }
            height = Math.max(height, getHeight(state, outPenalty) + s.getWeightDelta());
            if (height > epsilon) {
                s.height = Double.MAX_VALUE;
                return Double.MAX_VALUE;
            }
        }
        // now the pseudo-leaf,
        Double penalty = outPenalty.get(w);
        if (penalty == null) {
            penalty = 0.0;
        }
        height = Math.max(height, penalty);
        s.height = height;
        return height;
    }

    public Collection<ReachState> getInnerCircle() {
        return innerCircle;
    }

    public void precomputeHeight(ReachState state, Map<Vertex, Double> outPenalty) {
        
        Stack<ReachState> stack = new Stack<ReachState>();
        stack.push(state);
        while (!stack.isEmpty()) {
            state = stack.peek();
            Double height = state.height;
            ReachState backState = (ReachState) state.getBackState();
            if (height != null) {
                //visit after children
                stack.pop();
                Double maxHeight = outPenalty.get(state.getVertex());
                if (maxHeight != null && maxHeight > height) {
                    height = maxHeight;
                }
                state.height = height;
                if (backState != null) {
                    Double backHeight = backState.height;
                
                    if (backHeight == null || backHeight < height) {
                        backState.height = height;
                    }
                }
                continue;
            }

            Collection<ReachState> children = state.getChildren();
            if (children.size() == 0) {
                state.height = 0.0;
            } else {
                for (ReachState s : children) {
                    stack.push(s);
                }
            }
        }
        
    }


}
