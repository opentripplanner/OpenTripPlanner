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

import org.opentripplanner.routing.algorithm.NegativeWeightException;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateData.Editor;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.StateData;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.core.TransitStop;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;

/** Applies the local stop rules (see TransitStop.java and LocalStopFinder.java) 
 *  as well as transfer limits, timed and preferred transfer rules, 
 *  transfer penalties, and boarding costs.
 *  This avoids applying these costs/rules repeatedly in (Pattern)Board edges.
 *  These are single station or station-to-station specific costs, rather than
 *  trip-pattern specific costs.
 */
public class PreAlightEdge extends FreeEdge {

    private static final long serialVersionUID = -8046937388471651897L;

    public PreAlightEdge(Vertex from, Vertex to) {
        super(from, to);
    }

    @Override
    public TraverseResult traverse(State s0, TraverseOptions options)
            throws NegativeWeightException {
        State s1 = s0;
        TransitStop toVertex = (TransitStop) getToVertex();
        if (toVertex.isLocal()) {
            Editor editor = s0.edit();
            editor.setAlightedLocal(true);
            s1 = editor.createState();
        }
        return new TraverseResult(0, s1, this);
    }

    @Override
    public TraverseResult traverseBack(State s0, TraverseOptions options)
            throws NegativeWeightException {
        
        StateData data = s0.getData();
        // Do not board if passenger has alighted from a local stop
        if (data.isAlightedLocal()) {
            return null;
        }
        TransitStop toVertex = (TransitStop) getToVertex();
        // Do not board once one has alighted from a local stop
        if (toVertex.isLocal() && data.isEverBoarded()) {
            return null;
        }
        // If we've hit our transfer limit, don't go any further
        if (data.getNumBoardings() > options.maxTransfers)
            return null;
        
        /* apply transfer rules */
        /* look in the global transfer table for the rules from the previous stop to
         * this stop. 
         */
        long t0 = s0.getTime();
        long alight_before = t0 - options.minTransferTime * 1000;
        long transfer_penalty = 0;
        if (data.getLastAlightedTime() != 0) { 
        	/* this is a transfer rather than an initial boarding */
            TransferTable transferTable = options.getTransferTable();
            if (transferTable.hasPreferredTransfers()) {
            	// only penalize transfers if there are some that will be depenalized
                transfer_penalty = options.baseTransferPenalty;
            }
            int transfer_time = transferTable.getTransferTime(getFromVertex(), data.getPreviousStop());
            if (transfer_time == TransferTable.UNKNOWN_TRANSFER) {
            	// use min transfer time relative to arrival time at this stop (initialized above)
            } else if (transfer_time >= 0) {
                // handle minimum time transfers (>0) and timed transfers (0)
            	// relative to alight time at last stop
            	alight_before = data.getLastAlightedTime() - transfer_time * 1000;
            	if (alight_before > t0) alight_before = t0; 
            } else if (transfer_time == TransferTable.FORBIDDEN_TRANSFER) {
                return null;
            } else if (transfer_time == TransferTable.PREFERRED_TRANSFER) {
                // depenalize preferred transfers
            	// TODO: verify correctness of this method (AMB)
                transfer_penalty = 0; 
            } else {
            	throw new IllegalStateException("Undefined value in transfer table.");
            }
        } else { 
        	/* this is a first boarding */
        	// TODO: add a separate initial transfer slack option
        	// alight_before = t0 - options.minTransferTime * 500; 
        	// ^ first boarding slack makes graphpath.optimize malfunction 
        	alight_before = t0;
        }

        // penalize transfers more heavily if requested by the user
        if (options.optimizeFor == OptimizeType.TRANSFERS && data.isEverBoarded()) {
            //this is not the first boarding, therefore we must have "transferred" -- whether
            //via a formal transfer or by walking.
            transfer_penalty += options.optimizeTransferPenalty;
        }

        Editor edit = s0.edit();
        edit.setTime(alight_before);
        edit.setEverBoarded(true);
        State s1 = edit.createState();
        long wait_cost = (t0 - alight_before) / 1000;
        return new TraverseResult(wait_cost + options.boardCost + transfer_penalty, s1, this);
    }
}
