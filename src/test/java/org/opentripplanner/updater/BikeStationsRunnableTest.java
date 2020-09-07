package org.opentripplanner.updater;

import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.CoordinateXY;
import org.opentripplanner.graph_builder.linking.TemporaryStreetSplitter;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.core.vehicle_sharing.Provider;
import org.opentripplanner.routing.edgetype.rentedgetype.RentBikeEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TemporaryRentVehicleVertex;
import org.opentripplanner.updater.vehicle_sharing.parking_zones.ParkingZonesCalculator;
import org.opentripplanner.updater.vehicle_sharing.vehicles_positions.BikeStationsGraphWriterRunnable;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class BikeStationsRunnableTest {
    private Graph graph;

    private TemporaryStreetSplitter temporaryStreetSplitter;

    private TemporaryRentVehicleVertex vertex;

    private static final BikeRentalStation station11 = new BikeRentalStation("11", 0, 0, 1, 1, new Provider(1, "provider1"));


    @Before
    public void setUp() {
        graph = new Graph();

        temporaryStreetSplitter = mock(TemporaryStreetSplitter.class);

        vertex = new TemporaryRentVehicleVertex("id", new CoordinateXY(1, 2), "name");
    }

    @Test
    public void updateBikeStation() {
        //when
        temporaryStreetSplitter = mock(TemporaryStreetSplitter.class);
        RentBikeEdge rentVehicleEdge = new RentBikeEdge(vertex, station11);
        graph.bikeRentalStationsInGraph.put(station11, rentVehicleEdge);
        graph.parkingZonesCalculator = mock(ParkingZonesCalculator.class);

        BikeRentalStation newStation = station11.clone();

        newStation.spacesAvailable = 13;
        newStation.bikesAvailable = 12;

        //given
        BikeStationsGraphWriterRunnable runnable = new BikeStationsGraphWriterRunnable(temporaryStreetSplitter, Collections.singletonList(newStation));
        runnable.run(graph);

        //then
        assertEquals(12, (int) rentVehicleEdge.getBikeRentalStation().bikesAvailable);
        assertEquals(13, (int) rentVehicleEdge.getBikeRentalStation().spacesAvailable);
    }


}
