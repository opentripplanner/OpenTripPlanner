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
package org.opentripplanner.routing.edgetype.factory;

import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import org.apache.commons.math3.util.Pair;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Transfer;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.core.StopTransfer;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.edgetype.TimedTransferEdge;
import org.opentripplanner.routing.edgetype.TransferEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStationStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory.STOP_LOCATION_TYPE;
import static org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory.PARENT_STATION_LOCATION_TYPE;

/**
 * Refactor transfer creation out of GTFSPatternHopFactory so it can be re-used for cross-feed transfers.
 */
public class TransferFactory {

    private static final Logger LOG = LoggerFactory.getLogger(TransferFactory.class);

    private static GeometryFactory _geometryFactory = new GeometryFactory();

    private StopIndex _stopIndex;

    public TransferFactory(StopIndex stopVertexIndex) {
        this._stopIndex = stopVertexIndex;
    }

    public void loadTransferTable(Graph graph, Collection<Transfer> transfers) {
        TransferTable transferTable = graph.getTransferTable();
        for (Transfer sourceTransfer : transfers) {
            // Transfers may be specified using parent stations (https://developers.google.com/transit/gtfs/reference/transfers-file)
            // "If the stop ID refers to a station that contains multiple stops, this transfer rule applies to all stops in that station."
            // we thus expand transfers that use parent stations to all the member stops.
            for (Transfer t : expandTransfer(sourceTransfer)) {
                Stop fromStop = t.getFromStop();
                Stop toStop = t.getToStop();
                Route fromRoute = t.getFromRoute();
                Route toRoute = t.getToRoute();
                Trip fromTrip = t.getFromTrip();
                Trip toTrip = t.getToTrip();
                Vertex fromVertex = _stopIndex.getArriveVertexForStop(fromStop);
                Vertex toVertex = _stopIndex.getDepartVertexForStop(toStop);
                switch (t.getTransferType()) {
                    case 1:
                        // timed (synchronized) transfer
                        // Handle with edges that bypass the street network.
                        // from and to vertex here are stop_arrive and stop_depart vertices

                        // only add edge when it doesn't exist already
                        boolean hasTimedTransferEdge = false;

                        for (Edge outgoingEdge : fromVertex.getOutgoing()) {
                            if (outgoingEdge instanceof TimedTransferEdge) {
                                if (outgoingEdge.getToVertex() == toVertex) {
                                    hasTimedTransferEdge = true;
                                    break;
                                }
                            }
                        }
                        if (!hasTimedTransferEdge) {
                            new TimedTransferEdge(fromVertex, toVertex);
                        }
                        // add to transfer table to handle specificity
                        transferTable.addTransferTime(fromStop, toStop, fromRoute, toRoute, fromTrip, toTrip, StopTransfer.TIMED_TRANSFER);
                        break;
                    case 2:
                        // min transfer time
                        transferTable.addTransferTime(fromStop, toStop, fromRoute, toRoute, fromTrip, toTrip, t.getMinTransferTime());
                        break;
                    case 3:
                        // forbidden transfer
                        transferTable.addTransferTime(fromStop, toStop, fromRoute, toRoute, fromTrip, toTrip, StopTransfer.FORBIDDEN_TRANSFER);
                        break;
                    case 0:
                    default:
                        // preferred transfer
                        transferTable.addTransferTime(fromStop, toStop, fromRoute, toRoute, fromTrip, toTrip, StopTransfer.PREFERRED_TRANSFER);
                        break;
                }
            }
        }
    }

    // Only create one TransferEdge per stop pair (though there could be more Transfers in transfers.txt)
    public void createTransferEdges(Collection<Transfer> transfers) {
        LOG.info("creating transfer edges from file...");
        Set<Pair<Vertex, Vertex>> knownTransfers = Sets.newHashSet();

        for (Transfer transfer : transfers) {
            int type = transfer.getTransferType();
            if (type == 3) // type 3 = transfer not possible
                continue;
            if (transfer.getFromStop().equals(transfer.getToStop())) {
                continue;
            }
            TransitStationStop fromv = _stopIndex.getStationForStop(transfer.getFromStop());
            TransitStationStop tov = _stopIndex.getStationForStop(transfer.getToStop());

            Pair<Vertex, Vertex> transferKey = new Pair<>(fromv, tov);
            if (knownTransfers.contains(transferKey)) {
                continue;
            }
            else {
                knownTransfers.add(transferKey);
            }

            double distance = SphericalDistanceLibrary.distance(fromv.getCoordinate(), tov.getCoordinate());

            TransferEdge transferEdge = new TransferEdge(fromv, tov, distance);
            CoordinateSequence sequence = new PackedCoordinateSequence.Double(new Coordinate[] {
                    fromv.getCoordinate(), tov.getCoordinate() }, 2);
            LineString geometry = _geometryFactory.createLineString(sequence);
            transferEdge.setGeometry(geometry);
        }
    }

   private Collection<Transfer> expandTransfer (Transfer source) {
        Stop fromStop = source.getFromStop();
        Stop toStop = source.getToStop();

        if (fromStop.getLocationType() == STOP_LOCATION_TYPE && toStop.getLocationType() == STOP_LOCATION_TYPE) {
            // simple, no need to copy anything
            return Arrays.asList(source);
        } else {
            // at least one of the stops is a parent station
            // all the stops this transfer originates with
            Collection<Stop> fromStops;

            // all the stops this transfer terminates with
            Collection<Stop> toStops;

            if (fromStop.getLocationType() == PARENT_STATION_LOCATION_TYPE) {
                fromStops = _stopIndex.getStopsForStation(fromStop);
            } else {
                fromStops = Arrays.asList(fromStop);
            }

            if (toStop.getLocationType() == PARENT_STATION_LOCATION_TYPE) {
                toStops = _stopIndex.getStopsForStation(toStop);
            } else {
                toStops = Arrays.asList(toStop);
            }

            List<Transfer> expandedTransfers = new ArrayList<>(fromStops.size() * toStops.size());

            for (Stop expandedFromStop : fromStops) {
                for (Stop expandedToStop : toStops) {
                    Transfer expanded = new Transfer(source);
                    expanded.setFromStop(expandedFromStop);
                    expanded.setToStop(expandedToStop);
                    expandedTransfers.add(expanded);
                }
            }

            LOG.debug(
                    "Expanded transfer between stations \"{} ({})\" and \"{} ({})\" to {} transfers between {} and {} stops",
                    fromStop.getName(),
                    fromStop.getId(),
                    toStop.getName(),
                    toStop.getId(),
                    expandedTransfers.size(),
                    fromStops.size(),
                    toStops.size()
            );

            return expandedTransfers;
        }
    }
}
