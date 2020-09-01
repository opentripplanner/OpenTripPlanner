package org.opentripplanner.updater.vehicle_sharing.vehicles_positions;

import com.google.common.annotations.VisibleForTesting;
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

public class BikeStationsGraphWriterRunnable implements GraphWriterRunnable {
    private static final Logger LOG = LoggerFactory.getLogger(BikeStationsGraphWriterRunnable.class);

    public final TemporaryStreetSplitter temporaryStreetSplitter;
    private final List<BikeRentalStation> bikeRentalStationsFetchedFromApi;

    public BikeStationsGraphWriterRunnable(TemporaryStreetSplitter temporaryStreetSplitter, List<BikeRentalStation> bikeRentalStations) {
        this.temporaryStreetSplitter = temporaryStreetSplitter;
        this.bikeRentalStationsFetchedFromApi = bikeRentalStations;
    }

    @VisibleForTesting
    private boolean addBikeStationToGraph(BikeRentalStation station, Graph graph) {
        BikeDescription bike = station.getBikeFromStation();

        Optional<RentVehicleEdge> edge = temporaryStreetSplitter
                .linkRentableVehicleToGraph(bike)
                .map(RentVehicleEdge::getRentEdge);

        if (edge.isPresent()) {
            BikeStationParkingZone parkingZone = new BikeStationParkingZone(station);
            graph.parkingZonesCalculator.enableNewParkingZone(parkingZone, graph);
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
        if (graph.parkingZonesCalculator != null) {
            int count = (int) bikeRentalStationsFetchedFromApi.stream().filter(station -> updateBikeStationInfo(station, graph)).count();
            LOG.info("Placed {} bike stations on a map", count);
        } else {
            LOG.warn("Parking zones calculator is not initialised yet, omitting bike stations update");
        }
    }
}
