package org.opentripplanner.updater.vehicle_sharing.vehicles_positions;

import com.google.common.annotations.VisibleForTesting;
import org.opentripplanner.graph_builder.linking.TemporaryStreetSplitter;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.edgetype.rentedgetype.DropBikeEdge;
import org.opentripplanner.routing.edgetype.rentedgetype.RentBikeEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TemporaryRentVehicleVertex;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class BikeStationsGraphWriterRunnable implements GraphWriterRunnable {

    private static final Logger LOG = LoggerFactory.getLogger(BikeStationsGraphWriterRunnable.class);
    private final TemporaryStreetSplitter temporaryStreetSplitter;
    private final List<BikeRentalStation> bikeRentalStationsFetchedFromApi;

    public BikeStationsGraphWriterRunnable(TemporaryStreetSplitter temporaryStreetSplitter, List<BikeRentalStation> bikeRentalStations) {
        this.temporaryStreetSplitter = temporaryStreetSplitter;
        this.bikeRentalStationsFetchedFromApi = bikeRentalStations;
    }

    @VisibleForTesting
    private boolean addBikeStationToGraph(BikeRentalStation station, Graph graph) {
        Optional<TemporaryRentVehicleVertex> vertex = temporaryStreetSplitter.linkStationToGraph(station);
        RentBikeEdge edge;
        if (vertex.isPresent()) {
            edge = vertex.get().getOutgoing().stream()
                    .filter(RentBikeEdge.class::isInstance)
                    .map(RentBikeEdge.class::cast)
                    .findFirst().get();
        } else {
            return false;
        }

        if (graph.parkingZonesCalculator != null) {
            edge.setParkingZones(graph.parkingZonesCalculator.getParkingZonesForEdge(edge));
        }

        DropBikeEdge dropBikeEdge = new DropBikeEdge(vertex.get(), station);

        graph.bikeRentalStationsInGraph.put(station, edge);

        return true;
    }

    private boolean updateBikeStationInfo(BikeRentalStation station, Graph graph) {
        RentBikeEdge rentVehicleEdge = graph.bikeRentalStationsInGraph.getOrDefault(station, null);
        if (rentVehicleEdge != null) {
            rentVehicleEdge.getBikeRentalStation().bikesAvailable = station.bikesAvailable;
            rentVehicleEdge.getBikeRentalStation().spacesAvailable = station.spacesAvailable;
        } else {
            return addBikeStationToGraph(station, graph);
        }
        return true;
    }


    @Override
    public void run(Graph graph) {
        int count = (int) bikeRentalStationsFetchedFromApi.stream().filter(station -> updateBikeStationInfo(station, graph)).count();
        LOG.info("Placed {} bike stations on a map", count);
    }
}
