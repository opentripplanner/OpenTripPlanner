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
import org.opentripplanner.routing.vertextype.TransitStopDepart;

/**
 * PreBoard edges connect a TransitStop to its agency_stop_depart vertices; PreAlight edges connect
 * an agency_stop_arrive vertex to its TransitStop.
 * 
 * Applies the local stop rules (see TransitStop.java and LocalStopFinder.java) as well as transfer
 * limits, timed and preferred transfer rules, transfer penalties, and boarding costs. This avoids
 * applying these costs/rules repeatedly in (Pattern)Board edges. These are single station or
 * station-to-station specific costs, rather than trip-pattern specific costs.
 */
public class PreBoardEdge extends FreeEdge {

    private static final long serialVersionUID = -8046937388471651897L;

    public PreBoardEdge(TransitStop from, TransitStopDepart to) {
        super(from, to);
        if (!(from instanceof TransitStop))
            throw new IllegalStateException("Preboard edges must lead out of a transit stop.");
    }

    @Override
    public State traverse(State s0) {
        RoutingRequest options = s0.getOptions();
        
        // Ignore this edge if its stop is banned
        if (!options.getBannedStops().isEmpty() && fromv instanceof TransitStop) {
            if (options.getBannedStops().matches(((TransitStop) fromv).getStop())) {
                return null;
            }
        }
        
        if (options.isArriveBy()) {
            /* Traverse backward: not much to do */
            StateEditor s1 = s0.edit(this);
            TransitStop fromVertex = (TransitStop) getFromVertex();
            if (fromVertex.isLocal()) {
                s1.setAlightedLocal(true);
            }

            //apply board slack
            s1.incrementTimeInSeconds(options.getBoardSlack());
            s1.alightTransit();
            s1.setBackMode(getMode());
            return s1.makeState();
        } else {
            /* Traverse forward: apply stop(pair)-specific costs */

            // Do not pre-board if transit modes are not selected.
            // Return null here rather than in StreetTransitLink so that walk-only
            // options can be used to find transit stops without boarding vehicles.
            if (!options.getModes().isTransit())
                return null;

            TransitStop fromVertex = (TransitStop) getFromVertex();
            // Do not board once one has alighted from a local stop
            if (fromVertex.isLocal() && s0.isEverBoarded()) {
                return null;
            }
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
                slack = options.getTransferSlack() - options.getAlightSlack();
            } else {
                slack = options.getBoardSlack();
            }
            long board_after = t0 + slack;
            long transfer_penalty = 0;

            // penalize transfers more heavily if requested by the user
            if (s0.isEverBoarded()) {
                // this is not the first boarding, therefore we must have "transferred" -- whether
                // via a formal transfer or by walking.
                transfer_penalty += options.transferPenalty;
            }

            StateEditor s1 = s0.edit(this);
            s1.setTimeSeconds(board_after);
            s1.setEverBoarded(true);
            s1.setCurrentStop(fromVertex.getStop());
            long wait_cost = board_after - t0;
            s1.incrementWeight(wait_cost + transfer_penalty);
            s1.setBackMode(getMode());
            return s1.makeState();
        }
    }

    public TraverseMode getMode() {
        return TraverseMode.BOARDING;
    }

    public State optimisticTraverse(State s0) {
        // do not include minimum transfer time in heuristic weight
        // (it is path-dependent)
        StateEditor s1 = s0.edit(this);
        s1.setBackMode(getMode());
        return s1.makeState();
    }

    public String toString() {
        return "preboard edge at stop " + fromv;
    }

}
