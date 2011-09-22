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
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;

/**
 * A turn edge which includes no non-turn street -- it takes you from one vertex at an intersection
 * to another vertex at the same intersection.
 * 
 */
public class TinyTurnEdge extends FreeEdge {

    private static final long serialVersionUID = 3925814840369402222L;

    private boolean restricted = false;

    private int turnCost = 0;
    
    private StreetTraversalPermission permission;

    public TinyTurnEdge(Vertex from, Vertex to, StreetTraversalPermission permission) {
        super(from, to);
        this.permission = permission;
    }
    
    public boolean canTraverse(TraverseOptions options) {
        if (options.getModes().getWalk() && permission.allows(StreetTraversalPermission.PEDESTRIAN)) {
            return true;
        }

        if (options.getModes().getBicycle() && permission.allows(StreetTraversalPermission.BICYCLE)) {
            return true;
        }

        if (options.getModes().getCar() && permission.allows(StreetTraversalPermission.CAR)) {
            return true;
        }

        return false;
    }

    @Override
    public State traverse(State s0) {
        TraverseOptions options = s0.getOptions();

        if (!canTraverse(options)) {
            if (options.getModes().contains(TraverseMode.BICYCLE)) {
            	// try walking bike since you can't ride here
                return doTraverse(s0, options.getWalkingOptions());
            }
            return null;
        }
        return doTraverse(s0, options);
    }
    
    public State doTraverse(State s0, TraverseOptions options) {
        if (restricted && !options.getModes().contains(TraverseMode.WALK)) {
            return null;
        }
        double angleLength = turnCost / 20.0;
        double time = angleLength / options.speed;
        double weight = time * options.walkReluctance + turnCost / 20;
        EdgeNarrative en = new FixedModeEdge(this, s0.getOptions().getModes().getNonTransitMode());
        StateEditor s1 = s0.edit(this, en);
        s1.incrementWeight(weight);
        s1.incrementTimeInSeconds((int) Math.ceil(time));
        return s1.makeState();
    }

    public boolean equals(Object o) {
        if (o instanceof TinyTurnEdge) {
            TinyTurnEdge other = (TinyTurnEdge) o;
            return other.getFromVertex().equals(fromv) && other.getToVertex().equals(tov);
        }
        return false;
    }

    public String toString() {
        return "TinyTurnEdge(" + fromv + " -> " + tov + ")";
    }

    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }

    public void setTurnCost(int turnCost) {
        this.turnCost = turnCost;
    }
}
