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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.edgetype.RentABikeOffEdge;
import org.opentripplanner.routing.edgetype.RentABikeOnEdge;
import org.opentripplanner.routing.edgetype.loader.LinkRequest;
import org.opentripplanner.routing.edgetype.loader.NetworkLinkerLibrary;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class BikeRentalUpdater implements Runnable {
    private static final Logger _log = LoggerFactory.getLogger(BikeRentalUpdater.class);

    Map<BikeRentalStation, BikeRentalStationVertex> verticesByStation = new HashMap<BikeRentalStation, BikeRentalStationVertex>();

    private BikeRentalDataSource source;

    private Graph graph;

    private NetworkLinkerLibrary networkLinkerLibrary;

    private BikeRentalStationService service;

    private String routerId;

    private GraphService graphService;

    private String network = "default";

    public void setRouterId(String routerId) {
        this.routerId = routerId;
    }
    
    public void setNetwork(String network) {
        this.network = network;
    }

    @Autowired
    public void setBikeRentalDataSource(BikeRentalDataSource source) {
        this.source = source;
    }

    @Autowired
    public void setGraphService(GraphService graphService) {
        this.graphService = graphService;
    }

    @PostConstruct
    public void setup() {
        if (routerId != null) {
            graph = graphService.getGraph(routerId);
        } else {
            graph = graphService.getGraph();
        }
        networkLinkerLibrary = new NetworkLinkerLibrary(graph, Collections.<Class<?>, Object> emptyMap());
        service = graph.getService(BikeRentalStationService.class);
        if (service == null) {
            service = new BikeRentalStationService();
            graph.putService(BikeRentalStationService.class, service);
        }
    }

    public List<BikeRentalStation> getStations() {
        return source.getStations();
    }

    @Override
    public void run() {
        _log.debug("Updating bike rental stations from " + source);
        if (!source.update()) {
            _log.debug("No updates");
            return;
        }
        List<BikeRentalStation> stations = source.getStations();
        Set<BikeRentalStation> stationSet = new HashSet<BikeRentalStation>();
        for (BikeRentalStation station : stations) {
            service.addStation(station);
            String id = station.id;
            stationSet.add(station);
            BikeRentalStationVertex vertex = verticesByStation.get(station);
            if (vertex == null) {
                String name = "bike rental station " + id;
                vertex = new BikeRentalStationVertex(graph, id, name, station.x,
                        station.y, station.name, station.bikesAvailable, station.spacesAvailable);
                LinkRequest request = networkLinkerLibrary.connectVertexToStreets(vertex);
                for (Edge e : request.getEdgesAdded()) {
                    graph.addTemporaryEdge(e);
                }
                verticesByStation.put(station, vertex);
                new RentABikeOnEdge(vertex, vertex, network);
                new RentABikeOffEdge(vertex, vertex, network);
            } else {
                vertex.setBikesAvailable(station.bikesAvailable);
                vertex.setSpacesAvailable(station.spacesAvailable);
            }
        }
        List<BikeRentalStationVertex> toRemove = new ArrayList<BikeRentalStationVertex>();
        for (Entry<BikeRentalStation, BikeRentalStationVertex> entry : verticesByStation.entrySet()) {
            BikeRentalStation station = entry.getKey();
            if (stationSet.contains(station))
                continue;
            BikeRentalStationVertex vertex = entry.getValue();
            if (graph.containsVertex(vertex)) {
                graph.removeVertexAndEdges(vertex);
                toRemove.add(vertex);
            }
            service.removeStation(station);
            //TODO: need to unsplit any streets that were split
        }
        verticesByStation.keySet().removeAll(toRemove);

    }

}
