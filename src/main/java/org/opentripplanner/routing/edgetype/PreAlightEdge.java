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

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopArrive;

/**
 * PreBoard edges connect a TransitStop to its agency_stop_depart vertices; PreAlight edges connect
 * agency_stop_arrive vertices to a TransitStop.
 *
 * Applies the local stop rules (see TransitStop.java and LocalStopFinder.java) as well as transfer
 * limits, timed and preferred transfer rules, transfer penalties, and boarding costs. This avoids
 * applying these costs/rules repeatedly in (Pattern)Board edges. These are single station or
 * station-to-station specific costs, rather than trip-pattern specific costs.
 */
public class PreAlightEdge extends FreeEdge implements StationEdge {

    private static final long serialVersionUID = -8046937388471651897L;

    public PreAlightEdge(TransitStopArrive from, TransitStop to) {
        super(from, to);
        if (!(to instanceof TransitStop))
            throw new IllegalStateException("Pre-alight edges must lead to a transit stop.");
    }

    @Override
    public State traverse(State s0) {
        RoutingRequest options = s0.getOptions();
        // TODO: this could probably be fused with PreBoardEdge now (AMB)
        // they are currently only different because the StateEditor.incrementTime methods are not
        // used.

        // Ignore this edge if its stop is banned
        if (!options.bannedStops.isEmpty()) {
            if (options.bannedStops.matches(((TransitStop) tov).getStop())) {
                return null;
            }
        }
        if (!options.bannedStopsHard.isEmpty()) {
            if (options.bannedStopsHard.matches(((TransitStop) tov).getStop())) {
                return null;
            }
        }
        
        if (options.arriveBy) {
            /* Backward traversal: apply stop(pair)-specific costs */
            // Do not pre-board if transit modes are not selected.
            // Return null here rather than in StreetTransitLink so that walk-only
            // options can be used to find transit stops without boarding vehicles.
            if (!options.modes.isTransit())
                return null;

            TransitStop toVertex = (TransitStop) getToVertex();

            // If we've hit our transfer limit, don't go any further
            if (s0.getNumBoardings() > options.maxTransfers)
                return null;

            /* apply transfer rules */
            /*
             * look in the global transfer table for the rules from the previous stop to this stop.
             */
            long t0 = s0.getTimeSeconds();
            long slack;
            if (s0.isEverBoarded()) {
                slack = options.transferSlack - options.boardSlack;
            } else {
                slack = options.alightSlack;
            }
            long alight_before = t0 - slack;
            int transfer_penalty = 0;

            // penalize transfers more heavily if requested by the user
            if (s0.isEverBoarded()) {
                // this is not the first boarding, therefore we must have "transferred" -- whether
                // via a formal transfer or by walking.
                transfer_penalty += options.transferPenalty;
            }

            StateEditor s1 = s0.edit(this);
            s1.setTimeSeconds(alight_before);
            long wait_cost = t0 - alight_before;
            s1.incrementWeight(wait_cost + transfer_penalty);
            s1.setBackMode(getMode());
            return s1.makeState();
        } else {
            /* Forward traversal: not so much to do */
            StateEditor s1 = s0.edit(this);
            TransitStop toVertex = (TransitStop) getToVertex();
            s1.alightTransit();
            s1.incrementTimeInSeconds(options.alightSlack);
            s1.setBackMode(getMode());
            return s1.makeState();
        }
    }

    public TraverseMode getMode() {
        return TraverseMode.LEG_SWITCH;
    }

    public State optimisticTraverse(State s0) {
        // do not include minimum transfer time in heuristic weight
        // (it is path-dependent)
        StateEditor s1 = s0.edit(this);
        s1.setBackMode(getMode());
        return s1.makeState();
    }

    public String toString() {
        return "PreAlightEdge at stop " + tov;
    }

}
