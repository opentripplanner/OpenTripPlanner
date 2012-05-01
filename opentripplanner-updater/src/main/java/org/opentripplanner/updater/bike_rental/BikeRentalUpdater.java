package org.opentripplanner.updater.bike_rental;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opentripplanner.routing.edgetype.RentABikeOffEdge;
import org.opentripplanner.routing.edgetype.RentABikeOnEdge;
import org.opentripplanner.routing.edgetype.loader.NetworkLinkerLibrary;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.springframework.beans.factory.annotation.Autowired;

public class BikeRentalUpdater implements Runnable {

    Map<String, BikeRentalStationVertex> verticesByStation = new HashMap<String, BikeRentalStationVertex>();

    private BikeRentalDataSource source;

    private Graph graph;

    private NetworkLinkerLibrary networkLinkerLibrary;

    @Autowired
    public void setBikeRentalDataSource(BikeRentalDataSource source) {
        this.source = source;
    }

    @Autowired
    public void setGraphService(GraphService graphService) {
        graph = graphService.getGraph();
        networkLinkerLibrary = new NetworkLinkerLibrary(graph, Collections.<Class<?>, Object> emptyMap());
    }

    @Override
    public void run() {
        if (!source.update())
            return;
        List<BikeRentalStation> stations = source.getStations();
        Set<String> stationIds = new HashSet<String>();
        for (BikeRentalStation station : stations) {
            String id = station.id;
            stationIds.add(id);
            BikeRentalStationVertex vertex = verticesByStation.get(id);
            if (vertex == null) {
                vertex = new BikeRentalStationVertex(graph, "bike rental station " + id, station.x,
                        station.y, station.name, station.bikesAvailable, station.spacesAvailable);
                networkLinkerLibrary.connectVertexToStreets(vertex);
                verticesByStation.put(id, vertex);
                new RentABikeOnEdge(vertex, vertex);
                new RentABikeOffEdge(vertex, vertex);
            } else {
                vertex.setBikesAvailable(station.bikesAvailable);
                vertex.setSpacesAvailable(station.spacesAvailable);
            }
        }
        List<BikeRentalStationVertex> toRemove = new ArrayList<BikeRentalStationVertex>();
        for (Entry<String, BikeRentalStationVertex> entry : verticesByStation.entrySet()) {
            if (stationIds.contains(entry.getKey()))
                continue;
            BikeRentalStationVertex vertex = entry.getValue();
            graph.removeVertexAndEdges(vertex);
            toRemove.add(vertex);
            //TODO: need to unsplit any streets that were split
        }
        verticesByStation.keySet().removeAll(toRemove);

    }

}
