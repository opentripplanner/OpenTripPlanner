package org.opentripplanner.updater.vehicle_sharing.vehicles_positions;

import org.opentripplanner.graph_builder.linking.TemporaryStreetSplitter;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.core.vehicle_sharing.BikeDescription;
import org.opentripplanner.routing.edgetype.rentedgetype.*;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
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

    private boolean addBikeStationToGraph(BikeRentalStation station, Graph graph) {
        BikeDescription bike = station.getBikeFromStation();

        Optional<RentVehicleEdge> edge = temporaryStreetSplitter
                .linkRentableVehicleToGraph(bike)
                .map(RentVehicleEdge::getRentEdge);

        if (edge.isPresent()) {
            BikeStationParkingZone parkingZone = new BikeStationParkingZone(station);
            graph.parkingZonesCalculator.enableNewParkingZone(parkingZone);
            List<SingleParkingZone> parkingZonesEnabled = graph.parkingZonesCalculator.getParkingZonesEnabled();


            DropoffVehicleEdge dropEdge = new DropoffVehicleEdge(edge.get().getToVertex());
            ParkingZoneInfo parkingZoneInfo = new ParkingZoneInfo(Collections.singletonList(parkingZone), parkingZonesEnabled);
            dropEdge.setParkingZones(parkingZoneInfo);


            edge.get().setBikeRentalStation(station);
            graph.bikeRentalStationsInGraph.put(station, edge.get());
        } else {
            return false;
        }
        return true;
    }

    private boolean updateBikeStationInfo(BikeRentalStation station, Graph graph) {
        RentVehicleEdge rentVehicleEdge = graph.bikeRentalStationsInGraph.getOrDefault(station, null);
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
    }
}
