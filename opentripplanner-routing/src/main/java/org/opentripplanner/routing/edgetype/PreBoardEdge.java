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
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
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
        TraverseOptions options = s0.getOptions();
        if (options.isArriveBy()) {
            /* Traverse backward: not much to do */
            StateEditor s1 = s0.edit(this);
            TransitStop fromVertex = (TransitStop) getFromVertex();
            if (fromVertex.isLocal()) {
                s1.setAlightedLocal(true);
            }

            s1.incrementTimeInSeconds(options.minTransferTime / 2);
            s1.alightTransit();
            return s1.makeState();
        } else {
            /* Traverse forward: apply stop(pair)-specific costs */

            // Do not pre-board if transit modes are not selected.
            // Return null here rather than in StreetTransitLink so that walk-only
            // options can be used to find transit stops without boarding vehicles.
            if (!options.getModes().isTransit())
                return null;

            // Do not board if the passenger has alighted from a local stop
            if (s0.isAlightedLocal()) {
                return null;
            }
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
            long t0 = s0.getTime();
            long board_after = t0 + options.minTransferTime / 2;
            long transfer_penalty = 0;
            if (s0.getLastAlightedTime() != 0) {
                /* this is a transfer rather than an initial boarding */
                TransferTable transferTable = options.getTransferTable();
                if (transferTable.hasPreferredTransfers()) {
                    // only penalize transfers if there are some that will be depenalized
                    transfer_penalty = options.nonpreferredTransferPenalty;
                }
                int transfer_time = transferTable.getTransferTime(s0.getPreviousStop(),
                        getToVertex());
                if (transfer_time == TransferTable.UNKNOWN_TRANSFER) {
                    // use min transfer time relative to arrival time at this stop
                } else if (transfer_time >= 0) {
                    // handle minimum time transfers (>0) and timed transfers (0)
                    // relative to alight time at last stop
                    long table_board_after = s0.getLastAlightedTime() + transfer_time;
                    // do not let time run backward
                    // this could make timed transfers fail if there is walking involved
                    if (table_board_after > board_after)
                        board_after = table_board_after;
                } else if (transfer_time == TransferTable.FORBIDDEN_TRANSFER) {
                    return null;
                } else if (transfer_time == TransferTable.PREFERRED_TRANSFER) {
                    // depenalize preferred transfers
                    // TODO: verify correctness of this method (AMB)
                    transfer_penalty = 0;
                    // use min transfer time relative to arrival time at this stop
                } else {
                    throw new IllegalStateException("Undefined value in transfer table.");
                }
                if (transfer_time == 0) {
                    // timed transfers are assumed to be preferred
                    transfer_penalty = 0;
                }
            } else {
                /* this is a first boarding, not a transfer - divide minTransferTime in half */
            }

            // penalize transfers more heavily if requested by the user
            if (s0.isEverBoarded()) {
                // this is not the first boarding, therefore we must have "transferred" -- whether
                // via a formal transfer or by walking.
                transfer_penalty += options.transferPenalty;
            }

            StateEditor s1 = s0.edit(this);
            s1.setTime(board_after);
            s1.setEverBoarded(true);
            long wait_cost = board_after - t0;
            s1.incrementWeight(wait_cost + transfer_penalty);
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
        return s1.makeState();
    }

    public String toString() {
        return "preboard edge at stop " + fromv;
    }

}
