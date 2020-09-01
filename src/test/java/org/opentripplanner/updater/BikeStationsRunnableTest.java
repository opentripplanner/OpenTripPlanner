package org.opentripplanner.updater;

import com.sun.tools.javac.util.List;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.CoordinateXY;
import org.opentripplanner.graph_builder.linking.TemporaryStreetSplitter;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.core.vehicle_sharing.Provider;
import org.opentripplanner.routing.edgetype.rentedgetype.RentVehicleEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TemporaryRentVehicleVertex;
import org.opentripplanner.updater.vehicle_sharing.vehicles_positions.BikeStationsGraphWriterRunnable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class BikeStationsRunnableTest {
    private Graph graph;

    private TemporaryStreetSplitter temporaryStreetSplitter;

    private TemporaryRentVehicleVertex vertex;

//    private StreetLocation v1 = new StreetLocation

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
        RentVehicleEdge rentVehicleEdge = new RentVehicleEdge(vertex, station11.getBikeFromStation());
        BikeRentalStation newStation = station11.clone();
        newStation.spacesAvailable = 13;
        newStation.bikesAvailable = 12;

        rentVehicleEdge.setBikeRentalStation(station11);
        graph.bikeRentalStationsInGraph.put(station11, rentVehicleEdge);

        //given
        BikeStationsGraphWriterRunnable runnable = new BikeStationsGraphWriterRunnable(temporaryStreetSplitter, List.of(newStation));
        runnable.run(graph);

        //then
        verifyNoMoreInteractions(runnable.temporaryStreetSplitter);
        assertEquals(12, rentVehicleEdge.getBikeRentalStation().bikesAvailable);
        assertEquals(13, rentVehicleEdge.getBikeRentalStation().spacesAvailable);
    }


}
