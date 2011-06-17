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

import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.core.TransitStop;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;

/** PreBoard edges connect a TransitStop to its agency_stop_depart vertices;
 *  PreAlight edges connect an agency_stop_arrive vertex to its TransitStop.
 *  
 *  Applies the local stop rules (see TransitStop.java and LocalStopFinder.java) 
 *  as well as transfer limits, timed and preferred transfer rules, 
 *  transfer penalties, and boarding costs.
 *  This avoids applying these costs/rules repeatedly in (Pattern)Board edges.
 *  These are single station or station-to-station specific costs, rather than
 *  trip-pattern specific costs.
 */
public class PreBoardEdge extends FreeEdge {

    private static final long serialVersionUID = -8046937388471651897L;

    public PreBoardEdge(Vertex from, Vertex to) {
        super(from, to);
    	if (! (from instanceof TransitStop))
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
	        // apply scedule slack both boarding and alighting (forward/backward)
	        // this makes slack proportional to the number of vehicles involved
	        // and makes things work more smoothly when back-optimizing a path
	        s1.incrementTimeInSeconds(options.minTransferTime);
	        return s1.makeState();
    	} else {
    		/* Traverse forward: apply stop(pair)-specific costs */
    		
        	// Do not pre-board if transit modes are not selected.
        	// Return null here rather than in StreetTransitLink so that walk-only
        	// options can be used to find transit stops without boarding vehicles.
        	if ( ! options.getModes().getTransit())
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
            /* look in the global transfer table for the rules from the previous stop to
             * this stop. 
             */
            long t0 = s0.getTime();
            long board_after = t0 + options.minTransferTime * 1000;
            long transfer_penalty = 0;
            if (s0.getLastAlightedTime() != 0) { 
            	/* this is a transfer rather than an initial boarding */
                TransferTable transferTable = options.getTransferTable();
                if (transferTable.hasPreferredTransfers()) {
                	// only penalize transfers if there are some that will be depenalized
                    transfer_penalty = options.baseTransferPenalty;
                }
                int transfer_time = transferTable.getTransferTime(s0.getPreviousStop(), getToVertex());
                if (transfer_time == TransferTable.UNKNOWN_TRANSFER) {
                	// use min transfer time relative to arrival time at this stop (initialized above)
                } else if (transfer_time >= 0) {
                    // handle minimum time transfers (>0) and timed transfers (0)
                	// relative to alight time at last stop
                	board_after = s0.getLastAlightedTime() + transfer_time * 1000;
                	if (board_after < t0) 
                		board_after = t0; 
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
            	// board_after = t0 + options.minTransferTime * 500; 
            	// ^ first boarding slack makes graphpath.optimize malfunction 
            	board_after = t0; 
            }

            // penalize transfers more heavily if requested by the user
            if (options.optimizeFor == OptimizeType.TRANSFERS && s0.isEverBoarded()) {
                //this is not the first boarding, therefore we must have "transferred" -- whether
                //via a formal transfer or by walking.
                transfer_penalty += options.optimizeTransferPenalty;
            }

            StateEditor s1 = s0.edit(this);
            s1.setTime(board_after);
            s1.setEverBoarded(true); // eliminate this with state.wait() and state.board()
            long wait_cost = (board_after - t0) / 1000;
            s1.incrementWeight(wait_cost + options.boardCost + transfer_penalty);

            return s1.makeState();
    	}
    }
    
    public State optimisticTraverse(State s0) {
    	TraverseOptions opt = s0.getOptions();
    	StateEditor s1 = s0.edit(this);
        s1.incrementTimeInSeconds(opt.minTransferTime);
        s1.incrementWeight(opt.minTransferTime + opt.boardCost / 2); // half here, half when alighting
    	return s1.makeState();
    }

    public String toString() {
    	return "preboard edge at stop " + fromv; 
    }

}
