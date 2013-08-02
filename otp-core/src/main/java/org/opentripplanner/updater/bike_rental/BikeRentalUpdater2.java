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

package org.opentripplanner.updater.bike_rental;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.edgetype.RentABikeOffEdge;
import org.opentripplanner.routing.edgetype.RentABikeOnEdge;
import org.opentripplanner.routing.edgetype.loader.LinkRequest;
import org.opentripplanner.routing.edgetype.loader.NetworkLinkerLibrary;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.updater.GraphUpdaterRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dynamic bike-rental station updater which encapsulate one BikeRentalDataSource. 
 *
 */
public class BikeRentalUpdater2 implements GraphUpdaterRunnable {
    private static final Logger LOG = LoggerFactory.getLogger(BikeRentalUpdater2.class);

    Map<BikeRentalStation, BikeRentalStationVertex> verticesByStation = new HashMap<BikeRentalStation, BikeRentalStationVertex>();

    private BikeRentalDataSource source;

    private Graph graph;

    private NetworkLinkerLibrary networkLinkerLibrary;

    private BikeRentalStationService service;

    private String network = "default";

    public void setNetwork(String network) {
        this.network = network;
    }

    public BikeRentalUpdater2(Graph graph, BikeRentalDataSource source) {
        LOG.info("Setting up bike rental updater.");
        this.graph = graph;
        this.source = source;
        networkLinkerLibrary = new NetworkLinkerLibrary(graph,
                Collections.<Class<?>, Object> emptyMap());
        service = graph.getService(BikeRentalStationService.class);
        if (service == null) {
            service = new BikeRentalStationService();
            graph.putService(BikeRentalStationService.class, service);
        }
    }

    @Override
    public void setup() {
    }

    @Override
    public void run() {
        LOG.debug("Updating bike rental stations from " + source);
        if (!source.update()) {
            LOG.debug("No updates");
            return;
        }
        List<BikeRentalStation> stations = source.getStations();
        Set<BikeRentalStation> stationSet = new HashSet<BikeRentalStation>();
        Set<String> networks = new HashSet<String>(Arrays.asList(network));
        /* add any new stations and update bike counts for existing stations */
        for (BikeRentalStation station : stations) {
            service.addStation(station);
            stationSet.add(station);
            BikeRentalStationVertex vertex = verticesByStation.get(station);
            if (vertex == null) {
                vertex = new BikeRentalStationVertex(graph, station);
                LinkRequest request = networkLinkerLibrary.connectVertexToStreets(vertex);
                for (Edge e : request.getEdgesAdded()) {
                    graph.addTemporaryEdge(e);
                }
                verticesByStation.put(station, vertex);
                new RentABikeOnEdge(vertex, vertex, networks);
                new RentABikeOffEdge(vertex, vertex, networks);
            } else {
                vertex.setBikesAvailable(station.bikesAvailable);
                vertex.setSpacesAvailable(station.spacesAvailable);
            }
        }
        /* remove existing stations that were not present in the update */
        List<BikeRentalStation> toRemove = new ArrayList<BikeRentalStation>();
        for (Entry<BikeRentalStation, BikeRentalStationVertex> entry : verticesByStation.entrySet()) {
            BikeRentalStation station = entry.getKey();
            if (stationSet.contains(station))
                continue;
            BikeRentalStationVertex vertex = entry.getValue();
            if (graph.containsVertex(vertex)) {
                graph.removeVertexAndEdges(vertex);
            }
            toRemove.add(station);
            service.removeStation(station);
            // TODO: need to unsplit any streets that were split
        }
        for (BikeRentalStation station : toRemove) {
            // post-iteration removal to avoid concurrent modification
            verticesByStation.remove(station);
        }

    }

    @Override
    public void teardown() {
    }
}
