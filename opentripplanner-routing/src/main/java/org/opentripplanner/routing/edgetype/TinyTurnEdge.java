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

import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Vertex;

/**
 * A turn edge which includes no non-turn street -- it takes you from one vertex at an intersection
 * to another vertex at the same intersection.
 * 
 */
public class TinyTurnEdge extends FreeEdge {

    private static final long serialVersionUID = 3925814840369402222L;

    private TraverseModeSet restrictedModes;

    private int turnCost = 0;
    
    private StreetTraversalPermission permission;

    private TurnEdge replaces;

    public TinyTurnEdge(Vertex from, Vertex to, TurnEdge turnEdge) {
        super(from, to);
        this.replaces = turnEdge;
        this.permission = replaces.getPermission();
    }
    
    public String getName() {
        return replaces.getName();
    }

    public boolean canTraverse(RoutingRequest options, TraverseMode mode) {
        if (mode == TraverseMode.WALK && permission.allows(StreetTraversalPermission.PEDESTRIAN)) {
            return true;
        }

        if (mode == TraverseMode.BICYCLE && permission.allows(StreetTraversalPermission.BICYCLE)) {
            return true;
        }

        if (mode == TraverseMode.CAR && permission.allows(StreetTraversalPermission.CAR)) {
            return true;
        }

        return false;
    }

    @Override
    public State traverse(State s0) {
        RoutingRequest options = s0.getOptions();
        TraverseMode traverseMode = s0.getNonTransitMode(options);

        if (!canTraverse(options, traverseMode)) {
            if (traverseMode == TraverseMode.BICYCLE) {
            	// try walking bike since you can't ride here
                return doTraverse(s0, options.getWalkingOptions());
            }
            return null;
        }
        return doTraverse(s0, options);
    }
    
    private boolean turnRestricted(State s0, RoutingRequest options) {
        if (restrictedModes == null)
            return false;
        else {
            return restrictedModes.contains(s0.getNonTransitMode(options));
        }
    }

    public State doTraverse(State s0, RoutingRequest options) {
        if (turnRestricted(s0, options) && !options.getModes().getWalk()) {
            return null;
        }
        TraverseMode traverseMode = s0.getNonTransitMode(options);
        double speed = options.getSpeed(traverseMode);
        double angleLength = turnCost / 20.0;
        double time = angleLength / speed;
        double weight = time * options.walkReluctance + turnCost / 20;
        EdgeNarrative en = new FixedModeEdge(this, traverseMode);
        StateEditor s1 = s0.edit(this, en);
        s1.incrementWeight(weight);
        s1.incrementTimeInSeconds((int) Math.ceil(time));
        return s1.makeState();
    }

    public String toString() {
        return "TinyTurnEdge(" + fromv + " -> " + tov + ")";
    }

    public void setTurnCost(int turnCost) {
        this.turnCost = turnCost;
    }

    public void setRestrictedModes(TraverseModeSet restrictedModes) {
        this.restrictedModes = restrictedModes;
    }

    public TraverseModeSet getRestrictedModes() {
        return restrictedModes;
    }

}
