package org.opentripplanner.updater.vehicle_sharing.vehicles_positions;

import org.opentripplanner.graph_builder.linking.TemporaryStreetSplitter;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.core.vehicle_sharing.BikeDescription;
import org.opentripplanner.routing.edgetype.rentedgetype.BikeStationParkingZone;
import org.opentripplanner.routing.edgetype.rentedgetype.DropoffVehicleEdge;
import org.opentripplanner.routing.edgetype.rentedgetype.RentVehicleEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

class BikeStationsGraphWriterRunnable implements GraphWriterRunnable {
    private static final Logger LOG = LoggerFactory.getLogger(BikeStationsGraphWriterRunnable.class);


    private final TemporaryStreetSplitter temporaryStreetSplitter;
    private final List<BikeRentalStation> bikeRentalStationsFetchedFromApi;

    public BikeStationsGraphWriterRunnable(TemporaryStreetSplitter temporaryStreetSplitter, List<BikeRentalStation> bikeRentalStations) {
        this.temporaryStreetSplitter = temporaryStreetSplitter;
        this.bikeRentalStationsFetchedFromApi = bikeRentalStations;
    }

    private boolean updateBikeStationInfo(BikeRentalStation station, Graph graph) {
        RentVehicleEdge rentVehicleEdge = graph.bikeRentalStationsInGraph.getOrDefault(station, null);
        if (rentVehicleEdge != null) {
            rentVehicleEdge.getBikeRentalStation().bikesAvailable = station.bikesAvailable;
            rentVehicleEdge.getBikeRentalStation().spacesAvailable = station.spacesAvailable;

        } else {
            BikeDescription bike = station.getBikeFromStation();

            Optional<RentVehicleEdge> edge = temporaryStreetSplitter
                    .linkRentableVehicleToGraph(bike)
                    .map(RentVehicleEdge::getRentEdge);

            if (edge.isPresent()) {
                BikeStationParkingZone parkingZone = new BikeStationParkingZone(station);
                edge.get().setBikeRentalStation(station);
                DropoffVehicleEdge dropEdge = new DropoffVehicleEdge(edge.get().getToVertex());
                dropEdge.updateParkingZones();
                graph.bikeRentalStationsInGraph.put(station, edge.get());
            } else {
                return false;
            }
        }
        return true;
    }


    @Override
    public void run(Graph graph) {

//        Find all stations in graph
//        for each fetched station:
//            find station in graph. If found? update info : add station
//        For each station in graph:
//                if not found in fetched, mark as unused
//        TODO
    }
}
