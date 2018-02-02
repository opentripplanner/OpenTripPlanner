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
package org.opentripplanner.graph_builder.module;

import com.google.common.collect.Lists;
import org.apache.http.util.TextUtils;
import org.onebusaway.csv_entities.CsvEntityReader;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Transfer;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.graph_builder.module.transfers.FeedTransfer;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.edgetype.factory.StopIndex;
import org.opentripplanner.routing.edgetype.factory.TransferFactory;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.vertextype.TransitStationStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 *  {@link org.opentripplanner.graph_builder.services.GraphBuilderModule} module that creates transfers
 *  based on a feed-external CSV file.
 */
public class CrossFeedTransferGenerator implements GraphBuilderModule {

    private static final Logger LOG = LoggerFactory.getLogger(CrossFeedTransferGenerator.class);

    private File transfersFile;

    public CrossFeedTransferGenerator(File transfersFile) {
        this.transfersFile = transfersFile;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        LOG.info("Creating feed transfers....");
        GraphEntityResolver resolver = new GraphEntityResolver(graph);
        List<Transfer> transfers = Lists.newArrayList();
        List<Transfer> needsEdge = Lists.newArrayList();

        // read in transfers
        CsvEntityReader reader = new CsvEntityReader();
        reader.addEntityHandler(o -> {
            FeedTransfer ft = (FeedTransfer) o;
            boolean streetTransfer = ft.getStreetTransfer() > 0;
            for (Transfer t : expandFeedTransfer(resolver, ft)) {
                transfers.add(t);
                if (!streetTransfer) {
                    needsEdge.add(t);
                }
            }
        });
        try {
            reader.readEntities(FeedTransfer.class, new FileReader(transfersFile));
        } catch (IOException e) {
            LOG.error("Error reading feed transfers {}", e);
            return;
        }

        LOG.info("{} feed transfers found", transfers.size());

        TransferFactory factory = new TransferFactory(resolver);

        factory.loadTransferTable(graph, transfers);
        factory.createTransferEdges(needsEdge);
    }

    @Override
    public void checkInputs() {
    }

    private Collection<Transfer> expandFeedTransfer(GraphEntityResolver resolver, FeedTransfer feedTransfer) {

        Collection<Transfer> transfers = new ArrayList<>();

        for (Stop fromStop : resolver.getStops(feedTransfer.getFromStopId())) {
            for (Stop toStop : resolver.getStops(feedTransfer.getToStopId())) {
                Transfer transfer = new Transfer();
                String fromRoute = feedTransfer.getFromRouteId();
                String toRoute = feedTransfer.getToRouteId();
                String fromTrip = feedTransfer.getFromTripId();
                String toTrip = feedTransfer.getToTripId();

                transfer.setFromStop(fromStop);
                transfer.setToStop(toStop);

                if (!TextUtils.isEmpty(fromRoute)) {
                    transfer.setFromRoute(resolver.getRoute(fromRoute));
                }
                if (!TextUtils.isEmpty(toRoute)) {
                    transfer.setToRoute(resolver.getRoute(toRoute));
                }
                if (!TextUtils.isEmpty(fromTrip)) {
                    transfer.setFromTrip(resolver.getTrip(fromTrip));
                }
                if (!TextUtils.isEmpty(toTrip)) {
                    transfer.setToTrip(resolver.getTrip(toTrip));
                }

                transfer.setMinTransferTime(feedTransfer.getMinTransferTime());
                transfer.setTransferType(feedTransfer.getTransferType());

                transfers.add(transfer);
            }
        }
        return transfers;
    }

    private static class GraphEntityResolver implements StopIndex {

        Graph graph;

        GraphEntityResolver(Graph graph) {
            this.graph = graph;
            if (graph.index == null) {
                graph.index(new DefaultStreetVertexIndexFactory());
            }
        }

        private Collection<Stop> getStops(String idStr) {
            AgencyAndId id = AgencyAndId.convertFromString(idStr, ':');
            Collection<Stop> stops = new ArrayList<>();
            Stop thisStop = graph.index.stopForId.get(id);
            if (thisStop != null) {
                stops.add(thisStop);
            }
            for (Stop stop : graph.index.stopsForParentStation.get(id)) {
                if (stop.getId().getAgencyId().equals(id.getAgencyId())) {
                    stops.add(stop);
                }
            }
            return stops;
        }

        private Route getRoute(String idStr) {
            AgencyAndId id = AgencyAndId.convertFromString(idStr, ':');
            return graph.index.routeForId.get(id);
        }

        private Trip getTrip(String idStr) {
            AgencyAndId id = AgencyAndId.convertFromString(idStr, ':');
            return graph.index.tripForId.get(id);
        }

        @Override
        public Vertex getArriveVertexForStop(Stop stop) {
            return graph.index.stopVertexForStop.get(stop).arriveVertex;
        }

        @Override
        public Vertex getDepartVertexForStop(Stop stop) {
            return graph.index.stopVertexForStop.get(stop).departVertex;
        }

        @Override
        public Collection<Stop> getStopsForStation(Stop stop) {
            return graph.index.stopsForParentStation.get(stop.getId());
        }

        @Override
        public TransitStationStop getStationForStop(Stop stop) {
            return graph.index.stopVertexForStop.get(stop);
        }
    }

}


