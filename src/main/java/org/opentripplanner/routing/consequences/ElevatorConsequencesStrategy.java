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
package org.opentripplanner.routing.consequences;

import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.spt.GraphPath;

import java.util.ArrayList;
import java.util.List;

public class ElevatorConsequencesStrategy extends SingleOptionStrategy<Boolean> {

    private RoutingRequest options;

    public ElevatorConsequencesStrategy(RoutingRequest options) {
        super(() -> options.ignoreRealtimeUpdates,
                (b) -> options.ignoreRealtimeUpdates = b,
                true);
        this.options = options;
    }

    @Override
    public List<Alert> getConsequences(List<GraphPath> paths) {
        List<Alert> consequences = new ArrayList<>();
        for (GraphPath path : paths) {
            for (State s : path.states) {
                if (s.getBackEdge() != null) {
                    for (AlertPatch alert : options.rctx.graph.getAlertPatches(s.getBackEdge())) {
                        if (alert.displayDuring(s) && alert.isRoutingConsequence()) {
                            consequences.add(alert.getAlert());
                        }
                    }
                }
            }
        }
        return consequences;
    }
}
