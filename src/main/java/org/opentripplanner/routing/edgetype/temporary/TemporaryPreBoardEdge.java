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

package org.opentripplanner.routing.edgetype.temporary;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.StationEdge;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
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
public class TemporaryPreBoardEdge extends PreBoardEdge implements StationEdge, TemporaryEdge {

    public TemporaryPreBoardEdge(TransitStop from, TransitStopDepart to) {
        super(from, to);
    }

    @Override
    public void dispose() {
        fromv.removeOutgoing(this);
        tov.removeIncoming(this);
    }
}
