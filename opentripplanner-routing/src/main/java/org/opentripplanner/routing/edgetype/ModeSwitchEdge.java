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

package org.opentripplanner.routing.edgetype;

import lombok.Getter;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

/**
 * An edge that allows switching modes, e.g. parking, renting or returning a bike or automobile.
 * @author mattwigway
 */
public class ModeSwitchEdge extends Edge {
    private static final long serialVersionUID = 20130410L;

    /** The modes that this edge switches from and to */
    @Getter
    private TraverseMode fromMode, toMode;
    
    /** The cost of switching */
    @Getter
    private float traverseWeight;
    
    /** The time, in seconds, to switch modes */
    @Getter
    private int traverseTime;
    
    /**
     * Create a new mode switching edge
     * @param v The vertex to which to attach (ModeSwitchEdges are loop edges) 
     * @param fromMode The mode that the edge switches from
     * @param toMode The mode that the edge switches to
     * @param traverseCost The traversal cost of the edge
     * @param traverseTime The traversal time of the edge, in seconds
     */
    public ModeSwitchEdge(Vertex v, TraverseMode fromMode, TraverseMode toMode, float traverseWeight, int traverseTime) {
        super(v, v);
        this.fromMode = fromMode;
        this.toMode = toMode;
        this.traverseWeight = traverseWeight;
        this.traverseTime = traverseTime;
    }

    @Override
    public State traverse(State s0) {
        // we use non-transit mode because this is never called when the user is on transit
        TraverseMode mode = s0.getNonTransitMode();
        RoutingRequest options = s0.getOptions();
        TraverseModeSet modes = options.getModes();
        boolean arriveBy = options.isArriveBy();
        
        // Checks: Make sure that the mode we are switching to (the toMode in a forward search)
        // is in the allowed modes, and that the current mode matches the mode we are switching
        // (the fromMode in a forward search). Everything is backwards in a reverse search.
        if (!arriveBy) {
            if (!this.fromMode.equals(mode) || !modes.contains(toMode)) {
                return null;
            }
        }
        
        if (arriveBy) {
            if (!this.toMode.equals(mode) || !modes.contains(fromMode)) {
                return null;
            }
        }
        
        StateEditor s1 = s0.edit(this);
        
        if (arriveBy)
            s1.setNonTransitMode(fromMode);
        else
            s1.setNonTransitMode(toMode);
        
        s1.incrementWeight(traverseWeight);
        s1.incrementTimeInSeconds(traverseTime);
        
        return s1.makeState();
        
    }

    @Override
    public String getName() {
        return "ModeSwitchEdge<" + fromMode + " to " + toMode + ">";
    }

}
