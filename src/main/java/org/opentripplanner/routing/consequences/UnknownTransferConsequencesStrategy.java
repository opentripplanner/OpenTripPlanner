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

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StopTransfer;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.TransitStopDepart;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UnknownTransferConsequencesStrategy extends SingleOptionStrategy<Boolean> {

    private RoutingRequest options;

    private boolean mainSearchUnknownTransfers;

    public UnknownTransferConsequencesStrategy(RoutingRequest options) {
        super(() -> options.unknownTransfersAreForbidden,
                (b) -> options.unknownTransfersAreForbidden = b,
                false);
        this.options = options;
        this.mainSearchUnknownTransfers = getOldValue();
    }

    @Override
    public List<Alert> getConsequences(List<GraphPath> paths) {
        List<Alert> alerts = new ArrayList<>();
        for (GraphPath path : paths) {
            for (State state : path.states) {
                Edge edge = state.getBackEdge();
                State s0 = state.getBackState();
                if (edge instanceof TransitBoardAlight && s0.isEverBoarded() && (((TransitBoardAlight) edge).boarding)) {
                    Stop fromStop = s0.getPreviousStop();
                    Stop toStop = ((TransitStopDepart) edge.getFromVertex()).getStop();
                    Trip fromTrip = s0.getPreviousTrip();
                    Trip toTrip = state.getBackTrip();
                    TransferTable transferTable = options.getRoutingContext().transferTable;
                    int transferTime = transferTable.getTransferTime(fromStop,
                            toStop, fromTrip, toTrip, true);
                    if (transferTime == StopTransfer.UNKNOWN_TRANSFER) {
                        String fromFeed = fromStop.getId().getAgencyId();
                        String toFeed = toStop.getId().getAgencyId();
                        boolean feedTransfer = transferTable.hasFeedTransfers(fromFeed, toFeed, true);
                        if (feedTransfer && mainSearchUnknownTransfers) {
                            alerts.add(Alert.createSimpleAlerts("Unknown transfer",
                                    "Forbidden unknown transfer: from stop " + fromStop.getId() + " to " + toStop.getId() + " (" +
                                            fromStop.getName() + " to " + toStop.getName() + ")"
                                            + "; route " + shortName(fromTrip) + " to " + shortName(toTrip)
                                            + " [ itinerary: " + getItineraryString(path) + " ]"));
                        }
                    }
                }
            }
        }
        return alerts;
    }

    private static String getItineraryString(GraphPath path) {
        return path.getRoutes().stream().map(AgencyAndId::getId).collect(Collectors.joining(", "));
    }

    private static String shortName(Trip trip) {
        return trip.getRoute().getShortName() != null ? trip.getRoute().getShortName() : trip.getRoute().getLongName();
    }
}
